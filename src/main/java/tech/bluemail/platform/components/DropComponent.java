/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.components;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import tech.bluemail.platform.components.RotatorComponent;
import tech.bluemail.platform.models.admin.Isp;
import tech.bluemail.platform.models.admin.Negative;
import tech.bluemail.platform.models.admin.Offer;
import tech.bluemail.platform.models.admin.OfferName;
import tech.bluemail.platform.models.admin.OfferSubject;
import tech.bluemail.platform.models.admin.Server;
import tech.bluemail.platform.models.admin.Sponsor;
import tech.bluemail.platform.models.admin.Vmta;
import tech.bluemail.platform.models.lists.Fresh;

public class DropComponent
implements Serializable {
    public int id = 0;
    public boolean isSend;
    public String content;
    public String[] randomTags;
    public int mailerId;
    public int negativeId;
    public String negativeFileName;
    public Negative negativeObject;
    public boolean isNewDrop = true;
    public boolean isStoped = false;
    public String[] serversIds;
    public List<Server> servers;
    public String[] vmtasIds;
    public List<Vmta> vmtas;
    public String vmtasEmailsProcces;
    public int numberOfEmails;
    public int emailsPeriodValue;
    public String emailsPeriodType;
    public int batch;
    public long delay;
    public int vmtasRotation;
    public int fromNameId;
    public String fromName;
    public OfferName fromNameObject;
    public int subjectId;
    public String subject;
    public OfferSubject subjectObject;
    public int headersRotation;
    public String[] headers;
    public String bounceEmail;
    public String fromEmail;
    public String returnPath;
    public String replyTo;
    public String received;
    public String to;
    public boolean hasPlaceholders;
    public int placeholdersRotation;
    public String[] placeholders;
    public boolean uploadImages;
    public String charset;
    public String contentTransferEncoding;
    public String contentType;
    public String body;
    public String redirectFileName;
    public String optoutFileName;
    public boolean trackOpens;
    public String staticDomain;
    public int ispId;
    public Isp isp;
    public String emailsSplitType;
    public int testFrequency;
    public String[] testEmails;
    public String rcptfrom;
    public int dataStart;
    public int dataCount;
    public int emailsCount;
    public int emailsPerSeeds;
    public HashMap<Integer, String> lists;
    public int listsCount;
    public List<Fresh> emails;
    public int sponsorId;
    public Sponsor sponsor;
    public int offerId;
    public Offer offer;
    public int creativeId;
    public boolean isAutoResponse;
    public boolean randomCaseAutoResponse;
    public int autoResponseRotation;
    public String[] autoReplyEmails;
    public volatile String pickupsFolder;
    public volatile int emailsCounter = 0;
    public volatile RotatorComponent vmtasRotator;

    public synchronized int updateCounter() {
        return this.emailsCounter++;
    }

    public Vmta getCurrentVmta() {
        return (Vmta)this.vmtasRotator.getCurrentThenRotate();
    }
}

