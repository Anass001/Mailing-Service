package tech.bluemail.platform.controllers;

import tech.bluemail.platform.interfaces.*;
import tech.bluemail.platform.security.*;
import org.apache.commons.io.*;
import tech.bluemail.platform.helpers.*;
import tech.bluemail.platform.models.admin.*;
import tech.bluemail.platform.components.*;
import org.apache.commons.lang.*;
import java.io.*;
import tech.bluemail.platform.workers.*;
import tech.bluemail.platform.exceptions.*;
import java.util.concurrent.*;
import java.util.*;

public class DropsSender implements Controller
{
    public static volatile RotatorComponent PLACEHOLDERS_ROTATOR;
    public static volatile RotatorComponent HEADERS_ROTATOR;
    public static volatile RotatorComponent AUTOREPLAY_ROTATOR;
    
    public DropsSender() throws Exception {
        super();
        License.check();
    }
    
    @Override
    public void start(final String[] parameters) throws Exception {
        final File dropFile = new File(parameters[1]);
        if (!dropFile.exists()) {
            throw new Exception("No Drop File Found !");
        }
        final DropComponent drop = DropsHelper.parseDropFile(FileUtils.readFileToString(dropFile));
        if (drop != null) {
            DropsSender.PLACEHOLDERS_ROTATOR = (drop.hasPlaceholders ? new RotatorComponent(Arrays.asList(drop.placeholders), drop.placeholdersRotation) : null);
            DropsSender.HEADERS_ROTATOR = new RotatorComponent(Arrays.asList(drop.headers), drop.headersRotation);
            if (drop.autoReplyEmails != null && drop.autoReplyEmails.length > 0) {
                DropsSender.AUTOREPLAY_ROTATOR = new RotatorComponent(Arrays.asList(drop.autoReplyEmails), drop.autoResponseRotation);
            }
            if (!drop.servers.isEmpty() && !drop.vmtas.isEmpty()) {
                final ExecutorService serversExecutor = Executors.newFixedThreadPool(drop.servers.size());
                List<Vmta> serverVmtas = null;
                int offset = 0;
                int vmtasLimit = 0;
                int serverLimit = 0;
                int limitRest = 0;
                if (drop.isSend) {
                    if ("servers".equalsIgnoreCase(drop.emailsSplitType)) {
                        serverLimit = (int)Math.ceil(drop.dataCount / drop.servers.size());
                        limitRest = drop.dataCount - serverLimit * drop.servers.size();
                    }
                    else {
                        vmtasLimit = (int)Math.ceil(drop.dataCount / drop.vmtas.size());
                        limitRest = drop.dataCount - vmtasLimit * drop.vmtas.size();
                    }
                }
                for (int i = 0; i < drop.servers.size(); ++i) {
                    final Server server = drop.servers.get(i);
                    if (server != null && server.id > 0) {
                        if (drop.isSend && "vmtas".equalsIgnoreCase(drop.emailsSplitType)) {
                            serverLimit = 0;
                        }
                        serverVmtas = new ArrayList<Vmta>();
                        if (!drop.vmtas.isEmpty()) {
                            for (final Vmta vmta : drop.vmtas) {
                                if (vmta.serverId == server.id) {
                                    serverVmtas.add(vmta);
                                    if (!drop.isSend || !"vmtas".equalsIgnoreCase(drop.emailsSplitType)) {
                                        continue;
                                    }
                                    serverLimit += vmtasLimit;
                                }
                            }
                        }
                        if (i == drop.servers.size() - 1) {
                            serverLimit += limitRest;
                        }
                        final ServerWorker worker = new ServerWorker((DropComponent)SerializationUtils.clone((Serializable)drop), server, serverVmtas, offset, serverLimit);
                        worker.setUncaughtExceptionHandler(new ThreadException());
                        worker.setName("Drop-Thread-" + server.id + "-" + i);
                        serversExecutor.submit(worker);
                        offset += serverLimit;
                    }
                }
                serversExecutor.shutdown();
                serversExecutor.awaitTermination(10L, TimeUnit.DAYS);
            }
            if (dropFile.exists()) {
                dropFile.delete();
            }
            return;
        }
        throw new Exception("No Drop Content Found !");
    }
    
    public static synchronized String getCurrentPlaceHolder() {
        return (String)((DropsSender.PLACEHOLDERS_ROTATOR != null) ? DropsSender.PLACEHOLDERS_ROTATOR.getCurrentValue() : "");
    }
    
    public static synchronized void rotatePlaceHolders() {
        if (DropsSender.PLACEHOLDERS_ROTATOR != null) {
            DropsSender.PLACEHOLDERS_ROTATOR.rotate();
        }
    }
    
    public static synchronized String getCurrentHeader() {
        return (String)((DropsSender.HEADERS_ROTATOR != null) ? DropsSender.HEADERS_ROTATOR.getCurrentValue() : "");
    }
    
    public static synchronized void rotateHeaders() {
        if (DropsSender.HEADERS_ROTATOR != null) {
            DropsSender.HEADERS_ROTATOR.rotate();
        }
    }
    
    public static synchronized String getCurrentAutoReplay() {
        return (String)((DropsSender.AUTOREPLAY_ROTATOR != null) ? DropsSender.AUTOREPLAY_ROTATOR.getCurrentValue() : "");
    }
    
    public static synchronized void rotateAutoReplay() {
        if (DropsSender.AUTOREPLAY_ROTATOR != null) {
            DropsSender.AUTOREPLAY_ROTATOR.rotate();
        }
    }
}
