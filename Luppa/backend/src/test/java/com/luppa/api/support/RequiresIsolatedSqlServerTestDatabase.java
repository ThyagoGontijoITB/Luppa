package com.luppa.api.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequiresIsolatedSqlServerTestDatabase implements ExecutionCondition {
    private static final ConditionEvaluationResult ENABLED =
            ConditionEvaluationResult.enabled("TEST_DB_URL points to an isolated SQL Server test database");
    private static final Pattern DATABASE_NAME = Pattern.compile("(?i)(?:^|;)databaseName=([^;]+)");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String url = System.getenv("TEST_DB_URL");
        if (url == null || url.isBlank()) {
            return disabled("TEST_DB_URL is not set. Provide a disposable SQL Server database for integration tests.");
        }
        if (!url.regionMatches(true, 0, "jdbc:sqlserver://", 0, "jdbc:sqlserver://".length())) {
            return disabled("TEST_DB_URL must be a SQL Server JDBC URL.");
        }
        if (url.toLowerCase(Locale.ROOT).contains("invalid-test-db-host.invalid")) {
            return disabled("TEST_DB_URL still points to the invalid placeholder host.");
        }

        String databaseName = databaseName(url);
        if (databaseName == null || databaseName.isBlank()) {
            return disabled("TEST_DB_URL must include databaseName for an isolated test database.");
        }

        String normalizedDatabaseName = databaseName.trim().toLowerCase(Locale.ROOT);
        if ("luppabd".equals(normalizedDatabaseName)) {
            return disabled("Integration tests are blocked from using luppaBD.");
        }
        if (!normalizedDatabaseName.contains("test")) {
            return disabled("Integration tests require an isolated database whose name clearly contains 'test'.");
        }

        return ENABLED;
    }

    private ConditionEvaluationResult disabled(String reason) {
        return ConditionEvaluationResult.disabled(reason + " Skipping instead of connecting to a real or unknown database.");
    }

    private String databaseName(String url) {
        Matcher matcher = DATABASE_NAME.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }
}
