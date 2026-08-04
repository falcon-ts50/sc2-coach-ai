package ai.sc2coach.portal.analysis;

public final class ReplayDecodingException extends RuntimeException {

    public ReplayDecodingException(String message) {
        super(message);
    }

    public ReplayDecodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
