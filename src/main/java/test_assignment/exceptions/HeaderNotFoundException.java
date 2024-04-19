package test_assignment.exceptions;

public class HeaderNotFoundException extends RuntimeException {
    public HeaderNotFoundException(final String message) {
        super(message);
    }

    public HeaderNotFoundException(final String message, final Throwable e) {
        super(message, e);
    }
}
