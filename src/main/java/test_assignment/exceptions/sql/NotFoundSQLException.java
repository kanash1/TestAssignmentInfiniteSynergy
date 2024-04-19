package test_assignment.exceptions.sql;

public class NotFoundSQLException extends CustomSQLException {
    public NotFoundSQLException(final String message) {
        super(message);
    }

    public NotFoundSQLException(final String message, final Throwable e) {
        super(message, e);
    }
}
