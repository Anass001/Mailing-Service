/*
 * Decompiled with CFR <Could not determine version>.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.ArrayUtils
 *  org.apache.commons.lang.StringUtils
 *  org.codehaus.jackson.map.ObjectMapper
 *  org.jsoup.Jsoup
 *  org.jsoup.nodes.Document
 *  org.jsoup.nodes.Element
 *  org.jsoup.select.Elements
 */
package tech.bluemail.platform.helpers;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tech.bluemail.platform.components.DropComponent;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.models.admin.Isp;
import tech.bluemail.platform.models.admin.Negative;
import tech.bluemail.platform.models.admin.Offer;
import tech.bluemail.platform.models.admin.OfferName;
import tech.bluemail.platform.models.admin.OfferSubject;
import tech.bluemail.platform.models.admin.Server;
import tech.bluemail.platform.models.admin.Sponsor;
import tech.bluemail.platform.models.admin.Vmta;
import tech.bluemail.platform.models.production.Drop;
import tech.bluemail.platform.models.production.DropIp;
import tech.bluemail.platform.parsers.TypesParser;
import tech.bluemail.platform.remote.SSH;
import tech.bluemail.platform.utils.Mapper;
import tech.bluemail.platform.utils.Strings;

public class DropsHelper {
    public static void saveDrop(DropComponent dropComponent, Server server) throws Exception {
        Drop drop = new Drop();
        drop.id = 0;
        drop.pids = dropComponent.pickupsFolder + File.separator + "drop_status_" + server.id;
        drop.userId = dropComponent.mailerId;
        drop.serverId = server.id;
        drop.ispId = dropComponent.ispId;
        drop.status = "in-progress";
        drop.startTime = new Timestamp(System.currentTimeMillis());
        drop.finishTime = null;
        drop.totalEmails = dropComponent.emailsCount;
        drop.sentProgress = 0;
        drop.offerId = dropComponent.offerId;
        drop.offerFromNameId = dropComponent.fromNameId;
        drop.offerSubjectId = dropComponent.subjectId;
        drop.recipientsEmails = String.join((CharSequence)",", dropComponent.testEmails);
        drop.header = new String(Base64.encodeBase64(String.join((CharSequence)"\n\n", dropComponent.headers).getBytes()));
        drop.creativeId = dropComponent.creativeId;
        Object[] lists = new String[]{};
        Iterator<Map.Entry<Integer, String>> iterator = dropComponent.lists.entrySet().iterator();
        do {
            if (!iterator.hasNext()) {
                drop.lists = String.join((CharSequence)"|", (CharSequence[])lists);
                drop.postData = new String(Base64.encodeBase64(dropComponent.content.getBytes()));
                dropComponent.id = drop.insert();
                if (dropComponent.id != 0) return;
                throw new Exception("Error While Saving Drop !");
            }
            Map.Entry<Integer, String> en = iterator.next();
            lists = (String[])ArrayUtils.add((Object[])lists, (Object)String.valueOf(en.getValue()));
        } while (true);
    }

    public static void saveDropVmta(DropComponent dropComponent, Vmta vmta, int totalSent) throws Exception {
        DropIp dropIp = new DropIp();
        dropIp.id = 0;
        dropIp.serverId = vmta.serverId;
        dropIp.ispId = dropComponent.ispId;
        dropIp.dropId = dropComponent.id;
        dropIp.ipId = vmta.ipId;
        dropIp.dropDate = new Timestamp(System.currentTimeMillis());
        dropIp.totalSent = totalSent;
        dropIp.delivered = 0;
        dropIp.bounced = 0;
        if (dropIp.insert() != 0) return;
        throw new Exception("Error While Saving Drop Ip !");
    }

    public static DropComponent parseDropFile(String content) throws Exception {
        DropComponent drop = null;
        if ("".equalsIgnoreCase(content)) return drop;
        try {
            String[] listsParts;
            TreeMap data = (TreeMap)new ObjectMapper().readValue(content, TreeMap.class);
            if (data == null) return drop;
            if (data.isEmpty()) return drop;
            drop = new DropComponent();
            drop.id = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "drop-id", (Object)"0")));
            drop.isNewDrop = drop.id == 0;
            drop.isSend = "true".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "drop", (Object)"false")));
            drop.content = content;
            drop.mailerId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "user-id", (Object)"0")));
            drop.negativeId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "negative-id", (Object)"0")));
            if (!"".equals(drop.negativeId) && drop.negativeId > 0) {
                drop.negativeObject = new Negative(drop.negativeId);
                if (drop.negativeObject != null && drop.negativeObject.value != null) {
                    drop.negativeFileName = drop.negativeObject.value;
                }
            }
            drop.randomTags = DropsHelper.getAllRandomTags(drop.content);
            drop.serversIds = (String[])Arrays.copyOf(((List)Mapper.getMapValue(data, "servers", new ArrayList())).toArray(), ((List)Mapper.getMapValue(data, "servers", new ArrayList())).toArray().length, String[].class);
            if (drop.serversIds != null && drop.serversIds.length > 0) {
                drop.servers = Server.all(Server.class, "id IN (" + String.join((CharSequence)",", drop.serversIds) + ")", null);
            }
            if (drop.servers == null) throw new Exception("No Servers Found !");
            if (drop.servers.isEmpty()) {
                throw new Exception("No Servers Found !");
            }
            drop.vmtasRotation = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "vmtas-rotation", (Object)"1")));
            drop.vmtasIds = (String[])Arrays.copyOf(((List)Mapper.getMapValue(data, "selected-vmtas", new ArrayList())).toArray(), ((List)Mapper.getMapValue(data, "selected-vmtas", new ArrayList())).toArray().length, String[].class);
            if (drop.vmtasIds != null && drop.vmtasIds.length > 0) {
                String condition = "id IN (";
                for (String vmtasId : drop.vmtasIds) {
                    if (vmtasId == null || !vmtasId.contains("|")) continue;
                    condition = condition + vmtasId.split("\\|")[1] + ",";
                }
                condition = condition.substring(0, condition.length() - 1) + ")";
                drop.vmtas = Vmta.all(Vmta.class, condition, null);
            }
            if (drop.vmtas == null) throw new Exception("No Vmtas Found !");
            if (drop.vmtas.isEmpty()) {
                throw new Exception("No Vmtas Found !");
            }
            drop.vmtasEmailsProcces = String.valueOf(Mapper.getMapValue(data, "vmtas-emails-proccess", (Object)"vmtas-rotation"));
            drop.batch = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "batch", (Object)"1")));
            drop.delay = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "x-delay", (Object)"1")));
            drop.numberOfEmails = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "number-of-emails", (Object)"0")));
            drop.emailsPeriodValue = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "emails-period-value", (Object)"0")));
            drop.emailsPeriodType = String.valueOf(Mapper.getMapValue(data, "emails-period-type", (Object)"seconds"));
            if ("emails-per-period".equalsIgnoreCase(drop.vmtasEmailsProcces)) {
                if (drop.numberOfEmails == 0) {
                    throw new Exception("Number of Emails for Period is 0 !");
                }
                drop.batch = 1;
                switch (drop.emailsPeriodType) {
                    case "seconds": {
                        drop.emailsPeriodValue *= 1000;
                        break;
                    }
                    case "minutes": {
                        drop.emailsPeriodValue = drop.emailsPeriodValue * 60 * 1000;
                        break;
                    }
                    case "hours": {
                        drop.emailsPeriodValue = drop.emailsPeriodValue * 60 * 60 * 1000;
                    }
                }
                drop.delay = (int)Math.ceil(drop.emailsPeriodValue / drop.numberOfEmails);
            }
            drop.sponsorId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "sponsor", (Object)"0")));
            drop.sponsor = new Sponsor(drop.sponsorId);
            drop.offerId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "offer", (Object)"0")));
            drop.offer = new Offer(drop.offerId);
            drop.creativeId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "creative", (Object)"0")));
            drop.fromNameId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "from-name-id", (Object)"0")));
            drop.fromName = String.valueOf(Mapper.getMapValue(data, "from-name-text", (Object)""));
            if ("".equals(drop.fromName) && drop.fromNameId > 0) {
                drop.fromNameObject = new OfferName(drop.fromNameId);
                if (drop.fromNameObject != null && drop.fromNameObject.value != null) {
                    drop.fromName = drop.fromNameObject.value;
                }
            }
            drop.subjectId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "subject-id", (Object)"0")));
            drop.subject = String.valueOf(Mapper.getMapValue(data, "subject-text", (Object)""));
            if ("".equals(drop.subject) && drop.subjectId > 0) {
                drop.subjectObject = new OfferSubject(drop.subjectId);
                if (drop.subjectObject != null && drop.subjectObject.value != null) {
                    drop.subject = drop.subjectObject.value;
                }
            }
            drop.headersRotation = 1;
            drop.headers = (String[])Arrays.copyOf(((List)Mapper.getMapValue(data, "headers", new ArrayList())).toArray(), ((List)Mapper.getMapValue(data, "headers", new ArrayList())).toArray().length, String[].class);
            drop.bounceEmail = String.valueOf(Mapper.getMapValue(data, "bounce-email", (Object)""));
            drop.returnPath = String.valueOf(Mapper.getMapValue(data, "return-path", (Object)""));
            if (!drop.bounceEmail.contains("@") && !drop.returnPath.contains("@")) {
                drop.bounceEmail = drop.returnPath = !"".equals(drop.bounceEmail) && !"".equals(drop.returnPath) ? drop.bounceEmail + "@" + drop.returnPath : "";
            }
            drop.fromEmail = String.valueOf(Mapper.getMapValue(data, "from-email", (Object)"from@[domain]"));
            drop.replyTo = String.valueOf(Mapper.getMapValue(data, "reply-to", (Object)"reply@[domain]"));
            drop.received = String.valueOf(Mapper.getMapValue(data, "received", (Object)""));
            drop.to = String.valueOf(Mapper.getMapValue(data, "to", (Object)"[email]"));
            drop.placeholdersRotation = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "placeholders-rotation", (Object)"1")));
            drop.placeholders = !"".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "body-placeholders", (Object)""))) ? String.valueOf(Mapper.getMapValue(data, "body-placeholders", (Object)"")).split("\r\n") : new String[0];
            drop.hasPlaceholders = drop.placeholders.length > 0;
            drop.uploadImages = "on".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "upload-images", (Object)"off")));
            drop.charset = String.valueOf(Mapper.getMapValue(data, "charset", (Object)"utf-8"));
            drop.contentTransferEncoding = String.valueOf(Mapper.getMapValue(data, "content-transfer-encoding", (Object)"7bit"));
            drop.contentType = String.valueOf(Mapper.getMapValue(data, "content-type", (Object)"text/html"));
            drop.body = String.valueOf(Mapper.getMapValue(data, "body", (Object)""));
            drop.trackOpens = drop.isSend ? "on".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "track-opens", (Object)"off"))) : false;
            drop.ispId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "isp-id", (Object)"0")));
            drop.isp = new Isp(drop.ispId);
            drop.staticDomain = String.valueOf(Mapper.getMapValue(data, "static-domain", (Object)""));
            drop.emailsSplitType = String.valueOf(Mapper.getMapValue(data, "emails-split-type", (Object)"vmtas"));
            drop.testFrequency = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "send-test-after", (Object)"-1")));
            drop.testEmails = String.valueOf(Mapper.getMapValue(data, "recipients-emails", (Object)"")).split("\\;");
            drop.rcptfrom = String.valueOf(Mapper.getMapValue(data, "rcpt-from", (Object)""));
            drop.dataStart = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "data-start", (Object)"0")));
            drop.dataCount = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "data-count", (Object)"0")));
            drop.emailsPerSeeds = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "emails-per-seed", (Object)"1")));
            drop.emailsCount = 0;
            if (drop.isSend && !"".equals(Mapper.getMapValue(data, "lists", (Object)"")) && (listsParts = String.valueOf(Mapper.getMapValue(data, "lists", (Object)"")).split("\\,")) != null && listsParts.length > 0) {
                drop.lists = new HashMap();
                for (String tmp : listsParts) {
                    drop.lists.put(TypesParser.safeParseInt(String.valueOf(tmp.split("\\|")[0])), String.valueOf(tmp.split("\\|")[1]));
                }
            }
            int n = drop.listsCount = drop.lists != null ? drop.lists.size() : 0;
            if (drop.isSend) {
                drop.isAutoResponse = "on".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "auto-response", (Object)"off")));
                drop.randomCaseAutoResponse = "on".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "random-case-auto-response", (Object)"off")));
                drop.autoResponseRotation = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "auto-response-frequency", (Object)"0")));
                drop.autoReplyEmails = !"".equals(String.valueOf(Mapper.getMapValue(data, "auto-reply-emails", (Object)""))) ? String.valueOf(Mapper.getMapValue(data, "auto-reply-emails", (Object)"")).split("\n") : null;
            }
            drop.redirectFileName = "r.php";
            drop.optoutFileName = "optout.php";
            String dataSourcePath = new File(System.getProperty("base.path")).getAbsolutePath() + "/applications/bluemail/configs/application.ini";
            if (!new File(new File(System.getProperty("base.path")).getAbsolutePath() + "/applications/bluemail/configs/application.ini").exists()) return drop;
            HashMap<String, String> map = Mapper.readProperties(dataSourcePath);
            if (map == null) return drop;
            if (map.isEmpty()) return drop;
            if (map.containsKey("redirect_file")) {
                drop.redirectFileName = String.valueOf(map.get("redirect_file"));
            }
            if (!map.containsKey("optout_file")) return drop;
            drop.optoutFileName = String.valueOf(map.get("optout_file"));
            return drop;
        }
        catch (Exception e) {
            Logger.error(e, DropsHelper.class);
        }
        return drop;
    }

    public static String[] getAllRandomTags(String content) {
        Object[] tags = new String[]{};
        Pattern p = Pattern.compile("\\[(.*?)\\]");
        Matcher m = p.matcher(content);
        while (m.find()) {
            String match = m.group(1);
            String tag = match.replaceAll("[0-9]", "");
            if (!"a".equalsIgnoreCase(tag) && !"an".equalsIgnoreCase(tag) && !"al".equalsIgnoreCase(tag) && !"au".equalsIgnoreCase(tag) && !"anl".equalsIgnoreCase(tag) && !"anu".equalsIgnoreCase(tag) && !"n".equalsIgnoreCase(tag)) continue;
            tags = (String[])ArrayUtils.add((Object[])tags, (Object)match);
        }
        return tags;
    }

    public static String replaceRandomTags(String value, String[] randomTags) {
        if (value == null) return value;
        if ("".equals(value)) return value;
        if (randomTags == null) return value;
        if (randomTags.length <= 0) return value;
        String[] arrstring = randomTags;
        int n = arrstring.length;
        int n2 = 0;
        while (n2 < n) {
            String randomTag = arrstring[n2];
            if (value.contains(randomTag)) {
                value = StringUtils.replace((String)value, (String)("[" + randomTag + "]"), (String)DropsHelper.replaceRandomTag(randomTag));
            }
            ++n2;
        }
        return value;
    }

    public static String replaceRandomTag(String tag) {
        String type;
        int size = TypesParser.safeParseInt(tag.replaceAll("[^0-9]", ""));
        switch (type = tag.replaceAll("[0-9]", "")) {
            case "a": {
                return Strings.getSaltString(size, true, true, false, false);
            }
            case "al": {
                return Strings.getSaltString(size, true, true, false, false).toLowerCase();
            }
            case "au": {
                return Strings.getSaltString(size, true, true, false, false).toUpperCase();
            }
            case "an": {
                return Strings.getSaltString(size, true, true, true, false);
            }
            case "anl": {
                return Strings.getSaltString(size, true, true, true, false).toLowerCase();
            }
            case "anu": {
                return Strings.getSaltString(size, true, true, true, false).toUpperCase();
            }
            case "n": {
                return Strings.getSaltString(size, false, false, true, false);
            }
        }
        return "";
    }

    public static void uploadImage(DropComponent drop, SSH ssh) {
        if (drop == null) return;
        if ("".equalsIgnoreCase(drop.body)) return;
        if (ssh == null) return;
        if (!ssh.isConnected()) return;
        try {
            Document doc = Jsoup.parse((String)drop.body);
            Elements images = doc.select("img");
            ByteArrayOutputStream out = null;
            Iterator iterator = images.iterator();
            while (iterator.hasNext()) {
                byte[] response;
                Element image = (Element)iterator.next();
                String src = image.attr("src");
                URL url = new URL(src);
                URLConnection uc = url.openConnection();
                uc.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
                uc.connect();
                InputStream inStream = uc.getInputStream();
                if (inStream == null || inStream.available() <= 0) continue;
                try (BufferedInputStream in = new BufferedInputStream(inStream);){
                    out = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int n = 0;
                    while (-1 != (n = ((InputStream)in).read(buf))) {
                        out.write(buf, 0, n);
                    }
                    out.close();
                }
                if (out.size() <= 0 || (response = out.toByteArray()) == null || response.length <= 0 || !new File(System.getProperty("base.path") + "/tmp/").exists()) continue;
                String extension = src.substring(src.lastIndexOf("."));
                String imageName = Strings.getSaltString(20, true, true, true, false) + extension;
                try (FileOutputStream fos = new FileOutputStream(System.getProperty("base.path") + "/tmp/" + imageName);){
                    fos.write(response);
                }
                if (!new File(System.getProperty("base.path") + "/tmp/" + imageName).exists()) continue;
                ssh.uploadFile(System.getProperty("base.path") + "/tmp/" + imageName, "/var/www/html/img/" + imageName);
                drop.body = StringUtils.replace((String)drop.body, (String)src, (String)("http://[domain]/img/" + imageName));
                new File(System.getProperty("base.path") + "/tmp/" + imageName).delete();
            }
            return;
        }
        catch (Exception doc) {
            // empty catch block
        }
    }

    public static void writeThreadStatusFile(int serverId, String folder) {
        try {
            FileUtils.writeStringToFile(new File(folder + File.separator + "drop_status_" + serverId), "0");
            return;
        }
        catch (Exception e) {
            Logger.error(e, DropsHelper.class);
        }
    }

    public static boolean hasToStopDrop(int serverId, String folder) {
        boolean stop = false;
        try {
            stop = String.valueOf(FileUtils.readFileToString(new File(folder + File.separator + "drop_status_" + serverId))).trim().contains("1");
            if (!stop) return stop;
            System.out.println("Stoped !");
            return stop;
        }
        catch (Exception e) {
            Logger.error(e, DropsHelper.class);
        }
        return stop;
    }
}

