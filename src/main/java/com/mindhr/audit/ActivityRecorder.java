package com.mindhr.audit;

import com.mindhr.security.JwtAuthenticatedPrincipal;
import com.mindhr.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Writes one {@code ActivityLog} row for every successful write request (POST / PUT / PATCH /
 * DELETE with a 2xx status), so services do not have to log anything themselves.
 *
 * <p>The URL gives the entity type, id and action:
 * <pre>
 *   POST   /api/employees                 -> EMPLOYEE  CREATE         (id from the response body)
 *   PUT    /api/employees/5               -> EMPLOYEE  UPDATE         5
 *   DELETE /api/branches/3                -> BRANCH    DELETE         3
 *   POST   /api/leave-requests/7/approve  -> LEAVE_REQUEST APPROVE    7
 *   PATCH  /api/subscriptions/2/status    -> SUBSCRIPTION STATUS      2
 *   POST   /api/attendance/check-in       -> ATTENDANCE CHECK_IN      (id from the response body)
 * </pre>
 * Request bodies are never stored (they can hold passwords or salaries); {@code details} keeps
 * only the method and path. Recording runs after the response is complete, and a failure here
 * is logged and swallowed: losing an audit row must not turn a successful request into an error.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityRecorder implements HandlerInterceptor {

    static final String ATTR_ENTITY_ID = ActivityRecorder.class.getName() + ".entityId";
    static final String ATTR_ORGANIZATION_ID = ActivityRecorder.class.getName() + ".organizationId";

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    /** Login has no signed-in user yet; reading the log must not write to it; marking a notification read is noise. */
    private static final List<String> SKIPPED_PREFIXES = List.of("/api/auth/", "/api/activity-logs");
    private static final String NOTIFICATION_READ_SUFFIX = "/read";

    private final ActivityLogService activityLogService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null || !WRITE_METHODS.contains(request.getMethod())) {
            return;
        }
        int status = response.getStatus();
        if (status < 200 || status >= 300) {
            return;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || SKIPPED_PREFIXES.stream().anyMatch(path::startsWith)
                || (path.startsWith("/api/notifications/") && path.endsWith(NOTIFICATION_READ_SUFFIX))) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtAuthenticatedPrincipal principal)) {
            return;
        }
        try {
            Parsed parsed = parse(request.getMethod(), path);
            Long entityId = parsed.entityId() != null ? parsed.entityId() : (Long) request.getAttribute(ATTR_ENTITY_ID);
            // An organization's own record belongs to that organization, so its owner sees "created" / "owner changed".
            Long organizationId = "ORGANIZATION".equals(parsed.entityType()) && entityId != null
                    ? entityId : resolveOrganizationId(request, principal);
            activityLogService.record(organizationId, principal.userId(), parsed.action(), parsed.entityType(), entityId,
                    "{\"method\":\"" + request.getMethod() + "\",\"path\":\"" + escape(path) + "\"}", clientIp(request));
        } catch (RuntimeException e) {
            log.warn("Could not record activity for {} {}: {}", request.getMethod(), path, e.toString());
        }
    }

    record Parsed(String entityType, Long entityId, String action) {
    }

    /** {@code /api/<resource>[/<id>][/<action>...]} or {@code /api/<resource>/<action>}. */
    static Parsed parse(String method, String path) {
        List<String> segments = Arrays.stream(path.substring("/api/".length()).split("/"))
                .filter(s -> !s.isEmpty())
                .toList();
        int idIndex = -1;
        for (int i = 1; i < segments.size(); i++) {
            if (segments.get(i).chars().allMatch(Character::isDigit)) {
                idIndex = i;
                break;
            }
        }
        String resource;
        Long id = null;
        List<String> tail;
        if (idIndex > 0) {
            resource = segments.get(idIndex - 1);
            id = Long.valueOf(segments.get(idIndex));
            tail = segments.subList(idIndex + 1, segments.size());
        } else {
            resource = segments.isEmpty() ? "unknown" : segments.get(0);
            tail = segments.size() > 1 ? segments.subList(1, segments.size()) : List.of();
        }
        String action = tail.isEmpty() ? defaultAction(method) : toConstant(String.join("_", tail));
        return new Parsed(truncate(toConstant(singular(resource)), 50), id, truncate(action, 50));
    }

    private static String defaultAction(String method) {
        return switch (method) {
            case "POST" -> "CREATE";
            case "DELETE" -> "DELETE";
            default -> "UPDATE";
        };
    }

    private static String singular(String resource) {
        if (resource.endsWith("ies")) {
            return resource.substring(0, resource.length() - 3) + "y";
        }
        if (resource.endsWith("ches") || resource.endsWith("shes") || resource.endsWith("xes") || resource.endsWith("sses")) {
            return resource.substring(0, resource.length() - 2);
        }
        if (resource.endsWith("s") && !resource.endsWith("ss")) {
            return resource.substring(0, resource.length() - 1);
        }
        return resource;
    }

    private static String toConstant(String s) {
        return s.replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** The organization of the record that was written; else the user's own; else what a platform user asked for. */
    private static Long resolveOrganizationId(HttpServletRequest request, JwtAuthenticatedPrincipal principal) {
        Object fromBody = request.getAttribute(ATTR_ORGANIZATION_ID);
        if (fromBody instanceof Long id) {
            return id;
        }
        if (principal.organizationId() != null) {
            return principal.organizationId();
        }
        String param = request.getParameter("organizationId");
        if (param != null && param.chars().allMatch(Character::isDigit) && !param.isEmpty()) {
            return Long.valueOf(param);
        }
        return null;
    }

    /** Render (and most hosts) put the real client address first in X-Forwarded-For. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        return ip == null ? null : truncate(ip, 45);
    }
}
