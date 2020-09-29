/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.workers;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.orm.Database;
import tech.bluemail.platform.parsers.TypesParser;
import tech.bluemail.platform.remote.SSH;
import tech.bluemail.platform.utils.Strings;
import tech.bluemail.platform.workers.ServerWorker;

public class SenderWorker
extends Thread {
    public int dropId;
    public SSH ssh;
    public File pickupFile;
    public File pickupsFolder;

    public SenderWorker(int dropId, SSH ssh, File pickupFile, File pickupsFolder) {
        this.dropId = dropId;
        this.ssh = ssh;
        this.pickupFile = pickupFile;
        this.pickupsFolder = pickupsFolder;
    }

    @Override
    public void run() {
        try {
            if (this.ssh == null) return;
            if (this.pickupFile == null) return;
            if (!this.pickupFile.exists()) return;
            int progress = TypesParser.safeParseInt(String.valueOf(this.pickupFile.getName().split("\\_")[2]));
            String file = "/var/spool/bluemail/tmp/pickup_" + Strings.getSaltString(20, true, true, true, false) + ".txt";
            this.ssh.uploadFile(this.pickupFile.getAbsolutePath(), file);
            this.ssh.cmd("mv " + file + " /var/spool/bluemail/pickup/");
            if (this.dropId <= 0) return;
            if (!this.DropStatus().equalsIgnoreCase("interrupted")) {
                ServerWorker.updateDrop(this.dropId, progress);
                return;
            }
            if (this.pickupsFolder.exists()) {
                this.deleteDirectoryStream(this.pickupsFolder.toPath());
            }
            if (!this.pickupFile.exists()) return;
            this.deleteDirectoryStream(this.pickupFile.toPath());
            return;
        }
        catch (Exception e) {
            Logger.error(e, SenderWorker.class);
        }
    }

    public void deleteDirectoryStream(Path path) throws IOException {
        Files.walk(path, new FileVisitOption[0]).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    public String DropStatus() {
        String status = "";
        try {
            List<LinkedHashMap<String, Object>> result = Database.get("master").executeQuery("SELECT status FROM production.drops WHERE id =" + this.dropId, null, 0);
            if (result.isEmpty()) return status;
            return (String)result.get(0).get("status");
        }
        catch (Exception e) {
            Logger.error(e, ServerWorker.class);
        }
        return status;
    }
}

