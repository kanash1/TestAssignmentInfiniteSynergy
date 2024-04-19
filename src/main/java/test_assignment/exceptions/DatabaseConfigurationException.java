package test_assignment.exceptions;

public class DatabaseConfigurationException extends RuntimeException {
    public DatabaseConfigurationException(final String message) {
        super(message);
    }

    public DatabaseConfigurationException(final String message, final Throwable e) {
        super(message, e);
    }
}
