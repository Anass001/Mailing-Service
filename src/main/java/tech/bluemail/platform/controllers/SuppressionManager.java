/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.controllers;

import java.io.File;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.exceptions.ThreadException;
import tech.bluemail.platform.interfaces.Controller;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.models.admin.DataList;
import tech.bluemail.platform.orm.Database;
import tech.bluemail.platform.parsers.TypesParser;
import tech.bluemail.platform.security.License;
import tech.bluemail.platform.utils.Compressor;
import tech.bluemail.platform.utils.Request;
import tech.bluemail.platform.utils.Strings;
import tech.bluemail.platform.workers.SupressionWorker;

public class SuppressionManager
implements Controller {
    public static volatile Set<String> MD5_EMAILS = new HashSet<String>();
    public static volatile int INDEX = 1;

    public SuppressionManager() throws Exception {
        License.check();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void start(String[] parameters) throws Exception {
        int proccessId = TypesParser.safeParseInt(parameters[1]);
        String suppressionsFolder = new File(System.getProperty("base.path")).getAbsolutePath() + File.separator + "tmp" + File.separator + "suppressions";
        String tempDirectory = Strings.getSaltString(20, true, true, true, false);
        boolean errorOccured = false;
        try {
            int offerId = TypesParser.safeParseInt(parameters[2]);
            String link = new String(Base64.getDecoder().decode(parameters[3]));
            System.out.println("LINK --> " + link);
            if (proccessId == 0) {
                throw new Exception("No Proccess Id Found !");
            }
            if ("".equals(link)) {
                throw new Exception("No Link Found !");
            }
            System.out.println("Offer id -> " + offerId);
            new File(suppressionsFolder + File.separator + tempDirectory).mkdirs();
            String fileName = "suppression_" + Strings.getSaltString(10, true, true, true, false);
            System.out.println("Downlaod file " + fileName);
            String[] fileInfo = Request.downloadFile(link.trim(), suppressionsFolder + File.separator + tempDirectory + File.separator + fileName);
            System.out.println("DOWNLOADED ");
            String emailsFile = "";
            if (fileInfo.length == 0) {
                throw new Exception("Could not Download the File !");
            }
            Database.get("master").executeUpdate("UPDATE admin.suppression_proccesses SET status = 'in-progress' , progress = '0%' , emails_found = 0 WHERE Id = ?", new Object[]{proccessId}, 0);
            System.out.println("File info -> " + fileInfo[1]);
            if (fileInfo[1].toLowerCase().contains("application/zip") || fileInfo[1].toLowerCase().contains("application/x-zip-compressed")) {
                String zipDirectory = suppressionsFolder + File.separator + tempDirectory + File.separator + Strings.getSaltString(10, true, true, true, false) + File.separator;
                new File(zipDirectory).mkdir();
                Compressor.unzip(fileInfo[0], zipDirectory);
                File file = new File(zipDirectory);
                for (File child : file.listFiles()) {
                    if (child.getName().toLowerCase().contains("domain")) continue;
                    emailsFile = child.getAbsolutePath();
                }
            } else if (fileInfo[1].toLowerCase().contains("text/plain") || fileInfo[1].toLowerCase().contains("application/csv") || fileInfo[1].toLowerCase().contains("text/csv") || fileInfo[1].toLowerCase().contains("application/octet-stream") || fileInfo[1].toLowerCase().contains("application/octet-stream;charset=UTF-8")) {
                emailsFile = fileInfo[0];
            }
            if (!new File(emailsFile).exists()) {
                throw new Exception("Suppression File Not Found !");
            }
            List<String> md5lines = FileUtils.readLines(new File(emailsFile), "UTF-8");
            System.out.println("Checking line -> " + md5lines.get(0));
            boolean isMd5 = this.isValidMD5(md5lines.get(0));
            System.out.println("Is MD5 " + isMd5);
            Collections.sort(md5lines);
            MD5_EMAILS = Collections.unmodifiableSet(new HashSet<String>(md5lines));
            List dataLists = DataList.all(DataList.class);
            if (dataLists == null) throw new Exception("Data Lists Not Found !");
            if (dataLists.isEmpty()) {
                throw new Exception("Data Lists Not Found !");
            }
            int listsSize = dataLists.size();
            ExecutorService executor = Executors.newFixedThreadPool(30);
            SupressionWorker worker = null;
            for (DataList dataList : dataLists) {
                if (dataList.id <= 0 || dataList.name == null || "".equalsIgnoreCase(dataList.name) || dataList.name.contains("seeds")) continue;
                System.out.println("Cleannng List " + dataList.name);
                worker = new SupressionWorker(proccessId, offerId, dataList, isMd5, suppressionsFolder + File.separator + tempDirectory, listsSize);
                worker.setUncaughtExceptionHandler(new ThreadException());
                executor.submit(worker);
            }
            executor.shutdown();
            executor.awaitTermination(10L, TimeUnit.DAYS);
            return;
        }
        catch (Exception e) {
            this.interruptProccess(proccessId, suppressionsFolder + File.separator + tempDirectory);
            Logger.error(e, SuppressionManager.class);
            errorOccured = true;
            return;
        }
        finally {
            if (!errorOccured) {
                this.finishProccess(proccessId, suppressionsFolder + File.separator + tempDirectory);
            }
        }
    }

    public void interruptProccess(int proccessId, String directory) {
        try {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            long time = cal.getTimeInMillis();
            Database.get("master").executeUpdate("UPDATE admin.suppression_proccesses SET status = 'error' , finish_time = ?  WHERE id = ?", new Object[]{new Timestamp(time), proccessId}, 0);
            FileUtils.deleteDirectory(new File(directory));
            return;
        }
        catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }

    public void finishProccess(int proccessId, String directory) {
        try {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            long time = cal.getTimeInMillis();
            Database.get("master").executeUpdate("UPDATE admin.suppression_proccesses SET status = 'completed' ,  progress = '100%' , finish_time = ?  WHERE id = ?", new Object[]{new Timestamp(time), proccessId}, 0);
            FileUtils.deleteDirectory(new File(directory));
            return;
        }
        catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }

    public static synchronized void updateProccess(int proccessId, int size, int emailsFound) throws DatabaseException {
        int progress = (int)((double)SuppressionManager.getIndex() / (double)size * 100.0);
        Database.get("master").executeUpdate("UPDATE admin.suppression_proccesses SET progress = '" + progress + "%' ,emails_found = emails_found + " + emailsFound + " WHERE Id = ?", new Object[]{proccessId}, 0);
        SuppressionManager.updateIndex();
    }

    public static synchronized int getIndex() {
        return INDEX;
    }

    public static synchronized void updateIndex() {
        ++INDEX;
    }

    public boolean isValidMD5(String s) {
        return s.matches("^[a-fA-F0-9]{32}$");
    }
}

