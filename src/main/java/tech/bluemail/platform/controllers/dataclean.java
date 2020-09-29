/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.controllers;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import tech.bluemail.platform.controllers.StatsCalculator;
import tech.bluemail.platform.controllers.SuppressionManager;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.interfaces.Controller;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.models.admin.DataList;
import tech.bluemail.platform.orm.Database;
import tech.bluemail.platform.parsers.TypesParser;
import tech.bluemail.platform.security.License;

public class dataclean
implements Controller {
    public static int COUNT = 0;
    public static int INDEX = 1;

    public dataclean() throws Exception {
        License.check();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void start(String[] parameters) throws Exception {
        int proccessId = TypesParser.safeParseInt(parameters[1]);
        boolean errorOccured = false;
        try {
            System.out.println("Cleaning ...");
            String listName = parameters[2];
            String schema = listName.split("\\.")[0];
            String table = listName.split("\\.")[1];
            String type = table.split("_")[0];
            String flag = table.split("_")[1];
            String tablePrefix = table.replaceAll(flag + "_", "").replaceAll(type + "_", "");
            System.out.println("Table -> " + table);
            int ispId = TypesParser.safeParseInt(parameters[3]);
            int userId = TypesParser.safeParseInt(parameters[4]);
            int serverId = parameters.length > 5 ? TypesParser.safeParseInt(parameters[5]) : 0;
            ArrayList<String> bouncesFiles = new ArrayList<String>();
            ArrayList<String> deliverdFiles = new ArrayList<String>();
            ArrayList<String> bounceLinesToRemove = new ArrayList<String>();
            ArrayList<String> deliverdLinesToRemove = new ArrayList<String>();
            String[] lineParts = new String[]{};
            int dropId = 0;
            int ipId = 0;
            int clientId = 0;
            int listId = 0;
            int bounceCleand = 0;
            int dileverdCleand = 0;
            boolean listCount = false;
            DataList list = (DataList)DataList.first(DataList.class, "name = ?", new Object[]{listName});
            if (list == null) throw new Exception("Data List : " + listName + " Does not Exists !");
            if (list.id == 0) {
                throw new Exception("Data List : " + listName + " Does not Exists !");
            }
            StatsCalculator controllerCalcul = new StatsCalculator();
            String[] args = new String[]{"send_stats", parameters[5]};
            controllerCalcul.start(args);
            if (TypesParser.safeParseInt(parameters[5]) != 0) {
                String logsFolder = new File(System.getProperty("base.path")).getAbsolutePath() + File.separator + "tmp" + File.separator + "pmta-logs" + File.separator + "server_" + serverId;
                if (!new File(logsFolder).exists()) {
                    this.interruptProccess(proccessId);
                    errorOccured = true;
                    return;
                }
                List filesInFolder = Files.walk(Paths.get(logsFolder, new String[0]), new FileVisitOption[0]).filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).map(Path::toFile).collect(Collectors.toList());
                for (int i = 0; i < filesInFolder.size(); ++i) {
                    File pmtaFile = new File(((File)filesInFolder.get(i)).toString());
                    if (pmtaFile == null || !pmtaFile.isFile()) continue;
                    String[] abPath = pmtaFile.getAbsolutePath().split(File.separator);
                    String FileType = abPath[7];
                    if (FileType != null && FileType.equalsIgnoreCase("bounces")) {
                        bouncesFiles.add(pmtaFile.getAbsolutePath());
                    }
                    if (FileType == null || !FileType.equalsIgnoreCase("delivered")) continue;
                    deliverdFiles.add(pmtaFile.getAbsolutePath());
                }
                this.updateCount(listName);
                if (bouncesFiles != null && bouncesFiles.size() > 0) {
                    List<String> lines = null;
                    for (String bouncesFile : bouncesFiles) {
                        File bounce = new File(bouncesFile);
                        if (!bounce.isFile() || (lines = FileUtils.readLines(bounce)) == null || lines.isEmpty()) continue;
                        for (String line : lines) {
                            if ("".equals(line) || (lineParts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1)).length != 12 || !"hardbnc".equalsIgnoreCase(lineParts[1]) || "".equalsIgnoreCase(lineParts[10])) continue;
                            dropId = TypesParser.safeParseInt(lineParts[10].split("_")[0]);
                            ipId = TypesParser.safeParseInt(lineParts[10].split("_")[1]);
                            clientId = TypesParser.safeParseInt(lineParts[10].split("_")[2]);
                            listId = TypesParser.safeParseInt(lineParts[10].split("_")[3]);
                            if (listId != list.id) continue;
                            Database.get("lists").executeUpdate("DELETE FROM " + listName + " WHERE id = ?", new Object[]{clientId}, 0);
                            System.out.println("Deleitng Email ID -> " + clientId + " from list --> " + listName);
                            bounceLinesToRemove.add(line);
                            ++bounceCleand;
                            this.updateProccess(proccessId, "bounce");
                        }
                    }
                }
                if (deliverdFiles != null && deliverdFiles.size() > 0) {
                    List<String> lines = null;
                    for (String deliverdFile : deliverdFiles) {
                        File deliver = new File(deliverdFile);
                        if (!deliver.isFile() || (lines = FileUtils.readLines(deliver)) == null || lines.isEmpty()) continue;
                        for (String line : lines) {
                            if ("".equals(line) || (lineParts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1)).length != 12 || !"success".equalsIgnoreCase(lineParts[1]) || "".equalsIgnoreCase(lineParts[10])) continue;
                            dropId = TypesParser.safeParseInt(lineParts[10].split("_")[0]);
                            ipId = TypesParser.safeParseInt(lineParts[10].split("_")[1]);
                            clientId = TypesParser.safeParseInt(lineParts[10].split("_")[2]);
                            listId = TypesParser.safeParseInt(lineParts[10].split("_")[3]);
                            if (listId != list.id) continue;
                            deliverdLinesToRemove.add(line);
                            System.out.println("Email clean -> " + clientId);
                            ++dileverdCleand;
                            this.updateProccess(proccessId, "clean");
                        }
                    }
                }
                if (bounceCleand <= 0) {
                    if (bounceCleand <= 0) return;
                }
                String newName = "clean_" + flag + "_" + tablePrefix;
                Database.get("lists").executeUpdate("ALTER TABLE " + listName + " RENAME TO " + newName + ";", new Object[0], 0);
                Database.get("master").executeUpdate("UPDATE admin.data_lists SET name = '" + schema + "." + newName + "' WHERE id = ?", new Object[]{list.id}, 0);
                if (bounceLinesToRemove != null && bounceLinesToRemove.size() > 0) {
                    for (int i = 0; i < bounceLinesToRemove.size(); ++i) {
                        System.out.println("Removing this line of bounce  --> " + (String)bounceLinesToRemove.get(i));
                    }
                }
                if (deliverdLinesToRemove == null) return;
                if (deliverdLinesToRemove.size() <= 0) return;
                int i = 0;
                while (i < deliverdLinesToRemove.size()) {
                    System.out.println("Removing this line of delevred  --> " + (String)deliverdLinesToRemove.get(i));
                    ++i;
                }
                return;
            }
            System.out.println("Looking for all servers ... not yet ready ...");
            return;
        }
        catch (Exception e) {
            this.interruptProccess(proccessId);
            Logger.error(e, dataclean.class);
            errorOccured = true;
            return;
        }
        finally {
            if (!errorOccured) {
                this.finishProccess(proccessId);
            }
        }
    }

    public void updateProccess(int proccessId, String type) throws DatabaseException {
        int progress = (int)((double)this.getIndex() / (double)this.getCount() * 100.0);
        String update = "bounce".equalsIgnoreCase(type) ? " , hard_bounce = hard_bounce + 1 " : " , clean = clean + 1 ";
        Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET progress = '" + progress + "%' " + update + " WHERE Id = ?", new Object[]{proccessId}, 0);
        this.updateIndex();
    }

    public int getIndex() {
        return INDEX;
    }

    public void updateIndex() {
        ++INDEX;
    }

    public void updateCount(String listName) throws DatabaseException {
        List<LinkedHashMap<String, Object>> result = Database.get("lists").executeQuery("SELECT COUNT(id) AS count FROM " + listName + ";", null, 0);
        int size = Integer.parseInt(String.valueOf(result.get(0).get("count")));
        COUNT += size;
    }

    public int getCount() {
        return COUNT;
    }

    public void interruptProccess(int proccessId) {
        try {
            Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET status = 'error' , finish_time = ?  WHERE id = ?", new Object[]{new Timestamp(System.currentTimeMillis()), proccessId}, 0);
            return;
        }
        catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }

    public void finishProccess(int proccessId) {
        try {
            Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET status = 'completed' , progress = '100%' , finish_time = ?  WHERE id = ?", new Object[]{new Timestamp(System.currentTimeMillis()), proccessId}, 0);
            return;
        }
        catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }

    public void removeLineFromFile() {
    }
}

