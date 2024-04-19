package test_assignment.exceptions;

public class JwtNotFoundException extends RuntimeException {
    public JwtNotFoundException(final String message) {
        super(message);
    }

    public JwtNotFoundException(final String message, final Throwable e) {
        super(message, e);
    }
}
