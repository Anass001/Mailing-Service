package tech.bluemail.platform.exceptions;

public class DatabaseException extends Exception
{
    public DatabaseException(final String message) {
        super(message);
    }
    
    public DatabaseException(final String message, final Throwable cause) {
        super(message, cause);
    }
    
    public DatabaseException(final Throwable cause) {
        super(cause);
    }
    
    public DatabaseException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
