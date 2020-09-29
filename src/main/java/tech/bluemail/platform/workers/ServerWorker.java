/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.workers;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import tech.bluemail.platform.components.DropComponent;
import tech.bluemail.platform.components.RotatorComponent;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.exceptions.ThreadException;
import tech.bluemail.platform.helpers.DropsHelper;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.models.admin.Server;
import tech.bluemail.platform.models.admin.Vmta;
import tech.bluemail.platform.orm.Database;
import tech.bluemail.platform.parsers.TypesParser;
import tech.bluemail.platform.remote.SSH;
import tech.bluemail.platform.utils.Strings;
import tech.bluemail.platform.workers.PickupWorker;
import tech.bluemail.platform.workers.SenderWorker;

public class ServerWorker
extends Thread {
    public DropComponent drop;
    public Server server;
    public List<Vmta> vmtas;
    public int offset;
    public int limit;
    public String query;
    public List<PickupWorker> pickupWorkers = new ArrayList<PickupWorker>();
    public List<SenderWorker> sendersWorkers = new ArrayList<SenderWorker>();

    public ServerWorker(DropComponent drop, Server server, List<Vmta> vmtas, int offset, int limit) {
        this.drop = drop;
        this.server = server;
        this.vmtas = vmtas;
        this.offset = offset;
        this.limit = limit;
        if (this.drop.isSend) {
            this.query = "SELECT * FROM (";
            this.drop.lists.entrySet().stream().map(en -> {
                this.query = this.query + "SELECT id,'" + String.valueOf(en.getValue()) + "' AS table,'" + String.valueOf(en.getKey()) + "' AS list_id,fname,lname,email";
                return en;
            }).map(en -> {
                this.query = this.query + (String.valueOf(en.getValue()).contains("seeds") ? ",generate_series(1," + drop.emailsPerSeeds + ") AS serie" : ",id AS serie");
                return en;
            }).forEachOrdered(en -> {
                this.query = this.query + " FROM " + String.valueOf(en.getValue()) + " UNION ALL ";
            });
            this.query = this.query.substring(0, this.query.length() - 10) + " WHERE (offers_excluded IS NULL OR offers_excluded = '' OR NOT ('" + this.drop.offerId + "' = ANY(string_to_array(offers_excluded,',')))) ORDER BY id OFFSET " + this.drop.dataStart + " LIMIT " + this.drop.dataCount + ") As Sub OFFSET " + this.offset + " LIMIT " + this.limit;
        }
        if (!this.vmtas.isEmpty()) {
            int rotation = this.drop.isSend ? this.drop.vmtasRotation : this.drop.testEmails.length;
            this.drop.vmtasRotator = new RotatorComponent(this.vmtas, rotation);
        }
        this.drop.pickupsFolder = System.getProperty("base.path") + "/tmp/pickups/server_" + this.server.id + "_" + Strings.getSaltString(20, true, true, true, false);
        new File(this.drop.pickupsFolder).mkdirs();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void run() {
        SSH ssh = null;
        boolean errorOccured = false;
        boolean isStopped = false;
        try {
            ExecutorService senderExecutor;
            int pickupsFile2;
            File[] arrfile;
            SenderWorker senderWorker;
            if (this.server == null) return;
            if (this.server.id <= 0) return;
            if (this.vmtas.isEmpty()) return;
            ssh = this.server.server_auth != null && !"".equalsIgnoreCase(this.server.server_auth) && Integer.parseInt(this.server.server_auth) == 1 ? SSH.SSHKey(this.server.mainIp, this.server.username, String.valueOf(this.server.sshPort), "/home/keys/id_rsa") : SSH.SSHPassword(this.server.mainIp, String.valueOf(this.server.sshPort), this.server.username, this.server.password);
            ssh.connect();
            if (this.drop.uploadImages) {
                DropsHelper.uploadImage(this.drop, ssh);
            }
            if (this.vmtas.isEmpty()) {
                throw new Exception("No Vmtas Found !");
            }
            List<Object> result = null;
            if (this.drop.isSend) {
                result = Database.get("lists").executeQuery(this.query, null, 1);
                this.drop.emailsCount = result.size();
            } else {
                result = new ArrayList();
                if (this.drop.testEmails != null && this.drop.testEmails.length > 0) {
                    for (Vmta vmta : this.vmtas) {
                        if (vmta == null) continue;
                        for (String testEmail : this.drop.testEmails) {
                            LinkedHashMap<String, Object> tmp = new LinkedHashMap<String, Object>();
                            tmp.put("id", 0);
                            tmp.put("email", testEmail.trim());
                            tmp.put("table", "");
                            tmp.put("list_id", 0);
                            result.add(tmp);
                        }
                    }
                }
            }
            if (this.drop.isSend && this.drop.isNewDrop) {
                DropsHelper.saveDrop(this.drop, this.server);
                if (this.drop.id > 0) {
                    if (this.vmtas.isEmpty()) {
                        throw new Exception("No Vmtas Found !");
                    }
                    int vmtasTotal = (int)Math.ceil(this.drop.emailsCount / this.vmtas.size());
                    int vmtasRest = this.drop.emailsCount - vmtasTotal * this.vmtas.size();
                    int index = 0;
                    if (!this.vmtas.isEmpty()) {
                        for (Vmta vmta : this.vmtas) {
                            if (index < vmtasRest) {
                                DropsHelper.saveDropVmta(this.drop, vmta, vmtasTotal + 1);
                            } else {
                                DropsHelper.saveDropVmta(this.drop, vmta, vmtasTotal);
                            }
                            ++index;
                        }
                    }
                }
            }
            if (this.drop.isSend) {
                DropsHelper.writeThreadStatusFile(this.server.id, this.drop.pickupsFolder);
            }
            if (!this.drop.isSend) {
                this.drop.emailsCount = this.drop.testEmails.length * this.vmtas.size();
            }
            this.drop.batch = this.drop.batch > this.drop.emailsCount ? this.drop.emailsCount : this.drop.batch;
            this.drop.batch = this.drop.batch == 0 ? 1 : this.drop.batch;
            ExecutorService pickupsExecutor = Executors.newFixedThreadPool(100);
            if (this.drop.batch == 0) {
                throw new Exception("Batch should be greather than 0 !");
            }
            int pickupsNumber = this.drop.emailsCount % this.drop.batch == 0 ? (int)Math.ceil(this.drop.emailsCount / this.drop.batch) : (int)Math.ceil(this.drop.emailsCount / this.drop.batch) + 1;
            int start = 0;
            int finish = this.drop.batch;
            Vmta periodVmta = null;
            PickupWorker worker = null;
            for (int i = 0; i < pickupsNumber; ++i) {
                if (this.drop != null && this.drop.isSend && this.drop.id > 0) {
                    String status = this.DropStatus();
                    if (DropsHelper.hasToStopDrop(this.server.id, this.drop.pickupsFolder) || status == "interrupted") {
                        this.interrupt();
                        pickupsExecutor.shutdownNow();
                        this.interruptDrop();
                        isStopped = true;
                        this.drop.isStoped = true;
                        break;
                    }
                }
                if (!(isStopped || this.isInterrupted() || this.drop.isStoped)) {
                    periodVmta = "emails-per-period".equalsIgnoreCase(this.drop.vmtasEmailsProcces) ? this.drop.getCurrentVmta() : null;
                    worker = new PickupWorker(i, this.drop, this.server, result.subList(start, finish), periodVmta);
                    worker.setUncaughtExceptionHandler(new ThreadException());
                    pickupsExecutor.submit(worker);
                    this.pickupWorkers.add(worker);
                    start += this.drop.batch;
                    if ((finish += this.drop.batch) > result.size()) {
                        finish = result.size();
                    }
                    if (start < result.size()) continue;
                    break;
                }
                pickupsExecutor.shutdownNow();
                this.interrupt();
                this.pickupWorkers.forEach(previousWorker -> {
                    if (!previousWorker.isAlive()) return;
                    previousWorker.interrupt();
                });
                this.deleteDirectoryStream(new File(this.drop.pickupsFolder).toPath());
                break;
            }
            pickupsExecutor.shutdown();
            pickupsExecutor.awaitTermination(1L, TimeUnit.DAYS);
            if (!isStopped && !this.drop.isStoped) {
                File[] pickupsFiles = new File(this.drop.pickupsFolder).listFiles();
                if (pickupsFiles == null) return;
                if (pickupsFiles.length <= 0) return;
                if (!ssh.isConnected()) return;
                if (this.drop.isStoped) return;
                File[] tmp = this.drop.isSend ? new File[pickupsFiles.length - 1] : new File[pickupsFiles.length];
                int idx = 0;
                for (File pickupsFile2 : pickupsFiles) {
                    if (!pickupsFile2.getName().startsWith("pickup_")) continue;
                    tmp[idx] = pickupsFile2;
                    ++idx;
                }
                pickupsFiles = tmp;
                Arrays.sort(pickupsFiles, (f1, f2) -> new Integer(TypesParser.safeParseInt(((File)f1).getName().split("_")[1])).compareTo(TypesParser.safeParseInt(((File)f2).getName().split("_")[1])));
                senderExecutor = Executors.newFixedThreadPool(100);
                senderWorker = null;
                arrfile = pickupsFiles;
                pickupsFile2 = arrfile.length;
            } else {
                pickupsExecutor.shutdownNow();
                this.interrupt();
                this.pickupWorkers.forEach(previousWorker -> {
                    if (!previousWorker.isAlive()) return;
                    previousWorker.interrupt();
                });
                this.deleteDirectoryStream(new File(this.drop.pickupsFolder).toPath());
                return;
            }
            for (int i = 0; i < pickupsFile2; ++i) {
                File pickupsFile3 = arrfile[i];
                if (this.drop != null && this.drop.isSend && this.drop.id > 0) {
                    String status = this.DropStatus();
                    if (DropsHelper.hasToStopDrop(this.server.id, this.drop.pickupsFolder) || status == "interrupted") {
                        senderExecutor.shutdownNow();
                        this.interruptDrop();
                        isStopped = true;
                        break;
                    }
                }
                if (!isStopped && !this.drop.isStoped) {
                    senderWorker = new SenderWorker(this.drop.id, ssh, pickupsFile3, new File(this.drop.pickupsFolder));
                    senderWorker.setUncaughtExceptionHandler(new ThreadException());
                    senderExecutor.submit(senderWorker);
                    this.sendersWorkers.add(senderWorker);
                    if (this.drop.delay <= 0L) continue;
                    Thread.sleep(this.drop.delay);
                    continue;
                }
                senderExecutor.shutdownNow();
                this.interrupt();
                this.sendersWorkers.forEach(previousWorker -> {
                    if (!previousWorker.isAlive()) return;
                    previousWorker.interrupt();
                });
                break;
            }
            senderExecutor.shutdown();
            senderExecutor.awaitTermination(1L, TimeUnit.DAYS);
            return;
        }
        catch (Exception e) {
            if (this.drop != null && this.drop.isSend && this.drop.id > 0) {
                this.errorDrop();
            }
            Logger.error(e, ServerWorker.class);
            errorOccured = true;
            return;
        }
        finally {
            this.finishProccess(ssh, errorOccured, isStopped);
        }
    }

    public static synchronized void updateDrop(int dropId, int progress) throws DatabaseException {
        Database.get("master").executeUpdate("UPDATE production.drops SET sent_progress = sent_progress + '" + progress + "'  WHERE id = ?", new Object[]{dropId}, 0);
    }

    public void finishProccess(SSH ssh, boolean errorOccured, boolean isStopped) {
        try {
            if (ssh != null && ssh.isConnected()) {
                ssh.disconnect();
            }
            if (this.drop == null) return;
            if (this.drop.id > 0 && !errorOccured && !isStopped) {
                int progress = 0;
                List<LinkedHashMap<String, Object>> result = Database.get("master").executeQuery("SELECT sent_progress FROM production.drops WHERE id =" + this.drop.id, null, 0);
                if (!result.isEmpty()) {
                    progress = (Integer)result.get(0).get("sent_progress");
                    if (progress == this.drop.emailsCount) {
                        Database.get("master").executeUpdate("UPDATE production.drops SET status = 'completed' , finish_time = ?  WHERE id = ?", new Object[]{new Timestamp(System.currentTimeMillis()), this.drop.id}, 0);
                    } else {
                        System.out.println("NOT COUNT");
                    }
                }
            }
            FileUtils.deleteDirectory(new File(this.drop.pickupsFolder));
            return;
        }
        catch (Exception e) {
            Logger.error(e, ServerWorker.class);
        }
    }

    public void errorDrop() {
        try {
            Database.get("master").executeUpdate("UPDATE production.drops SET status = 'error' , finish_time = ?  WHERE id = ?", new Object[]{new Timestamp(System.currentTimeMillis()), this.drop.id}, 0);
            if (this.drop == null) return;
            FileUtils.deleteDirectory(new File(this.drop.pickupsFolder));
            return;
        }
        catch (Exception e) {
            Logger.error(e, ServerWorker.class);
        }
    }

    public void interruptDrop() {
        try {
            Database.get("master").executeUpdate("UPDATE production.drops SET status = 'interrupted' , finish_time = ?  WHERE id = ?", new Object[]{new Timestamp(System.currentTimeMillis()), this.drop.id}, 0);
            if (this.drop == null) return;
            FileUtils.deleteDirectory(new File(this.drop.pickupsFolder));
            return;
        }
        catch (Exception e) {
            Logger.error(e, ServerWorker.class);
        }
    }

    public String DropStatus() {
        String status = "";
        try {
            List<LinkedHashMap<String, Object>> result = Database.get("master").executeQuery("SELECT status FROM production.drops WHERE id =" + this.drop.id, null, 0);
            if (result.isEmpty()) return status;
            return (String)result.get(0).get("status");
        }
        catch (Exception e) {
            Logger.error(e, ServerWorker.class);
        }
        return status;
    }

    public void deleteDirectoryStream(Path path) throws IOException {
        Files.walk(path, new FileVisitOption[0]).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
}

