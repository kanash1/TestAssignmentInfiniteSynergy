package test_assignment.exceptions.sql;

public class CustomSQLException extends RuntimeException {
    public CustomSQLException(final String message) {
        super(message);
    }

    public CustomSQLException(final String message, final Throwable e) {
        super(message, e);
    }
}
