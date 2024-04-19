package test_assignment.exceptions;

public class ServerConfigurationException extends RuntimeException {
    public ServerConfigurationException(final String message) {
        super(message);
    }

    public ServerConfigurationException(final String message, final Throwable e) {
        super(message, e);
    }
}
