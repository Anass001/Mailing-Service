package tech.bluemail.platform.logging;

import org.apache.log4j.*;
import java.net.*;
import org.slf4j.*;
import org.apache.commons.lang.exception.*;

public class Logger
{
    public Logger() {
        super();
    }
    
    public static void initlog4Java() {
        final URL propertiesFileURL = Logger.class.getResource("/tech/bluemail/platform/logging/log4j2.properties");
        PropertyConfigurator.configure(propertiesFileURL);
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
    }
    
    public static void info(final String msg) {
        LoggerFactory.getLogger("ApplicationInfoMessage").info(msg);
    }
    
    public static void debug(final String msg) {
        LoggerFactory.getLogger("ApplicationDebugMessage").debug(msg);
    }
    
    public static void error(final String msg) {
        LoggerFactory.getLogger("ApplicationErrorMessage").error(msg);
    }
    
    public static void error(final String msg, final Throwable exception) {
        LoggerFactory.getLogger("ApplicationErrorMessage").error(msg + ". Caused By : " + ExceptionUtils.getRootCauseMessage(exception));
    }
    
    public static void error(final Exception e, final Class<?> c) {
        e.printStackTrace();
    }
    
    public static void error(final Throwable e, final Class<?> c) {
        e.printStackTrace();
    }
}
