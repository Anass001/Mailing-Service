/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.workers;

import java.io.File;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import tech.bluemail.platform.controllers.SuppressionManager;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.models.admin.DataList;
import tech.bluemail.platform.orm.Database;

public class SupressionWorker
extends Thread {
    public int proccessId;
    public int offerId;
    public DataList dataList;
    public boolean isMd5;
    public String directory;
    public int listsSize;

    public SupressionWorker(int proccessId, int offerId, DataList dataList, boolean isMd5, String directory, int listsSize) {
        this.proccessId = proccessId;
        this.offerId = offerId;
        this.dataList = dataList;
        this.isMd5 = isMd5;
        this.directory = directory;
        this.listsSize = listsSize;
    }

    @Override
    public void run() {
        try {
            if (this.dataList == null) return;
            if (this.proccessId <= 0) return;
            if (this.offerId <= 0) return;
            ArrayList<String> suppressionEmails = new ArrayList<String>();
            String[] columns = null;
            String schema = this.dataList.name.split("\\.")[0];
            String table = this.dataList.name.split("\\.")[1];
            System.out.println("Table -> " + table);
            columns = table.startsWith("fresh_") || table.startsWith("clean_") ? new String[]{"id", "email", "fname", "lname", "offers_excluded"} : (table.startsWith("unsubscribers_") ? new String[]{"id", "email", "fname", "lname", "drop_id", "action_date", "message", "offers_excluded", "verticals", "agent", "ip", "country", "region", "city", "language", "device_type", "device_name", "os", "browser_name", "browser_version"} : new String[]{"id", "email", "fname", "lname", "action_date", "offers_excluded", "verticals", "agent", "ip", "country", "region", "city", "language", "device_type", "device_name", "os", "browser_name", "browser_version"});
            List<LinkedHashMap<String, Object>> totalEmails = this.getsuppressionEmails(suppressionEmails, columns);
            if (suppressionEmails.isEmpty()) return;
            if (totalEmails.isEmpty()) return;
            if (columns == null) return;
            Collections.sort(suppressionEmails);
            suppressionEmails.retainAll(SuppressionManager.MD5_EMAILS);
            HashSet<String> hashset = new HashSet<String>();
            hashset.addAll(suppressionEmails);
            suppressionEmails.clear();
            suppressionEmails.addAll(hashset);
            String csv = this.convertEmailsToCsv(totalEmails, suppressionEmails, columns);
            if ("".equalsIgnoreCase(csv)) return;
            FileUtils.writeStringToFile(new File(this.directory + File.separator + this.dataList.name + ".csv"), csv);
            String exists = "false";
            List<LinkedHashMap<String, Object>> res = Database.get("lists").executeQuery("SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace WHERE  n.nspname = '" + schema + "' AND c.relname = '" + table + "_suppression_copy' AND c.relkind = 'r');", null, 1);
            if (res != null && !res.isEmpty()) {
                exists = String.valueOf(res.get(0).get("exists"));
            }
            if ("true".equals(exists)) {
                Database.get("lists").executeUpdate("DROP TABLE " + this.dataList.name + "_suppression_copy", null, 0);
            }
            Runtime.getRuntime().exec(new String[]{"bash", "-c", "chmod a+rw " + this.directory + File.separator + this.dataList.name + ".csv"});
            Database.get("lists").executeUpdate("CREATE TABLE " + this.dataList.name + "_suppression_copy ( like " + this.dataList.name + " including defaults including constraints including indexes )", null, 0);
            Database.get("lists").executeUpdate("COPY " + this.dataList.name + "_suppression_copy FROM '" + this.directory + File.separator + this.dataList.name + ".csv' WITH CSV HEADER DELIMITER AS ',' NULL AS '';", null, 0);
            List<LinkedHashMap<String, Object>> result = Database.get("lists").executeQuery("SELECT (SELECT COUNT(id) AS count1 FROM " + this.dataList.name + ") - (SELECT COUNT(id) AS count2 FROM " + this.dataList.name + "_suppression_copy) AS difference", null, 0);
            boolean identical = false;
            if (result != null && !result.isEmpty() && result.get(0).containsKey("difference")) {
                identical = "0".equalsIgnoreCase(String.valueOf(result.get(0).get("difference")));
            }
            if (identical) {
                Database.get("lists").executeUpdate("DROP TABLE " + this.dataList.name, null, 0);
                Database.get("lists").executeUpdate("ALTER TABLE " + this.dataList.name + "_suppression_copy RENAME TO " + this.dataList.name.split("\\.")[1], null, 0);
            }
            SuppressionManager.updateProccess(this.proccessId, this.listsSize, suppressionEmails.size());
            return;
        }
        catch (Exception e) {
            Logger.error(e, SupressionWorker.class);
        }
    }

    public List<LinkedHashMap<String, Object>> getsuppressionEmails(List<String> suppressionEmails, String[] columns) {
        List<LinkedHashMap<String, Object>> emails = null;
        try {
            emails = this.isMd5 ? Database.get("lists").executeQuery("SELECT " + String.join((CharSequence)",", columns) + ",md5(email) as md5_email FROM " + this.dataList.name, null, 1) : Database.get("lists").executeQuery("SELECT " + String.join((CharSequence)",", columns) + ",email as md5_email FROM " + this.dataList.name, null, 1);
            Iterator<LinkedHashMap<String, Object>> iterator = emails.iterator();
            while (iterator.hasNext()) {
                LinkedHashMap<String, Object> row = iterator.next();
                if (row == null) continue;
                suppressionEmails.add(String.valueOf(row.get("md5_email")).trim());
            }
            return emails;
        }
        catch (Exception e) {
            Logger.error(e, SupressionWorker.class);
        }
        return emails;
    }

    public String convertEmailsToCsv(List<LinkedHashMap<String, Object>> totalEmails, List<String> suppressionEmails, String[] columns) throws SQLException {
        StringBuilder csv = new StringBuilder();
        boolean insertOfferId = false;
        ArrayList<String> offerIds = null;
        for (int i = 0; i < columns.length; ++i) {
            csv.append("\"").append(columns[i]);
            if (i < columns.length - 1) {
                csv.append("\"").append(",");
                continue;
            }
            csv.append("\"");
        }
        csv.append("\n");
        Iterator<LinkedHashMap<String, Object>> i = totalEmails.iterator();
        while (i.hasNext()) {
            LinkedHashMap<String, Object> row = i.next();
            insertOfferId = false;
            if (suppressionEmails.contains(String.valueOf(row.get("md5_email")).trim())) {
                insertOfferId = true;
            }
            for (int i2 = 0; i2 < columns.length; ++i2) {
                csv.append("\"");
                if ("offers_excluded".equalsIgnoreCase(columns[i2])) {
                    if (row.get(columns[i2]) == null || "null".equalsIgnoreCase(String.valueOf(row.get(columns[i2]))) || "".equalsIgnoreCase(String.valueOf(row.get(columns[i2])))) {
                        if (insertOfferId) {
                            csv.append(this.offerId);
                        } else {
                            csv.append("");
                        }
                    } else {
                        offerIds = insertOfferId ? new ArrayList<String>(new HashSet<String>(Arrays.asList((this.offerId + "," + String.valueOf(row.get(columns[i2]))).split(",")))) : new ArrayList<String>(new HashSet<String>(Arrays.asList(String.valueOf(row.get(columns[i2])).split(","))));
                        for (int j = 0; j < offerIds.size(); ++j) {
                            if ("".equalsIgnoreCase(((String)offerIds.get(j)).trim())) continue;
                            csv.append((String)offerIds.get(j));
                            if (j >= offerIds.size() - 1) continue;
                            csv.append(",");
                        }
                    }
                } else if (row.get(columns[i2]) == null || "null".equalsIgnoreCase(String.valueOf(row.get(columns[i2])))) {
                    csv.append("");
                } else {
                    csv.append(String.valueOf(row.get(columns[i2])).replaceAll("\"", "\\\""));
                }
                if (i2 < columns.length - 1) {
                    csv.append("\"").append(",");
                    continue;
                }
                csv.append("\"");
            }
            csv.append("\n");
        }
        return csv.toString();
    }
}

