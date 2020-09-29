package tech.bluemail.platform.exceptions;

public class SystemException extends Exception
{
    public SystemException(final String message) {
        super(message);
    }
    
    public SystemException(final String message, final Throwable cause) {
        super(message, cause);
    }
    
    public SystemException(final Throwable cause) {
        super(cause);
    }
    
    public SystemException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
