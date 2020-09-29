/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.exceptions;

import tech.bluemail.platform.logging.Logger;

public class ThreadException
implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Logger.error(e, t.getClass());
    }
}

