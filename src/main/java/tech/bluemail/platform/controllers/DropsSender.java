/*
 * Decompiled with CFR <Could not determine version>.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.SerializationUtils
 */
package tech.bluemail.platform.controllers;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SerializationUtils;
import tech.bluemail.platform.components.DropComponent;
import tech.bluemail.platform.components.RotatorComponent;
import tech.bluemail.platform.exceptions.ThreadException;
import tech.bluemail.platform.helpers.DropsHelper;
import tech.bluemail.platform.interfaces.Controller;
import tech.bluemail.platform.models.admin.Server;
import tech.bluemail.platform.models.admin.Vmta;
import tech.bluemail.platform.security.License;
import tech.bluemail.platform.workers.ServerWorker;

public class DropsSender
implements Controller {
    public static volatile RotatorComponent PLACEHOLDERS_ROTATOR;
    public static volatile RotatorComponent HEADERS_ROTATOR;
    public static volatile RotatorComponent AUTOREPLAY_ROTATOR;

    public DropsSender() throws Exception {
        License.check();
    }

    @Override
    public void start(String[] parameters) throws Exception {
        File dropFile = new File(parameters[1]);
        if (!dropFile.exists()) throw new Exception("No Drop File Found !");
        DropComponent drop = DropsHelper.parseDropFile(FileUtils.readFileToString(dropFile));
        if (drop == null) throw new Exception("No Drop Content Found !");
        PLACEHOLDERS_ROTATOR = drop.hasPlaceholders ? new RotatorComponent(Arrays.asList(drop.placeholders), drop.placeholdersRotation) : null;
        HEADERS_ROTATOR = new RotatorComponent(Arrays.asList(drop.headers), drop.headersRotation);
        if (drop.autoReplyEmails != null && drop.autoReplyEmails.length > 0) {
            AUTOREPLAY_ROTATOR = new RotatorComponent(Arrays.asList(drop.autoReplyEmails), drop.autoResponseRotation);
        }
        if (!drop.servers.isEmpty() && !drop.vmtas.isEmpty()) {
            ExecutorService serversExecutor = Executors.newFixedThreadPool(drop.servers.size());
            ArrayList<Vmta> serverVmtas = null;
            int offset = 0;
            int vmtasLimit = 0;
            int serverLimit = 0;
            int limitRest = 0;
            if (drop.isSend) {
                if ("servers".equalsIgnoreCase(drop.emailsSplitType)) {
                    serverLimit = (int)Math.ceil(drop.dataCount / drop.servers.size());
                    limitRest = drop.dataCount - serverLimit * drop.servers.size();
                } else {
                    vmtasLimit = (int)Math.ceil(drop.dataCount / drop.vmtas.size());
                    limitRest = drop.dataCount - vmtasLimit * drop.vmtas.size();
                }
            }
            for (int i = 0; i < drop.servers.size(); ++i) {
                Server server = drop.servers.get(i);
                if (server == null || server.id <= 0) continue;
                if (drop.isSend && "vmtas".equalsIgnoreCase(drop.emailsSplitType)) {
                    serverLimit = 0;
                }
                serverVmtas = new ArrayList<Vmta>();
                if (!drop.vmtas.isEmpty()) {
                    for (Vmta vmta : drop.vmtas) {
                        if (vmta.serverId != server.id) continue;
                        serverVmtas.add(vmta);
                        if (!drop.isSend || !"vmtas".equalsIgnoreCase(drop.emailsSplitType)) continue;
                        serverLimit += vmtasLimit;
                    }
                }
                if (i == drop.servers.size() - 1) {
                    serverLimit += limitRest;
                }
                ServerWorker worker = new ServerWorker((DropComponent)SerializationUtils.clone((Serializable)drop), server, serverVmtas, offset, serverLimit);
                worker.setUncaughtExceptionHandler(new ThreadException());
                worker.setName("Drop-Thread-" + server.id + "-" + i);
                serversExecutor.submit(worker);
                offset += serverLimit;
            }
            serversExecutor.shutdown();
            serversExecutor.awaitTermination(10L, TimeUnit.DAYS);
        }
        if (!dropFile.exists()) return;
        dropFile.delete();
    }

    public static synchronized String getCurrentPlaceHolder() {
        if (PLACEHOLDERS_ROTATOR == null) return "";
        String string = (String)PLACEHOLDERS_ROTATOR.getCurrentValue();
        return string;
    }

    public static synchronized void rotatePlaceHolders() {
        if (PLACEHOLDERS_ROTATOR == null) return;
        PLACEHOLDERS_ROTATOR.rotate();
    }

    public static synchronized String getCurrentHeader() {
        if (HEADERS_ROTATOR == null) return "";
        String string = (String)HEADERS_ROTATOR.getCurrentValue();
        return string;
    }

    public static synchronized void rotateHeaders() {
        if (HEADERS_ROTATOR == null) return;
        HEADERS_ROTATOR.rotate();
    }

    public static synchronized String getCurrentAutoReplay() {
        if (AUTOREPLAY_ROTATOR == null) return "";
        String string = (String)AUTOREPLAY_ROTATOR.getCurrentValue();
        return string;
    }

    public static synchronized void rotateAutoReplay() {
        if (AUTOREPLAY_ROTATOR == null) return;
        AUTOREPLAY_ROTATOR.rotate();
    }
}

