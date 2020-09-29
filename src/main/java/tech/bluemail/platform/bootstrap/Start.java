/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.bootstrap;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import org.apache.commons.codec.binary.Base64;
import tech.bluemail.platform.controllers.BounceCleaner;
import tech.bluemail.platform.controllers.DropsSender;
import tech.bluemail.platform.controllers.StatsCalculator;
import tech.bluemail.platform.controllers.SuppressionManager;
import tech.bluemail.platform.controllers.dataclean;
import tech.bluemail.platform.interfaces.Controller;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.orm.Database;
import tech.bluemail.platform.security.License;

public class Start {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     * Enabled unnecessary exception pruning
     */
    public static void main(String[] args) {
        startTime = System.currentTimeMillis();
        try {
            System.setProperty("base.path", new File(Start.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getParentFile().getParentFile().getParentFile().getAbsolutePath().replaceAll("%20", " "));
            Logger.initlog4Java();
            Database.init();
            License.check();
            if (args.length == 0) {
                throw new Exception("No Parameters Passed !");
            }
            controller = null;
            var4_6 = new String(Base64.encodeBase64(args[0].getBytes()));
            var5_7 = -1;
            switch (var4_6.hashCode()) {
                case -507738556: {
                    if (!var4_6.equals("c2VuZF9wcm9jY2Vzcw==")) break;
                    var5_7 = 0;
                    break;
                }
                case -592331332: {
                    if (!var4_6.equals("c2VuZF9zdGF0cw==")) break;
                    var5_7 = 1;
                    break;
                }
                case -2010054304: {
                    if (!var4_6.equals("Ym91bmNlX2NsZWFu")) break;
                    var5_7 = 2;
                    break;
                }
                case 1552951987: {
                    if (!var4_6.equals("c3VwcHJlc3Npb25fcHJvY2Nlc3M=")) break;
                    var5_7 = 3;
                    break;
                }
                case 928352289: {
                    if (!var4_6.equals("ZGF0YV9jbGVhbg==")) break;
                    var5_7 = 4;
                }
            }
            switch (var5_7) {
                case 0: {
                    controller = new DropsSender();
                    ** break;
                }
                case 1: {
                    controller = new StatsCalculator();
                    ** break;
                }
                case 2: {
                    controller = new BounceCleaner();
                    ** break;
                }
                case 3: {
                    controller = new SuppressionManager();
                    ** break;
                }
                case 4: {
                    controller = new dataclean();
                    ** break;
                }
            }
            throw new Exception("Unsupported Action !");
lbl50: // 5 sources:
            if (controller == null) return;
            controller.start(args);
            return;
        }
        catch (Exception e) {
            Logger.error(e, Start.class);
            return;
        }
        finally {
            end = System.currentTimeMillis();
            System.out.println("Job Completed in : " + (end - startTime) + " miliseconds");
            System.exit(0);
        }
    }
}

