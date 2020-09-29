/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.exceptions;

public class DropException
extends Exception {
    public DropException(String message) {
        super(message);
    }

    public DropException(String message, Throwable cause) {
        super(message, cause);
    }

    public DropException(Throwable cause) {
        super(cause);
    }

    public DropException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

