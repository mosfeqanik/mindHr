package com.mindhr.entity.enums;

public enum RequestType {
    SALARY_ADVANCE,
    LOAN,
    SHIFT_CHANGE,
    LATE_ARRIVAL,
    EARLY_EXIT,
    EXPENSE,
    ASSET_REQUEST,
    WORK_FROM_HOME,
    OVERTIME,
    /** Missed or wrong check-in/out; the details say which day and times to fix. */
    ATTENDANCE_CORRECTION
}
