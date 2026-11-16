package com.mindhr.exception;

/** The organization's subscription does not allow the requested action (limit, feature or status). */
public class PlanLimitExceededException extends RuntimeException {

    public PlanLimitExceededException(String message) {
        super(message);
    }
}
