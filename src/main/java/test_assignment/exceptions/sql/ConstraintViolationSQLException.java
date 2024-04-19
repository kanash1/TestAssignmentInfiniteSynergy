package test_assignment.exceptions.sql;

public class ConstraintViolationSQLException extends CustomSQLException {
    public ConstraintViolationSQLException(final String message) {
        super(message);
    }

    public ConstraintViolationSQLException(final String message, final Throwable e) {
        super(message, e);
    }
}
