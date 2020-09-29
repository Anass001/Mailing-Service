package tech.bluemail.platform.helpers;

import tech.bluemail.platform.components.*;
import java.sql.*;
import org.apache.commons.codec.binary.*;
import tech.bluemail.platform.models.production.*;
import org.codehaus.jackson.map.*;
import tech.bluemail.platform.parsers.*;
import tech.bluemail.platform.orm.*;
import tech.bluemail.platform.models.admin.*;
import java.util.*;
import tech.bluemail.platform.logging.*;
import java.util.regex.*;
import org.apache.commons.lang.*;
import tech.bluemail.platform.utils.*;
import tech.bluemail.platform.remote.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.net.*;
import java.io.*;
import org.apache.commons.io.*;

public class DropsHelper
{
    public DropsHelper() {
        super();
    }
    
    public static void saveDrop(final DropComponent dropComponent, final Server server) throws Exception {
        final Drop drop = new Drop();
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
        drop.recipientsEmails = String.join(",", (CharSequence[])dropComponent.testEmails);
        drop.header = new String(Base64.encodeBase64(String.join("\n\n", (CharSequence[])dropComponent.headers).getBytes()));
        drop.creativeId = dropComponent.creativeId;
        String[] lists = new String[0];
        for (final Map.Entry<Integer, String> en : dropComponent.lists.entrySet()) {
            lists = (String[])ArrayUtils.add((Object[])lists, (Object)String.valueOf(en.getValue()));
        }
        drop.lists = String.join("|", (CharSequence[])lists);
        drop.postData = new String(Base64.encodeBase64(dropComponent.content.getBytes()));
        dropComponent.id = drop.insert();
        if (dropComponent.id == 0) {
            throw new Exception("Error While Saving Drop !");
        }
    }
    
    public static void saveDropVmta(final DropComponent dropComponent, final Vmta vmta, final int totalSent) throws Exception {
        final DropIp dropIp = new DropIp();
        dropIp.id = 0;
        dropIp.serverId = vmta.serverId;
        dropIp.ispId = dropComponent.ispId;
        dropIp.dropId = dropComponent.id;
        dropIp.ipId = vmta.ipId;
        dropIp.dropDate = new Timestamp(System.currentTimeMillis());
        dropIp.totalSent = totalSent;
        dropIp.delivered = 0;
        dropIp.bounced = 0;
        if (dropIp.insert() == 0) {
            throw new Exception("Error While Saving Drop Ip !");
        }
    }
    
    public static DropComponent parseDropFile(final String content) throws Exception {
        DropComponent drop = null;
        if (!"".equalsIgnoreCase(content)) {
            try {
                final TreeMap<String, Object> data = (TreeMap<String, Object>)new ObjectMapper().readValue(content, (Class)TreeMap.class);
                if (data != null && !data.isEmpty()) {
                    drop = new DropComponent();
                    drop.id = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "drop-id", "0")));
                    drop.isNewDrop = (drop.id == 0);
                    drop.isSend = "true".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "drop", "false")));
                    drop.content = content;
                    drop.mailerId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "user-id", "0")));
                    drop.negativeId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "negative-id", "0")));
                    if (!"".equals(drop.negativeId) && drop.negativeId > 0) {
                        drop.negativeObject = new Negative(drop.negativeId);
                        if (drop.negativeObject != null && drop.negativeObject.value != null) {
                            drop.negativeFileName = drop.negativeObject.value;
                        }
                    }
                    drop.randomTags = getAllRandomTags(drop.content);
                    drop.serversIds = Arrays.copyOf(((List)Mapper.getMapValue(data, "servers", new ArrayList())).toArray(), ((List)Mapper.getMapValue(data, "servers", new ArrayList())).toArray().length, (Class<? extends String[]>)String[].class);
                    if (drop.serversIds != null && drop.serversIds.length > 0) {
                        drop.servers = (List<Server>)ActiveRecord.all(Server.class, "id IN (" + String.join(",", (CharSequence[])drop.serversIds) + ")", null);
                    }
                    if (drop.servers == null || drop.servers.isEmpty()) {
                        throw new Exception("No Servers Found !");
                    }
                    drop.vmtasRotation = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "vmtas-rotation", "1")));
                    drop.vmtasIds = Arrays.copyOf(((List)Mapper.getMapValue(data, "selected-vmtas", new ArrayList())).toArray(), ((List)Mapper.getMapValue(data, "selected-vmtas", new ArrayList())).toArray().length, (Class<? extends String[]>)String[].class);
                    if (drop.vmtasIds != null && drop.vmtasIds.length > 0) {
                        String condition = "id IN (";
                        for (final String vmtasId : drop.vmtasIds) {
                            if (vmtasId != null && vmtasId.contains("|")) {
                                condition = condition + vmtasId.split("\\|")[1] + ",";
                            }
                        }
                        condition = condition.substring(0, condition.length() - 1) + ")";
                        drop.vmtas = (List<Vmta>)ActiveRecord.all(Vmta.class, condition, null);
                    }
                    if (drop.vmtas == null || drop.vmtas.isEmpty()) {
                        throw new Exception("No Vmtas Found !");
                    }
                    drop.vmtasEmailsProcces = String.valueOf(Mapper.getMapValue(data, "vmtas-emails-proccess", "vmtas-rotation"));
                    drop.batch = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "batch", "1")));
                    drop.delay = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "x-delay", "1")));
                    drop.numberOfEmails = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "number-of-emails", "0")));
                    drop.emailsPeriodValue = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "emails-period-value", "0")));
                    drop.emailsPeriodType = String.valueOf(Mapper.getMapValue(data, "emails-period-type", "seconds"));
                    if ("emails-per-period".equalsIgnoreCase(drop.vmtasEmailsProcces)) {
                        if (drop.numberOfEmails == 0) {
                            throw new Exception("Number of Emails for Period is 0 !");
                        }
                        drop.batch = 1;
                        final String emailsPeriodType = drop.emailsPeriodType;
                        switch (emailsPeriodType) {
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
                                break;
                            }
                        }
                        drop.delay = (int)Math.ceil(drop.emailsPeriodValue / drop.numberOfEmails);
                    }
                    drop.sponsorId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "sponsor", "0")));
                    drop.sponsor = new Sponsor(drop.sponsorId);
                    drop.offerId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "offer", "0")));
                    drop.offer = new Offer(drop.offerId);
                    drop.creativeId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "creative", "0")));
                    drop.fromNameId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "from-name-id", "0")));
                    drop.fromName = String.valueOf(Mapper.getMapValue(data, "from-name-text", ""));
                    if ("".equals(drop.fromName) && drop.fromNameId > 0) {
                        drop.fromNameObject = new OfferName(drop.fromNameId);
                        if (drop.fromNameObject != null && drop.fromNameObject.value != null) {
                            drop.fromName = drop.fromNameObject.value;
                        }
                    }
                    drop.subjectId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "subject-id", "0")));
                    drop.subject = String.valueOf(Mapper.getMapValue(data, "subject-text", ""));
                    if ("".equals(drop.subject) && drop.subjectId > 0) {
                        drop.subjectObject = new OfferSubject(drop.subjectId);
                        if (drop.subjectObject != null && drop.subjectObject.value != null) {
                            drop.subject = drop.subjectObject.value;
                        }
                    }
                    drop.headersRotation = 1;
                    drop.headers = Arrays.copyOf(((List)Mapper.getMapValue(data, "headers", new ArrayList())).toArray(), ((List)Mapper.getMapValue(data, "headers", new ArrayList())).toArray().length, (Class<? extends String[]>)String[].class);
                    drop.bounceEmail = String.valueOf(Mapper.getMapValue(data, "bounce-email", ""));
                    drop.returnPath = String.valueOf(Mapper.getMapValue(data, "return-path", ""));
                    if (!drop.bounceEmail.contains("@") && !drop.returnPath.contains("@")) {
                        drop.returnPath = (("".equals(drop.bounceEmail) || "".equals(drop.returnPath)) ? "" : (drop.bounceEmail + "@" + drop.returnPath));
                        drop.bounceEmail = drop.returnPath;
                    }
                    drop.fromEmail = String.valueOf(Mapper.getMapValue(data, "from-email", "from@[domain]"));
                    drop.replyTo = String.valueOf(Mapper.getMapValue(data, "reply-to", "reply@[domain]"));
                    drop.received = String.valueOf(Mapper.getMapValue(data, "received", ""));
                    drop.to = String.valueOf(Mapper.getMapValue(data, "to", "[email]"));
                    drop.placeholdersRotation = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "placeholders-rotation", "1")));
                    drop.placeholders = ("".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "body-placeholders", ""))) ? new String[0] : String.valueOf(Mapper.getMapValue(data, "body-placeholders", "")).split("\r\n"));
                    drop.hasPlaceholders = (drop.placeholders.length > 0);
                    drop.uploadImages = "on".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "upload-images", "off")));
                    drop.charset = String.valueOf(Mapper.getMapValue(data, "charset", "utf-8"));
                    drop.contentTransferEncoding = String.valueOf(Mapper.getMapValue(data, "content-transfer-encoding", "7bit"));
                    drop.contentType = String.valueOf(Mapper.getMapValue(data, "content-type", "text/html"));
                    drop.body = String.valueOf(Mapper.getMapValue(data, "body", ""));
                    drop.trackOpens = (drop.isSend && "on".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "track-opens", "off"))));
                    drop.ispId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "isp-id", "0")));
                    drop.isp = new Isp(drop.ispId);
                    drop.staticDomain = String.valueOf(Mapper.getMapValue(data, "static-domain", ""));
                    drop.emailsSplitType = String.valueOf(Mapper.getMapValue(data, "emails-split-type", "vmtas"));
                    drop.testFrequency = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "send-test-after", "-1")));
                    drop.testEmails = String.valueOf(Mapper.getMapValue(data, "recipients-emails", "")).split("\\;");
                    drop.rcptfrom = String.valueOf(Mapper.getMapValue(data, "rcpt-from", ""));
                    drop.dataStart = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "data-start", "0")));
                    drop.dataCount = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "data-count", "0")));
                    drop.emailsPerSeeds = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "emails-per-seed", "1")));
                    drop.emailsCount = 0;
                    if (drop.isSend && !"".equals(Mapper.getMapValue(data, "lists", ""))) {
                        final String[] listsParts = String.valueOf(Mapper.getMapValue(data, "lists", "")).split("\\,");
                        if (listsParts != null && listsParts.length > 0) {
                            drop.lists = new HashMap<Integer, String>();
                            for (final String tmp : listsParts) {
                                drop.lists.put(TypesParser.safeParseInt(String.valueOf(tmp.split("\\|")[0])), String.valueOf(tmp.split("\\|")[1]));
                            }
                        }
                    }
                    drop.listsCount = ((drop.lists != null) ? drop.lists.size() : 0);
                    if (drop.isSend) {
                        drop.isAutoResponse = "on".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "auto-response", "off")));
                        drop.randomCaseAutoResponse = "on".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "random-case-auto-response", "off")));
                        drop.autoResponseRotation = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "auto-response-frequency", "0")));
                        drop.autoReplyEmails = (String[])("".equals(String.valueOf(Mapper.getMapValue(data, "auto-reply-emails", ""))) ? null : String.valueOf(Mapper.getMapValue(data, "auto-reply-emails", "")).split("\n"));
                    }
                    drop.redirectFileName = "r.php";
                    drop.optoutFileName = "optout.php";
                    final String dataSourcePath = new File(System.getProperty("base.path")).getAbsolutePath() + "/applications/bluemail/configs/application.ini";
                    if (new File(new File(System.getProperty("base.path")).getAbsolutePath() + "/applications/bluemail/configs/application.ini").exists()) {
                        final HashMap<String, String> map = Mapper.readProperties(dataSourcePath);
                        if (map != null && !map.isEmpty()) {
                            if (map.containsKey("redirect_file")) {
                                drop.redirectFileName = String.valueOf(map.get("redirect_file"));
                            }
                            if (map.containsKey("optout_file")) {
                                drop.optoutFileName = String.valueOf(map.get("optout_file"));
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                Logger.error(e, DropsHelper.class);
            }
        }
        return drop;
    }
    
    public static String[] getAllRandomTags(final String content) {
        String[] tags = new String[0];
        final Pattern p = Pattern.compile("\\[(.*?)\\]");
        final Matcher m = p.matcher(content);
        while (m.find()) {
            final String match = m.group(1);
            final String tag = match.replaceAll("[0-9]", "");
            if ("a".equalsIgnoreCase(tag) || "an".equalsIgnoreCase(tag) || "al".equalsIgnoreCase(tag) || "au".equalsIgnoreCase(tag) || "anl".equalsIgnoreCase(tag) || "anu".equalsIgnoreCase(tag) || "n".equalsIgnoreCase(tag)) {
                tags = (String[])ArrayUtils.add((Object[])tags, (Object)match);
            }
        }
        return tags;
    }
    
    public static String replaceRandomTags(String value, final String[] randomTags) {
        if (value != null && !"".equals(value) && randomTags != null && randomTags.length > 0) {
            for (final String randomTag : randomTags) {
                if (value.contains(randomTag)) {
                    value = StringUtils.replace(value, "[" + randomTag + "]", replaceRandomTag(randomTag));
                }
            }
        }
        return value;
    }
    
    public static String replaceRandomTag(final String tag) {
        final int size = TypesParser.safeParseInt(tag.replaceAll("[^0-9]", ""));
        final String replaceAll;
        final String type = replaceAll = tag.replaceAll("[0-9]", "");
        switch (replaceAll) {
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
            default: {
                return "";
            }
        }
    }
    
    public static void uploadImage(final DropComponent drop, final SSH ssh) {
        if (drop != null && !"".equalsIgnoreCase(drop.body) && ssh != null && ssh.isConnected()) {
            try {
                final Document doc = Jsoup.parse(drop.body);
                final Elements images = doc.select("img");
                ByteArrayOutputStream out = null;
                for (final Element image : images) {
                    final String src = image.attr("src");
                    final URL url = new URL(src);
                    final URLConnection uc = url.openConnection();
                    uc.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
                    uc.connect();
                    final InputStream inStream = uc.getInputStream();
                    if (inStream != null && inStream.available() > 0) {
                        final InputStream in = new BufferedInputStream(inStream);
                        try {
                            out = new ByteArrayOutputStream();
                            final byte[] buf = new byte[1024];
                            int n = 0;
                            while (-1 != (n = in.read(buf))) {
                                out.write(buf, 0, n);
                            }
                            out.close();
                            in.close();
                        }
                        catch (Throwable t) {
                            try {
                                in.close();
                            }
                            catch (Throwable exception) {
                                t.addSuppressed(exception);
                            }
                            throw t;
                        }
                        if (out.size() <= 0) {
                            continue;
                        }
                        final byte[] response = out.toByteArray();
                        if (response == null || response.length <= 0 || !new File(System.getProperty("base.path") + "/tmp/").exists()) {
                            continue;
                        }
                        final String extension = src.substring(src.lastIndexOf("."));
                        final String imageName = Strings.getSaltString(20, true, true, true, false) + extension;
                        final FileOutputStream fos = new FileOutputStream(System.getProperty("base.path") + "/tmp/" + imageName);
                        try {
                            fos.write(response);
                            fos.close();
                        }
                        catch (Throwable t2) {
                            try {
                                fos.close();
                            }
                            catch (Throwable exception2) {
                                t2.addSuppressed(exception2);
                            }
                            throw t2;
                        }
                        if (!new File(System.getProperty("base.path") + "/tmp/" + imageName).exists()) {
                            continue;
                        }
                        ssh.uploadFile(System.getProperty("base.path") + "/tmp/" + imageName, "/var/www/html/img/" + imageName);
                        drop.body = StringUtils.replace(drop.body, src, "http://[domain]/img/" + imageName);
                        new File(System.getProperty("base.path") + "/tmp/" + imageName).delete();
                    }
                }
            }
            catch (Exception ex) {}
        }
    }
    
    public static void writeThreadStatusFile(final int serverId, final String folder) {
        try {
            FileUtils.writeStringToFile(new File(folder + File.separator + "drop_status_" + serverId), "0");
        }
        catch (Exception e) {
            Logger.error(e, DropsHelper.class);
        }
    }
    
    public static boolean hasToStopDrop(final int serverId, final String folder) {
        boolean stop = false;
        try {
            stop = String.valueOf(FileUtils.readFileToString(new File(folder + File.separator + "drop_status_" + serverId))).trim().contains("1");
            if (stop) {
                System.out.println("Stoped !");
            }
        }
        catch (Exception e) {
            Logger.error(e, DropsHelper.class);
        }
        return stop;
    }
}
