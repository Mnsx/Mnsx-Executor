package excutor;

/**
 * @Author Mnsx_x xx1527030652@gmail.com
 */
public class RejectTaskException extends Exception {
    public RejectTaskException() {
        super();
    }

    public RejectTaskException(String message) {
        super(message);
    }

    public RejectTaskException(String message, Throwable cause) {
        super(message, cause);
    }

    public RejectTaskException(Throwable cause) {
        super(cause);
    }

    protected RejectTaskException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
