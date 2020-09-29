package tech.bluemail.platform.exceptions;

public class DropException extends Exception
{
    public DropException(final String message) {
        super(message);
    }
    
    public DropException(final String message, final Throwable cause) {
        super(message, cause);
    }
    
    public DropException(final Throwable cause) {
        super(cause);
    }
    
    public DropException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
