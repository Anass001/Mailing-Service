package tech.bluemail.platform.controllers;

import tech.bluemail.platform.interfaces.*;
import tech.bluemail.platform.security.*;
import tech.bluemail.platform.parsers.*;
import tech.bluemail.platform.models.admin.*;
import java.io.*;
import java.nio.file.*;
import java.util.function.*;
import java.util.stream.*;
import org.apache.commons.io.*;
import tech.bluemail.platform.orm.*;
import tech.bluemail.platform.logging.*;
import tech.bluemail.platform.exceptions.*;
import java.util.*;
import java.sql.*;

public class dataclean implements Controller
{
    public static int COUNT;
    public static int INDEX;
    
    public dataclean() throws Exception {
        super();
        License.check();
    }
    
    @Override
    public void start(final String[] parameters) throws Exception {
        final int proccessId = TypesParser.safeParseInt(parameters[1]);
        boolean errorOccured = false;
        try {
            System.out.println("Cleaning ...");
            final String listName = parameters[2];
            final String schema = listName.split("\\.")[0];
            final String table = listName.split("\\.")[1];
            final String type = table.split("_")[0];
            final String flag = table.split("_")[1];
            final String tablePrefix = table.replaceAll(flag + "_", "").replaceAll(type + "_", "");
            System.out.println("Table -> " + table);
            final int ispId = TypesParser.safeParseInt(parameters[3]);
            final int userId = TypesParser.safeParseInt(parameters[4]);
            final int serverId = (parameters.length > 5) ? TypesParser.safeParseInt(parameters[5]) : 0;
            final ArrayList<String> bouncesFiles = new ArrayList<String>();
            final ArrayList<String> deliverdFiles = new ArrayList<String>();
            final ArrayList<String> bounceLinesToRemove = new ArrayList<String>();
            final ArrayList<String> deliverdLinesToRemove = new ArrayList<String>();
            String[] lineParts = new String[0];
            int dropId = 0;
            int ipId = 0;
            int clientId = 0;
            int listId = 0;
            int bounceCleand = 0;
            int dileverdCleand = 0;
            final int listCount = 0;
            final DataList list = (DataList)ActiveRecord.first(DataList.class, "name = ?", new Object[] { listName });
            if (list == null || list.id == 0) {
                throw new Exception("Data List : " + listName + " Does not Exists !");
            }
            final Controller controllerCalcul = new StatsCalculator();
            final String[] args = { "send_stats", parameters[5] };
            controllerCalcul.start(args);
            if (TypesParser.safeParseInt(parameters[5]) != 0) {
                final String logsFolder = new File(System.getProperty("base.path")).getAbsolutePath() + File.separator + "tmp" + File.separator + "pmta-logs" + File.separator + "server_" + serverId;
                if (new File(logsFolder).exists()) {
                    final List<File> filesInFolder = Files.walk(Paths.get(logsFolder, new String[0]), new FileVisitOption[0]).filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).map((Function<? super Path, ?>)Path::toFile).collect((Collector<? super Object, ?, List<File>>)Collectors.toList());
                    for (int i = 0; i < filesInFolder.size(); ++i) {
                        final File pmtaFile = new File(filesInFolder.get(i).toString());
                        if (pmtaFile != null && pmtaFile.isFile()) {
                            final String[] abPath = pmtaFile.getAbsolutePath().split(File.separator);
                            final String FileType = abPath[7];
                            if (FileType != null && FileType.equalsIgnoreCase("bounces")) {
                                bouncesFiles.add(pmtaFile.getAbsolutePath());
                            }
                            if (FileType != null && FileType.equalsIgnoreCase("delivered")) {
                                deliverdFiles.add(pmtaFile.getAbsolutePath());
                            }
                        }
                    }
                    this.updateCount(listName);
                    if (bouncesFiles != null && bouncesFiles.size() > 0) {
                        List<String> lines = null;
                        for (final String bouncesFile : bouncesFiles) {
                            final File bounce = new File(bouncesFile);
                            if (bounce.isFile()) {
                                lines = FileUtils.readLines(bounce);
                                if (lines == null || lines.isEmpty()) {
                                    continue;
                                }
                                for (final String line : lines) {
                                    if (!"".equals(line)) {
                                        lineParts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                                        if (lineParts.length != 12 || !"hardbnc".equalsIgnoreCase(lineParts[1]) || "".equalsIgnoreCase(lineParts[10])) {
                                            continue;
                                        }
                                        dropId = TypesParser.safeParseInt(lineParts[10].split("_")[0]);
                                        ipId = TypesParser.safeParseInt(lineParts[10].split("_")[1]);
                                        clientId = TypesParser.safeParseInt(lineParts[10].split("_")[2]);
                                        listId = TypesParser.safeParseInt(lineParts[10].split("_")[3]);
                                        if (listId != list.id) {
                                            continue;
                                        }
                                        Database.get("lists").executeUpdate("DELETE FROM " + listName + " WHERE id = ?", new Object[] { clientId }, 0);
                                        System.out.println("Deleitng Email ID -> " + clientId + " from list --> " + listName);
                                        bounceLinesToRemove.add(line);
                                        ++bounceCleand;
                                        this.updateProccess(proccessId, "bounce");
                                    }
                                }
                            }
                        }
                    }
                    if (deliverdFiles != null && deliverdFiles.size() > 0) {
                        List<String> lines = null;
                        for (final String deliverdFile : deliverdFiles) {
                            final File deliver = new File(deliverdFile);
                            if (deliver.isFile()) {
                                lines = FileUtils.readLines(deliver);
                                if (lines == null || lines.isEmpty()) {
                                    continue;
                                }
                                for (final String line : lines) {
                                    if (!"".equals(line)) {
                                        lineParts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                                        if (lineParts.length != 12 || !"success".equalsIgnoreCase(lineParts[1]) || "".equalsIgnoreCase(lineParts[10])) {
                                            continue;
                                        }
                                        dropId = TypesParser.safeParseInt(lineParts[10].split("_")[0]);
                                        ipId = TypesParser.safeParseInt(lineParts[10].split("_")[1]);
                                        clientId = TypesParser.safeParseInt(lineParts[10].split("_")[2]);
                                        listId = TypesParser.safeParseInt(lineParts[10].split("_")[3]);
                                        if (listId != list.id) {
                                            continue;
                                        }
                                        deliverdLinesToRemove.add(line);
                                        System.out.println("Email clean -> " + clientId);
                                        ++dileverdCleand;
                                        this.updateProccess(proccessId, "clean");
                                    }
                                }
                            }
                        }
                    }
                    if (bounceCleand > 0 || bounceCleand > 0) {
                        final String newName = "clean_" + flag + "_" + tablePrefix;
                        Database.get("lists").executeUpdate("ALTER TABLE " + listName + " RENAME TO " + newName + ";", new Object[0], 0);
                        Database.get("master").executeUpdate("UPDATE admin.data_lists SET name = '" + schema + "." + newName + "' WHERE id = ?", new Object[] { list.id }, 0);
                        if (bounceLinesToRemove != null && bounceLinesToRemove.size() > 0) {
                            for (int j = 0; j < bounceLinesToRemove.size(); ++j) {
                                System.out.println("Removing this line of bounce  --> " + bounceLinesToRemove.get(j));
                            }
                        }
                        if (deliverdLinesToRemove != null && deliverdLinesToRemove.size() > 0) {
                            for (int j = 0; j < deliverdLinesToRemove.size(); ++j) {
                                System.out.println("Removing this line of delevred  --> " + deliverdLinesToRemove.get(j));
                            }
                        }
                    }
                }
                else {
                    this.interruptProccess(proccessId);
                    errorOccured = true;
                }
            }
            else {
                System.out.println("Looking for all servers ... not yet ready ...");
            }
        }
        catch (Exception e) {
            this.interruptProccess(proccessId);
            Logger.error(e, dataclean.class);
            errorOccured = true;
        }
        finally {
            if (!errorOccured) {
                this.finishProccess(proccessId);
            }
        }
    }
    
    public void updateProccess(final int proccessId, final String type) throws DatabaseException {
        final int progress = (int)(this.getIndex() / (double)this.getCount() * 100.0);
        final String update = "bounce".equalsIgnoreCase(type) ? " , hard_bounce = hard_bounce + 1 " : " , clean = clean + 1 ";
        Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET progress = '" + progress + "%' " + update + " WHERE Id = ?", new Object[] { proccessId }, 0);
        this.updateIndex();
    }
    
    public int getIndex() {
        return dataclean.INDEX;
    }
    
    public void updateIndex() {
        ++dataclean.INDEX;
    }
    
    public void updateCount(final String listName) throws DatabaseException {
        final List<LinkedHashMap<String, Object>> result = Database.get("lists").executeQuery("SELECT COUNT(id) AS count FROM " + listName + ";", null, 0);
        final int size = Integer.parseInt(String.valueOf(result.get(0).get("count")));
        dataclean.COUNT += size;
    }
    
    public int getCount() {
        return dataclean.COUNT;
    }
    
    public void interruptProccess(final int proccessId) {
        try {
            Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET status = 'error' , finish_time = ?  WHERE id = ?", new Object[] { new Timestamp(System.currentTimeMillis()), proccessId }, 0);
        }
        catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }
    
    public void finishProccess(final int proccessId) {
        try {
            Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET status = 'completed' , progress = '100%' , finish_time = ?  WHERE id = ?", new Object[] { new Timestamp(System.currentTimeMillis()), proccessId }, 0);
        }
        catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }
    
    public void removeLineFromFile() {
    }
    
    private static /* synthetic */ boolean lambda$start$0(final Path x$0) {
        return Files.isRegularFile(x$0, new LinkOption[0]);
    }
    
    static {
        dataclean.COUNT = 0;
        dataclean.INDEX = 1;
    }
}
