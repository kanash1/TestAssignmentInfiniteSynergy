package test_assignment.exceptions;

public class ResourceExtractionException extends RuntimeException {
    public ResourceExtractionException(final String message) {
        super(message);
    }

    public ResourceExtractionException(final String message, final Throwable e) {
        super(message, e);
    }
}
