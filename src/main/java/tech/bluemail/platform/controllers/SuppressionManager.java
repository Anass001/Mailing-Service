package tech.bluemail.platform.controllers;

import tech.bluemail.platform.interfaces.*;
import tech.bluemail.platform.security.*;
import tech.bluemail.platform.parsers.*;
import java.io.*;
import tech.bluemail.platform.utils.*;
import org.apache.commons.io.*;
import tech.bluemail.platform.models.admin.*;
import tech.bluemail.platform.orm.*;
import tech.bluemail.platform.workers.*;
import tech.bluemail.platform.logging.*;
import java.util.concurrent.*;
import java.util.*;
import java.sql.*;
import tech.bluemail.platform.exceptions.*;

public class SuppressionManager implements Controller
{
    public static volatile Set<String> MD5_EMAILS;
    public static volatile int INDEX;
    
    public SuppressionManager() throws Exception {
        super();
        License.check();
    }
    
    @Override
    public void start(final String[] parameters) throws Exception {
        final int proccessId = TypesParser.safeParseInt(parameters[1]);
        final String suppressionsFolder = new File(System.getProperty("base.path")).getAbsolutePath() + File.separator + "tmp" + File.separator + "suppressions";
        final String tempDirectory = Strings.getSaltString(20, true, true, true, false);
        boolean errorOccured = false;
        try {
            final int offerId = TypesParser.safeParseInt(parameters[2]);
            final String link = new String(Base64.getDecoder().decode(parameters[3]));
            System.out.println("LINK --> " + link);
            if (proccessId == 0) {
                throw new Exception("No Proccess Id Found !");
            }
            if ("".equals(link)) {
                throw new Exception("No Link Found !");
            }
            System.out.println("Offer id -> " + offerId);
            new File(suppressionsFolder + File.separator + tempDirectory).mkdirs();
            final String fileName = "suppression_" + Strings.getSaltString(10, true, true, true, false);
            System.out.println("Downlaod file " + fileName);
            final String[] fileInfo = Request.downloadFile(link.trim(), suppressionsFolder + File.separator + tempDirectory + File.separator + fileName);
            System.out.println("DOWNLOADED ");
            String emailsFile = "";
            if (fileInfo.length == 0) {
                throw new Exception("Could not Download the File !");
            }
            Database.get("master").executeUpdate("UPDATE admin.suppression_proccesses SET status = 'in-progress' , progress = '0%' , emails_found = 0 WHERE Id = ?", new Object[] { proccessId }, 0);
            System.out.println("File info -> " + fileInfo[1]);
            if (fileInfo[1].toLowerCase().contains("application/zip") || fileInfo[1].toLowerCase().contains("application/x-zip-compressed")) {
                final String zipDirectory = suppressionsFolder + File.separator + tempDirectory + File.separator + Strings.getSaltString(10, true, true, true, false) + File.separator;
                new File(zipDirectory).mkdir();
                Compressor.unzip(fileInfo[0], zipDirectory);
                final File file = new File(zipDirectory);
                for (final File child : file.listFiles()) {
                    if (!child.getName().toLowerCase().contains("domain")) {
                        emailsFile = child.getAbsolutePath();
                    }
                }
            }
            else if (fileInfo[1].toLowerCase().contains("text/plain") || fileInfo[1].toLowerCase().contains("application/csv") || fileInfo[1].toLowerCase().contains("text/csv") || fileInfo[1].toLowerCase().contains("application/octet-stream") || fileInfo[1].toLowerCase().contains("application/octet-stream;charset=UTF-8")) {
                emailsFile = fileInfo[0];
            }
            if (!new File(emailsFile).exists()) {
                throw new Exception("Suppression File Not Found !");
            }
            final List<String> md5lines = FileUtils.readLines(new File(emailsFile), "UTF-8");
            System.out.println("Checking line -> " + md5lines.get(0));
            final boolean isMd5 = this.isValidMD5(md5lines.get(0));
            System.out.println("Is MD5 " + isMd5);
            Collections.sort(md5lines);
            SuppressionManager.MD5_EMAILS = Collections.unmodifiableSet((Set<? extends String>)new HashSet<String>(md5lines));
            final List<DataList> dataLists = (List<DataList>)ActiveRecord.all(DataList.class);
            if (dataLists == null || dataLists.isEmpty()) {
                throw new Exception("Data Lists Not Found !");
            }
            final int listsSize = dataLists.size();
            final ExecutorService executor = Executors.newFixedThreadPool(30);
            SupressionWorker worker = null;
            for (final DataList dataList : dataLists) {
                if (dataList.id > 0 && dataList.name != null && !"".equalsIgnoreCase(dataList.name) && !dataList.name.contains("seeds")) {
                    System.out.println("Cleannng List " + dataList.name);
                    worker = new SupressionWorker(proccessId, offerId, dataList, isMd5, suppressionsFolder + File.separator + tempDirectory, listsSize);
                    worker.setUncaughtExceptionHandler(new ThreadException());
                    executor.submit(worker);
                }
            }
            executor.shutdown();
            executor.awaitTermination(10L, TimeUnit.DAYS);
        }
        catch (Exception e) {
            this.interruptProccess(proccessId, suppressionsFolder + File.separator + tempDirectory);
            Logger.error(e, SuppressionManager.class);
            errorOccured = true;
        }
        finally {
            if (!errorOccured) {
                this.finishProccess(proccessId, suppressionsFolder + File.separator + tempDirectory);
            }
        }
    }
    
    public void interruptProccess(final int proccessId, final String directory) {
        try {
            final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            final long time = cal.getTimeInMillis();
            Database.get("master").executeUpdate("UPDATE admin.suppression_proccesses SET status = 'error' , finish_time = ?  WHERE id = ?", new Object[] { new Timestamp(time), proccessId }, 0);
            FileUtils.deleteDirectory(new File(directory));
        }
        catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }
    
    public void finishProccess(final int proccessId, final String directory) {
        try {
            final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            final long time = cal.getTimeInMillis();
            Database.get("master").executeUpdate("UPDATE admin.suppression_proccesses SET status = 'completed' ,  progress = '100%' , finish_time = ?  WHERE id = ?", new Object[] { new Timestamp(time), proccessId }, 0);
            FileUtils.deleteDirectory(new File(directory));
        }
        catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }
    
    public static synchronized void updateProccess(final int proccessId, final int size, final int emailsFound) throws DatabaseException {
        final int progress = (int)(getIndex() / (double)size * 100.0);
        Database.get("master").executeUpdate("UPDATE admin.suppression_proccesses SET progress = '" + progress + "%' ,emails_found = emails_found + " + emailsFound + " WHERE Id = ?", new Object[] { proccessId }, 0);
        updateIndex();
    }
    
    public static synchronized int getIndex() {
        return SuppressionManager.INDEX;
    }
    
    public static synchronized void updateIndex() {
        ++SuppressionManager.INDEX;
    }
    
    public boolean isValidMD5(final String s) {
        return s.matches("^[a-fA-F0-9]{32}$");
    }
    
    static {
        SuppressionManager.MD5_EMAILS = new HashSet<String>();
        SuppressionManager.INDEX = 1;
    }
}
