package com.mindhr.audit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityRecorderTest {

    @ParameterizedTest(name = "{0} {1} -> {2} {3} {4}")
    @CsvSource(nullValues = "null", value = {
            "POST,   /api/employees,                  EMPLOYEE,       null, CREATE",
            "PUT,    /api/employees/5,                EMPLOYEE,       5,    UPDATE",
            "DELETE, /api/branches/3,                 BRANCH,         3,    DELETE",
            "POST,   /api/leave-requests/7/approve,   LEAVE_REQUEST,  7,    APPROVE",
            "PATCH,  /api/subscriptions/2/status,     SUBSCRIPTION,   2,    STATUS",
            "POST,   /api/attendance/check-in,        ATTENDANCE,     null, CHECK_IN",
            "POST,   /api/account/change-password,    ACCOUNT,        null, CHANGE_PASSWORD",
            "POST,   /api/payroll-runs/4/pay,         PAYROLL_RUN,    4,    PAY",
            "PUT,    /api/payslips/9/adjustments,     PAYSLIP,        9,    ADJUSTMENTS",
            "POST,   /api/payroll-runs,               PAYROLL_RUN,    null, CREATE",
            "PATCH,  /api/organizations/1/owner,      ORGANIZATION,   1,    OWNER",
            "POST,   /api/support-tickets/8/comments, SUPPORT_TICKET, 8,    COMMENTS",
            "PUT,    /api/salary-structures/6,        SALARY_STRUCTURE, 6,  UPDATE",
            "DELETE, /api/menu-categories/2,          MENU_CATEGORY,  2,    DELETE",
            "POST,   /api/addresses,                  ADDRESS,        null, CREATE",
    })
    void parsesEntityIdAndActionFromThePath(String method, String path, String entityType, Long id, String action) {
        ActivityRecorder.Parsed parsed = ActivityRecorder.parse(method, path);

        assertThat(parsed.entityType()).isEqualTo(entityType);
        assertThat(parsed.entityId()).isEqualTo(id);
        assertThat(parsed.action()).isEqualTo(action);
    }
}
