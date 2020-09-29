/*
 * Decompiled with CFR <Could not determine version>.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.exception.ExceptionUtils
 *  org.apache.log4j.Level
 *  org.apache.log4j.Logger
 *  org.apache.log4j.PropertyConfigurator
 */
package tech.bluemail.platform.logging;

import java.net.URL;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.LoggerFactory;

public class Logger {
    public static void initlog4Java() {
        URL propertiesFileURL = Logger.class.getResource("/tech/bluemail/platform/logging/log4j2.properties");
        PropertyConfigurator.configure((URL)propertiesFileURL);
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
    }

    public static void info(String msg) {
        LoggerFactory.getLogger("ApplicationInfoMessage").info(msg);
    }

    public static void debug(String msg) {
        LoggerFactory.getLogger("ApplicationDebugMessage").debug(msg);
    }

    public static void error(String msg) {
        LoggerFactory.getLogger("ApplicationErrorMessage").error(msg);
    }

    public static void error(String msg, Throwable exception) {
        LoggerFactory.getLogger("ApplicationErrorMessage").error(msg + ". Caused By : " + ExceptionUtils.getRootCauseMessage((Throwable)exception));
    }

    public static void error(Exception e, Class<?> c) {
        e.printStackTrace();
    }

    public static void error(Throwable e, Class<?> c) {
        e.printStackTrace();
    }
}

