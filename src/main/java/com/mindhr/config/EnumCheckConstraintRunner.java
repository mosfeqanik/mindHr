package com.mindhr.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hibernate adds a {@code CHECK (col IN (...))} constraint for every {@code @Enumerated(STRING)} column
 * when it creates a table, and {@code ddl-auto=update} never refreshes it. Adding a value to one of
 * these enums then makes inserts fail on existing databases, so the stale constraints are dropped here.
 * The Java enum already restricts the values. Idempotent; runs before the other startup runners.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class EnumCheckConstraintRunner implements ApplicationRunner {

    /** {@code table.column} of each enum column that has gained values since its table was first created. */
    private static final List<String> COLUMNS = List.of(
            "work_requests.request_type");

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (String column : COLUMNS) {
            String[] parts = column.split("\\.");
            String constraint = parts[0] + "_" + parts[1] + "_check";
            jdbcTemplate.execute("ALTER TABLE " + parts[0] + " DROP CONSTRAINT IF EXISTS " + constraint);
        }
        log.info("Enum check constraints are up to date");
    }
}
