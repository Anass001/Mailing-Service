package tech.bluemail.platform.bootstrap;

import java.io.*;
import tech.bluemail.platform.logging.*;
import tech.bluemail.platform.orm.*;
import tech.bluemail.platform.security.*;
import org.apache.commons.codec.binary.*;
import tech.bluemail.platform.controllers.*;
import tech.bluemail.platform.interfaces.*;

public class Start
{
    public Start() {
        super();
    }
    
    public static void main(final String[] args) {
        final long startTime = System.currentTimeMillis();
        try {
            System.setProperty("base.path", new File(Start.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getParentFile().getParentFile().getParentFile().getAbsolutePath().replaceAll("%20", " "));
            Logger.initlog4Java();
            Database.init();
            License.check();
            if (args.length == 0) {
                throw new Exception("No Parameters Passed !");
            }
            Controller controller = null;
            final String s = new String(Base64.encodeBase64(args[0].getBytes()));
            switch (s) {
                case "c2VuZF9wcm9jY2Vzcw==": {
                    controller = new DropsSender();
                    break;
                }
                case "c2VuZF9zdGF0cw==": {
                    controller = new StatsCalculator();
                    break;
                }
                case "Ym91bmNlX2NsZWFu": {
                    controller = new BounceCleaner();
                    break;
                }
                case "c3VwcHJlc3Npb25fcHJvY2Nlc3M=": {
                    controller = new SuppressionManager();
                    break;
                }
                case "ZGF0YV9jbGVhbg==": {
                    controller = new dataclean();
                    break;
                }
                default: {
                    throw new Exception("Unsupported Action !");
                }
            }
            if (controller != null) {
                controller.start(args);
            }
        }
        catch (Exception e) {
            Logger.error(e, Start.class);
        }
        finally {
            final long end = System.currentTimeMillis();
            System.out.println("Job Completed in : " + (end - startTime) + " miliseconds");
            System.exit(0);
        }
    }
}
