package tech.bluemail.platform.controllers;

import tech.bluemail.platform.interfaces.*;
import tech.bluemail.platform.security.*;
import tech.bluemail.platform.models.admin.*;
import tech.bluemail.platform.parsers.*;
import tech.bluemail.platform.orm.*;
import java.io.*;
import java.time.*;
import tech.bluemail.platform.remote.*;
import tech.bluemail.platform.utils.*;
import tech.bluemail.platform.components.*;
import org.apache.commons.lang.*;
import org.apache.commons.io.*;
import tech.bluemail.platform.models.production.*;
import tech.bluemail.platform.logging.*;
import java.util.*;

public class StatsCalculator implements Controller
{
    public StatsCalculator() throws Exception {
        super();
        License.check();
    }
    
    @Override
    public void start(final String[] parameters) throws Exception {
        try {
            SSH ssh = null;
            List<Server> servers = new ArrayList<Server>();
            if (parameters != null && parameters.length > 1 && TypesParser.safeParseInt(parameters[1]) != 0) {
                final Integer ServerId = TypesParser.safeParseInt(parameters[1]);
                final Server serverObj = (Server)ActiveRecord.first(Server.class, "id = ? AND status_id = ?", new Object[] { ServerId, 1 });
                servers.add(serverObj);
            }
            else {
                servers = (List<Server>)ActiveRecord.all(Server.class, "status_id = ?", new Object[] { 1 });
            }
            if (servers == null || servers.isEmpty()) {
                throw new Exception("No Servers Found To Calculate Pmta Logs !");
            }
            for (final Server server : servers) {
                if (server != null) {
                    final String logsFolder = new File(System.getProperty("base.path")).getAbsolutePath() + File.separator + "tmp" + File.separator + "pmta-logs" + File.separator + "server_" + server.id;
                    final String today = LocalDate.now().toString();
                    if (!new File(logsFolder + "/" + today).exists()) {
                        new File(logsFolder).mkdirs();
                        new File(logsFolder + "/" + today + "/bounces/").mkdirs();
                        new File(logsFolder + "/" + today + "/delivered/").mkdirs();
                    }
                    if (server.server_auth != null && !"".equalsIgnoreCase(server.server_auth) && Integer.parseInt(server.server_auth) == 1) {
                        ssh = SSH.SSHKey(server.mainIp, server.username, String.valueOf(server.sshPort), "/home/keys/id_rsa");
                    }
                    else {
                        ssh = SSH.SSHPassword(server.mainIp, String.valueOf(server.sshPort), server.username, server.password);
                    }
                    ssh.connect();
                    if (!ssh.isConnected()) {
                        throw new Exception("Could not connect to the server : " + server.name + " !");
                    }
                    final String[] types = { "delivered", "bounces" };
                    String result = "";
                    String[] archiveFiles = new String[0];
                    String prefix = "";
                    String fileName = "";
                    for (final String type : types) {
                        fileName = today + "_" + type + ".csv";
                        prefix = ("delivered".equalsIgnoreCase(type) ? "d" : "b");
                        result = ssh.cmd("awk 'FNR > 1' /etc/pmta/" + type + "/archived/*.csv > /etc/pmta/" + type + "/archived/" + fileName + " && find /etc/pmta/" + type + "/archived/" + fileName);
                        archiveFiles = new String[0];
                        ssh.downloadFile("/etc/pmta/" + type + "/archived/" + fileName, logsFolder + File.separator + today + File.separator + type + File.separator + fileName);
                        ssh.cmd("zip -r " + today + "_" + type + "_" + Strings.getSaltString(10, true, true, true, false) + ".zip /etc/pmta/" + type + "/archived/* ");
                        ssh.cmd("mkdir -p /home/pmtaFilesBackUP");
                        ssh.cmd("mv " + today + "_" + type + ".zip /home/pmtaFilesBackUP/");
                        ssh.cmd("rm -rf /etc/pmta/" + type + "/archived/* ");
                    }
                    ssh.disconnect();
                    final HashMap<Integer, HashMap<Integer, AccountingComponent>> stats = new HashMap<Integer, HashMap<Integer, AccountingComponent>>();
                    String[] lineParts = new String[0];
                    int dropId = 0;
                    int ipId = 0;
                    File[] bounceFiles = new File(logsFolder + File.separator + today + File.separator + "bounces").listFiles();
                    bounceFiles = (File[])ArrayUtils.addAll((Object[])bounceFiles, (Object[])new File(logsFolder + File.separator + today + File.separator + "bounces").listFiles());
                    List<String> lines = new ArrayList<String>();
                    for (final File bounceFile : bounceFiles) {
                        if (bounceFile.isFile()) {
                            lines.addAll(FileUtils.readLines(bounceFile));
                        }
                    }
                    for (final String line : lines) {
                        if (!"".equals(line)) {
                            lineParts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                            if (lineParts.length != 12 || (!"hardbnc".equalsIgnoreCase(lineParts[1]) && !"other".equalsIgnoreCase(lineParts[1])) || "".equalsIgnoreCase(lineParts[10])) {
                                continue;
                            }
                            dropId = TypesParser.safeParseInt(lineParts[10].split("_")[0]);
                            ipId = TypesParser.safeParseInt(lineParts[10].split("_")[1]);
                            if (!stats.containsKey(dropId)) {
                                stats.put(dropId, new HashMap<Integer, AccountingComponent>());
                            }
                            if (stats.get(dropId).containsKey(ipId)) {
                                final AccountingComponent accountingComponent = stats.get(dropId).get(ipId);
                                ++accountingComponent.bounced;
                            }
                            else {
                                stats.get(dropId).put(ipId, new AccountingComponent(dropId, ipId, 0, 1));
                            }
                        }
                    }
                    File[] deliveredFiles = new File(logsFolder + File.separator + today + File.separator + "delivered").listFiles();
                    deliveredFiles = (File[])ArrayUtils.addAll((Object[])deliveredFiles, (Object[])new File(logsFolder + File.separator + today + File.separator + "delivered").listFiles());
                    lines = new ArrayList<String>();
                    for (final File deliveredFile : deliveredFiles) {
                        if (deliveredFile.isFile()) {
                            lines.addAll(FileUtils.readLines(deliveredFile));
                        }
                    }
                    for (final String line2 : lines) {
                        if (!"".equals(line2)) {
                            lineParts = line2.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                            if (lineParts.length != 12 || !"success".equalsIgnoreCase(lineParts[1]) || "".equalsIgnoreCase(lineParts[10])) {
                                continue;
                            }
                            dropId = TypesParser.safeParseInt(lineParts[10].split("_")[0]);
                            ipId = TypesParser.safeParseInt(lineParts[10].split("_")[1]);
                            if (!stats.containsKey(dropId)) {
                                stats.put(dropId, new HashMap<Integer, AccountingComponent>());
                            }
                            if (stats.get(dropId).containsKey(ipId)) {
                                final AccountingComponent accountingComponent2 = stats.get(dropId).get(ipId);
                                ++accountingComponent2.delivered;
                            }
                            else {
                                stats.get(dropId).put(ipId, new AccountingComponent(dropId, ipId, 1, 0));
                            }
                        }
                    }
                    for (final Map.Entry<Integer, HashMap<Integer, AccountingComponent>> statsEntry : stats.entrySet()) {
                        dropId = statsEntry.getKey();
                        final HashMap<Integer, AccountingComponent> value = statsEntry.getValue();
                        if (dropId > 0 && value != null && !value.isEmpty()) {
                            for (final Map.Entry<Integer, AccountingComponent> accountingEntry : value.entrySet()) {
                                ipId = accountingEntry.getKey();
                                final AccountingComponent accounting = accountingEntry.getValue();
                                final DropIp dropIp = (DropIp)ActiveRecord.first(DropIp.class, "drop_id = ? AND ip_id = ?", new Object[] { dropId, ipId });
                                if (dropIp != null) {
                                    dropIp.delivered = accounting.delivered;
                                    dropIp.bounced = accounting.bounced;
                                    dropIp.update();
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            Logger.error(e, StatsCalculator.class);
        }
    }
}
