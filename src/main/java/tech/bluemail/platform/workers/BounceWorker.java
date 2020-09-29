/*
 * Decompiled with CFR <Could not determine version>.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.ArrayUtils
 */
package tech.bluemail.platform.workers;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import tech.bluemail.platform.controllers.BounceCleaner;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.models.admin.DataList;
import tech.bluemail.platform.models.admin.Server;
import tech.bluemail.platform.models.lists.Clean;
import tech.bluemail.platform.models.lists.HardBounce;
import tech.bluemail.platform.orm.Database;
import tech.bluemail.platform.orm.Query;
import tech.bluemail.platform.parsers.TypesParser;

public class BounceWorker
extends Thread {
    public int proccessId;
    public String listName;
    public Server server;
    public int mailerId;
    public int ispId;

    public BounceWorker(int proccessId, String listName, Server server, int mailerId, int ispId) {
        this.proccessId = proccessId;
        this.listName = listName;
        this.server = server;
        this.mailerId = mailerId;
        this.ispId = ispId;
    }

    @Override
    public void run() {
        try {
            int count;
            String logsFolder = new File(System.getProperty("base.path")).getAbsolutePath() + File.separator + "tmp" + File.separator + "pmta-logs" + File.separator + "server_" + this.server.id;
            if (!new File(logsFolder).exists()) throw new Exception("Pmta Logs Folder Does Not Exists For Server : " + this.server.name);
            String schema = this.listName.split("\\.")[0];
            String table = this.listName.split("\\.")[1];
            String type = table.split("_")[0];
            String flag = table.split("_")[1];
            String tablePrefix = table.replaceAll(flag + "_", "").replaceAll(type + "_", "");
            ArrayList<HardBounce> bounceEmails = new ArrayList<HardBounce>();
            ArrayList<Clean> cleanEmails = new ArrayList<Clean>();
            DataList list = (DataList)DataList.first(DataList.class, "name = ?", new Object[]{this.listName});
            if (list == null) throw new Exception("Data List : " + this.listName + " Does not Exists !");
            if (list.id == 0) {
                throw new Exception("Data List : " + this.listName + " Does not Exists !");
            }
            File[] logsDirs = new File(logsFolder).listFiles();
            Object[] bounceFiles = new File[]{};
            File[] deliveredFiles = new File[]{};
            if (logsDirs != null && logsDirs.length > 0) {
                for (File logsDir : logsDirs) {
                    if (!logsDir.isDirectory()) continue;
                    bounceFiles = (File[])ArrayUtils.addAll((Object[])bounceFiles, (Object[])new File(logsDir.getAbsolutePath() + File.separator + "bounces").listFiles());
                    deliveredFiles = (File[])ArrayUtils.addAll((Object[])bounceFiles, (Object[])new File(logsDir.getAbsolutePath() + File.separator + "delivered").listFiles());
                }
            }
            List<String> lines = null;
            String[] lineParts = new String[]{};
            int clientId = 0;
            int listId = 0;
            ArrayList<String> checkBounce = new ArrayList<String>();
            if (bounceFiles != null && bounceFiles.length > 0) {
                for (File bounceFile : bounceFiles) {
                    if (bounceFile.isDirectory() || (lines = FileUtils.readLines(bounceFile)) == null || lines.isEmpty()) continue;
                    for (String line : lines) {
                        if ("".equals(line) || (lineParts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1)).length != 12 || !"hardbnc".equalsIgnoreCase(lineParts[1]) || "".equalsIgnoreCase(lineParts[10])) continue;
                        clientId = TypesParser.safeParseInt(lineParts[10].split("_")[2]);
                        listId = TypesParser.safeParseInt(lineParts[10].split("_")[3]);
                        List<LinkedHashMap<String, Object>> result = Database.get("lists").executeQuery("SELECT * FROM " + this.listName + " WHERE id =" + clientId, null, 0);
                        if (result.isEmpty()) continue;
                        lineParts[5] = (String)((LinkedHashMap)result.get(0)).get("email");
                        if (listId != list.id || checkBounce.contains(lineParts[5])) continue;
                        HardBounce bounceEmail = new HardBounce();
                        bounceEmail.id = clientId;
                        bounceEmail.setSchema(schema);
                        bounceEmail.setTable(table);
                        bounceEmail.load();
                        bounceEmail.setTable("hard_bounce");
                        bounceEmails.add(bounceEmail);
                        checkBounce.add(lineParts[5]);
                    }
                }
            }
            lines = null;
            lineParts = new String[]{};
            clientId = 0;
            listId = 0;
            ArrayList<String> checkClean = new ArrayList<String>();
            String cleanTable = "clean_" + flag + "_" + tablePrefix;
            if (deliveredFiles != null && deliveredFiles.length > 0) {
                for (File deliveredFile : deliveredFiles) {
                    if (deliveredFile.isDirectory() || (lines = FileUtils.readLines(deliveredFile)) == null || lines.isEmpty()) continue;
                    for (String line : lines) {
                        if ("".equals(line) || (lineParts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1)).length != 12 || !"success".equalsIgnoreCase(lineParts[1]) || "".equalsIgnoreCase(lineParts[10])) continue;
                        clientId = TypesParser.safeParseInt(lineParts[10].split("_")[2]);
                        listId = TypesParser.safeParseInt(lineParts[10].split("_")[3]);
                        List<LinkedHashMap<String, Object>> result = Database.get("lists").executeQuery("SELECT * FROM " + this.listName + " WHERE id =" + clientId, null, 0);
                        if (result.isEmpty()) continue;
                        lineParts[5] = (String)result.get(0).get("email");
                        if (listId != list.id || checkClean.contains(lineParts[5])) continue;
                        Clean cleanEmail = new Clean();
                        cleanEmail.id = clientId;
                        cleanEmail.setSchema(schema);
                        cleanEmail.setTable(table);
                        cleanEmail.load();
                        cleanEmail.setTable(cleanTable);
                        cleanEmails.add(cleanEmail);
                        checkClean.add(lineParts[5]);
                    }
                }
            }
            BounceCleaner.updateCount(bounceEmails.size() + cleanEmails.size());
            if (!bounceEmails.isEmpty()) {
                HardBounce email = null;
                if (!((HardBounce)bounceEmails.get(0)).tableExists()) {
                    ((HardBounce)bounceEmails.get(0)).sync();
                }
                for (HardBounce bounce : bounceEmails) {
                    email = new HardBounce();
                    email.setSchema(bounce.getSchema());
                    email.setTable(bounce.getTable());
                    email.email = bounce.email;
                    Database.get("lists").executeUpdate("DELETE FROM " + this.listName + " WHERE id = ?", new Object[]{bounce.id}, 0);
                    email.insert();
                    BounceCleaner.updateProccess(this.proccessId, "bounce");
                }
            }
            if (!cleanEmails.isEmpty()) {
                Clean email = null;
                if (!((Clean)cleanEmails.get(0)).tableExists()) {
                    ((Clean)cleanEmails.get(0)).sync();
                    DataList listObject = new DataList();
                    listObject.name = ((Clean)cleanEmails.get(0)).getSchema() + "." + ((Clean)cleanEmails.get(0)).getTable();
                    listObject.ispId = list.ispId;
                    listObject.authorizedUsers = list.authorizedUsers;
                    listObject.flag = list.flag;
                    listObject.statusId = list.statusId;
                    listObject.createdAt = list.createdAt;
                    listObject.createdBy = list.createdBy;
                    listObject.lastUpdatedAt = list.lastUpdatedAt;
                    listObject.lastUpdatedBy = list.lastUpdatedBy;
                    listObject.insert();
                }
                for (Clean clean : cleanEmails) {
                    email = new Clean();
                    email.setSchema(clean.getSchema());
                    email.setTable(clean.getTable());
                    email.email = clean.email;
                    email.fname = clean.email.split("\\@")[0];
                    email.lname = clean.fname;
                    email.offersExcluded = clean.offersExcluded == null ? "" : clean.offersExcluded;
                    Database.get("lists").executeUpdate("DELETE FROM " + this.listName + " WHERE id = ?", new Object[]{clean.id}, 0);
                    email.insert();
                    BounceCleaner.updateProccess(this.proccessId, "clean");
                }
            }
            if ((count = Database.get("lists").query().from(this.listName, new String[]{"id"}).count()) != 0) return;
            Database.get("lists").executeUpdate("DROP TABLE " + this.listName, new Object[0], 0);
            Database.get("lists").executeUpdate("DROP SEQUENCE " + schema + ".seq_id_" + table, new Object[0], 0);
            list.delete();
            return;
        }
        catch (Exception e) {
            Logger.error(e, BounceWorker.class);
        }
    }
}

