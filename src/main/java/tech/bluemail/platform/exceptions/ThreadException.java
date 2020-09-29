package tech.bluemail.platform.exceptions;

import tech.bluemail.platform.logging.*;

public class ThreadException implements Thread.UncaughtExceptionHandler
{
    public ThreadException() {
        super();
    }
    
    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        Logger.error(e, t.getClass());
    }
}
