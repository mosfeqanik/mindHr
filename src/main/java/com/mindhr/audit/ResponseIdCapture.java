package com.mindhr.audit;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.RecordComponent;

/**
 * For write requests, copies {@code id} and {@code organizationId} from the response record onto
 * the request, so {@link ActivityRecorder} knows which record a create produced and which
 * organization it belongs to. The body itself is returned unchanged.
 */
@ControllerAdvice
public class ResponseIdCapture implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body != null && body.getClass().isRecord() && request instanceof ServletServerHttpRequest servlet
                && !"GET".equals(servlet.getServletRequest().getMethod())) {
            Long id = readLong(body, "id");
            Long organizationId = readLong(body, "organizationId");
            if (id != null) {
                servlet.getServletRequest().setAttribute(ActivityRecorder.ATTR_ENTITY_ID, id);
            }
            if (organizationId != null) {
                servlet.getServletRequest().setAttribute(ActivityRecorder.ATTR_ORGANIZATION_ID, organizationId);
            }
        }
        return body;
    }

    private static Long readLong(Object record, String name) {
        for (RecordComponent component : record.getClass().getRecordComponents()) {
            if (component.getName().equals(name) && component.getType() == Long.class) {
                try {
                    return (Long) component.getAccessor().invoke(record);
                } catch (ReflectiveOperationException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
