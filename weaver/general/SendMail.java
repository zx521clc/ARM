/**
 * Title:        发送邮件类
 * Company:      泛微软件
 * @author:      刘煜
 * @version:     1.0
 * create date : 2001-10-23
 * modify log:
 *
 *
 * Description:  邮件的发送，采用 SMTP 协议， SMTP 服务器在系统的设置中设置
 *               SMTP 服务器在系统的设置中设置：
 *               在ECOLOGY 系统中， 进入系统，选择设置，设置 SMTP 服务器
 *
 *
 */

package weaver.general;

import javax.mail.Message.*;
import javax.mail.*;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.internet.*;
import javax.activation.*;

import java.security.Security;
import java.util.*;
import java.io.*;
import java.net.*;

import org.apache.oro.text.regex.*;

import weaver.system.*;
import weaver.email.*;
import weaver.email.FileDataSource;
import weaver.email.timer.MailSendApartRunable;
import weaver.conn.RecordSet;
import weaver.hrm.User;


public class SendMail extends MailErrorBean {
    
    static {
        System.getProperties().setProperty("mail.mime.splitlongparameters", "false"); // linux 会默认为 true，会截断附件名（ava Mail 附件名太长导致接收端附件名解析出错）
        System.setProperty("mail.mime.splitlongparameters", "false"); // linux 会默认为 true，会截断附件名（ava Mail 附件名太长导致接收端附件名解析出错）
    }
    
    private BaseBean logBean = new BaseBean();

    /** 原来的:&lt;IMG alt=docimages_  修改为:&lt;img alt="docimages_ */
    public static String IMAGE_FLAG = "<img alt=\"docimages_";
    private boolean isDebug = false;
    
    /**
     *  构造方法
     */
    public SendMail() {
    }

    private SystemComInfo cominfo = new SystemComInfo();  // 系统设置，默认取群发参数
    
    private String username = cominfo.getDefmailuser();  // 登录名
    private String password = cominfo.getDefmailpassword();  // 密码
    private String mailserver = cominfo.getDefmailserver();  // 发件服务器地址
    private boolean needauthsend = "1".equals(cominfo.getDefneedauth());  // 是否发件认证
    private String needSSL = cominfo.getNeedSSL();  // 是否开启SSL模式
    private String smtpServerPort = cominfo.getSmtpServerPort();  // 发件服务端口
    private String needReceipt = "";  // 是否需要阅读回执
    private String accountName = cominfo.getMailAccountName();  // 发件人昵称
    private boolean isStartTls = "1".equals(cominfo.getMailIsStartTls());  // 是否启用starttls方式
    
    private boolean isSendApart = false; //是否是分别发送
    private int bindMailId = -1; //邮件模块，分别发送，纯文本发送时设置的值。
    
    public String getSmtpServerPort() {
        return smtpServerPort;
    }

    public void setSmtpServerPort(String smtpServerPort) {
        this.smtpServerPort = smtpServerPort;
    }

    public String getNeedSSL() {
        return needSSL;
    }
    
    public void setNeedSSL(String needSSL) {
        this.needSSL = needSSL;
    }

    public void setNeedauthsend(boolean needauthsend) {
        this.needauthsend = needauthsend;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setMailServer(String mailserver) {
        this.mailserver = mailserver;
    }

    public String getNeedReceipt() {
        return needReceipt;
    }

    public void setNeedReceipt(String needReceipt) {
        this.needReceipt = needReceipt;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public boolean isStartTls() {
        return isStartTls;
    }
    
    public void setStartTls(boolean isStartTls) {
        this.isStartTls = isStartTls;
    }

    /**
     * 获取isSendApart
     * @return the isSendApart
     */
    public boolean isSendApart() {
        return isSendApart;
    }
    
    /**
     * 设置isSendApart
     * @param isSendApart the isSendApart to set
     */
    public void setIsSendApart(boolean isSendApart) {
        this.isSendApart = isSendApart;
    }
    
    /**
     * 设置bindMailId
     * @param bindMailId the bindMailId to set
     */
    public void setBindMailId(int bindMailId) {
        this.bindMailId = bindMailId;
    }

    /**
     * 发送文本邮件
     *
     * @param from  邮件的发件人参数
     * @param to    邮件的收件人参数，多个收件人之间用逗号隔开
     * @param cc    邮件的抄送人参数，多个抄送人之间用逗号隔开
     * @param bcc   邮件的暗送人参数，多个暗送人之间用逗号隔开
     * @param subject    邮件的主题参数（采用ISO8859编码格式）
     * @param body       邮件的正文参数（采用ISO8859编码格式）
     * @param priority   邮件的重要性参数 3：普通 2：重要 4：紧急
     *
     * @return boolean 邮件发送成功，返回 true，否则，返回false
     */
    public boolean send(String from, String to, String cc, String bcc, String subject, String body, String priority) {
        Session _session = null;
        
        Properties props = new Properties();
        props.put("mail.smtp.from", from);
        props.put("mail.from", from);

        _session = setSSLConnectMsg(_session, props);

        MimeMessage msg = new MimeMessage(_session);
        try {
            msg.setFrom(new InternetAddress(from, this.accountName));
            if( !this.isSendApart ) {
                msg.setRecipients(RecipientType.TO, InternetAddress.parse(to, true));
                if (cc != null) {
                    msg.setRecipients(RecipientType.CC, InternetAddress.parse(cc, true));
                }
                if (bcc != null) {
                    msg.setRecipients(RecipientType.BCC, InternetAddress.parse(bcc, true));
                }
            }
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());
            msg.setText(body);
            msg.setHeader("X-Mailer", "weaver");
            if (null != this.needReceipt && "1".equals(this.needReceipt)) {// 需要回执
                msg.setHeader("Disposition-Notification-To", from); // from为发送者邮件地址
            }
            if (priority != null) {
                msg.setHeader("X-Priority", priority);
            }

            if(this.isSendApart) {
                new Thread(new MailSendApartRunable(this.bindMailId, msg, to)).start();
            } else {
                Transport.send(msg);
            }

            return true;
        } catch (Exception e) {
            logBean.writeLog("method:send, from=" + from + ",subject=" + subject);
            logBean.writeLog(e);
            MailErrorMessageInfo merinfo =  new MailErrorFormat(e).getMailErrorMessageInfo();
            setErrorMess(merinfo.toString());
            return false;
        }
    }

    /**
     * 发送HTML邮件，具体到每个email地址的发送
     * 
     * @param id
     * @param to
     * @param subject
     * @param body
     * @param sendToId
     * @return
     */
    public boolean sendhtmlProxy(int id, String to, String subject, String body, String sendToId) {
        char flag = Util.getSeparator();
        String para = "";
        para = id + "" + flag + to + flag + subject + flag + body + flag + "0" + flag + sendToId;
        RecordSet rs = new RecordSet();
        rs.executeProc("MailSendRecord_Insert", para);
        return true;
    }

    /**
     * 发送HTML邮件，具体到每批发送的信息，扳回存储到数据库中主表的id，由这个id可以存贮这个批次的明细信息
     * @param from
     * @param cc
     * @param bcc
     * @param char_set
     * @param priority
     * @param user
     * @param sendDate
     * @param sendTime
     * @param sendToType
     * @return
     */
    public int sendhtmlMain(String from, String cc, String bcc, int char_set, String priority, User user, String sendDate, String sendTime, String sendToType) {
        char flag = Util.getSeparator();
        String para = "";
        RecordSet rs = new RecordSet();
        int ret = 0;
        rs.executeProc("SequenceIndex_SMailSendId","");
        if(rs.next()){
            ret = rs.getInt(1);
        }
        para = ret + "" + flag + from + flag + cc + flag + bcc + flag + "" + char_set + flag + priority + flag + sendDate + flag + sendTime + flag + "0" + flag + sendToType + flag + "" + user.getUID();
        rs.executeProc("MailSendMain_Insert", para);

        return ret;
    }

    /**
     * 发送HTML邮件
     *
     * @param from  邮件的发件人参数
     * @param to    邮件的收件人参数，多个收件人之间用逗号隔开
     * @param cc    邮件的抄送人参数，多个抄送人之间用逗号隔开
     * @param bcc   邮件的暗送人参数，多个暗送人之间用逗号隔开
     * @param subject    邮件的主题参数（采用ISO8859编码格式）
     * @param body       邮件的正文参数（采用ISO8859编码格式）
     * @param char_set   编码方式 1：iso-8859-1 2：big5 3：GBK
     * @param priority   邮件的重要性参数 3：普通 2：重要 4：紧急
     *
     * @return boolean 邮件发送成功，返回 true，否则，返回false
     */
    public boolean sendhtml(String from, String to, String cc, String bcc, String subject, String body, int char_set, String priority) {
        Session _session = null;
        if (from == null || "".equals(from)) {
            return false;
        }
        Properties props = new Properties();
        props.put("mail.smtp.from", from);
        props.put("mail.from", from);

        _session = setSSLConnectMsg(_session, props);
        MimeMessage msg = new MimeMessage(_session);
        try {
            msg.setFrom(new InternetAddress(from, this.accountName));
            msg.setRecipients(RecipientType.TO, InternetAddress.parse(to));
            if (cc != null) {
                msg.setRecipients(RecipientType.CC, InternetAddress.parse(cc, true));
            }
            if (bcc != null) {
                msg.setRecipients(RecipientType.BCC, InternetAddress.parse(bcc, true));
            }
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());
            msg.setHeader("X-Mailer", "weaver");
            if (null != this.needReceipt && "1".equals(this.needReceipt)) {// 需要回执
                msg.setHeader("Disposition-Notification-To", from); // from为发送者邮件地址
            }
            if (priority != null) {
                msg.setHeader("X-Priority", priority);
            }

            String charset = null;
            switch (char_set) {
                case 1: {
                    charset = "iso-8859-1";
                    break;
                }
                case 2: {
                    charset = "big5";
                    break;
                }
                case 3: {
                    charset = "UTF-8";
                    break;
                }
                default: {
                    charset = "UTF-8";
                    break;
                }
            }
            
            int tmppos = body.indexOf(IMAGE_FLAG);
            ArrayList list = new ArrayList();
            String docimageNum = "0";
            int tmppos2 = 0;
            while (tmppos != -1) {
                tmppos2 = tmppos + IMAGE_FLAG.length();
                tmppos = body.indexOf("\"", tmppos2);
                docimageNum = body.substring(tmppos2, tmppos);
                tmppos = body.indexOf("src=", tmppos);
                int startpos = body.indexOf("\"", tmppos);
                int endpos = body.indexOf("\"", startpos + 1);
                String tempStr = body.substring(startpos + 1, endpos);
                String replaceStr = "cid:img" + docimageNum + "@www.weaver.com.cn";

                if (tempStr.indexOf("weaver.file.FileDownload") != -1) {
                    tmppos = body.indexOf(IMAGE_FLAG, startpos + tempStr.length());
                    continue;
                }
                body = Util.StringReplace(body, tempStr, replaceStr);
                tmppos = body.indexOf(IMAGE_FLAG, startpos + replaceStr.length());
                list.add(docimageNum);
            }
            this.processBodyImg(body);
            
            int n = list.size();// 这是由上传图片的CID计数开始。
            int groups;
            PatternMatcher matcher;
            PatternCompiler compiler;
            Pattern pattern;
            PatternMatcherInput input;
            MatchResult result;

            compiler = new Perl5Compiler();
            matcher  = new Perl5Matcher();
            
            ArrayList list3 = new ArrayList();
            input = new PatternMatcherInput(body);
            pattern = compiler.compile("<img.*?src=['\"\\s]?(/.*?weaver.file.FileDownload\\?fileid=(\\d*)).*?>", Perl5Compiler.CASE_INSENSITIVE_MASK);
            while (matcher.contains(input, pattern)) {
                result = matcher.getMatch();
                body = Util.StringReplace(body, result.group(1), "cid:img" + n + "@www.weaver.com.cn");
                list3.add(result.group(2));
                n++;
            }
            this.processBodyImg(body);
            
            // 新建一个MimeMultipart对象用来存放BodyPart对象(事实上可以存放多个)
            MimeMultipart mm = new MimeMultipart();
            // 新建一个存放信件内容的BodyPart对象
            BodyPart mdp = new MimeBodyPart();
            // 给BodyPart对象设置内容和格式/编码方式
            mdp.setContent(body, "text/html;  charset=\"" + charset + "\"");
            mdp.addHeader("Content-Transfer-Encoding", "base64");
            org.apache.ws.commons.util.Base64 enc = new org.apache.ws.commons.util.Base64();
            body = enc.encode(body.getBytes());
            //这句很重要
            mm.setSubType("related");
            mm.addBodyPart(mdp);
            
            for (int j = 0; j < list.size(); j++) {
                // 新建一个存放附件的BodyPart
                mdp = new MimeBodyPart();
                mdp.setDataHandler(new DataHandler(new FileDataSource((String) list.get(j))));
                // mdp.setFileName(j+".jpg");
                mdp.setHeader("Content-ID", "<img" + j + "@www.weaver.com.cn>");
                // 将含有附件的BodyPart加入到MimeMultipart对象中
                mm.addBodyPart(mdp);
            }
            
            n = list.size();
            for (int j = 0; j < list3.size(); j++) {
                try {
                    String imageFileid = (String) list3.get(j);
                    mdp = new MimeBodyPart();
                    mdp.setDataHandler(new DataHandler(new FileDataSource(imageFileid)));
                    mdp.setHeader("Content-Type", "image/gif");
                    mdp.setHeader("Content-ID", "<img" + (n++) + "@www.weaver.com.cn>");
                    mm.addBodyPart(mdp);
                } catch (NullPointerException nulle) {
                    logBean.writeLog(nulle);
                    continue;
                }
            }
            
            
            //把mm作为消息对象的内容
            msg.setContent(mm);
            msg.saveChanges();

            //msg.writeTo(new BufferedOutputStream(new FileOutputStream(new File("D:/EMLS/" + System.currentTimeMillis() + ".eml"))));
            
            Transport.send(msg);

            return true;
        } catch (Exception e) {
            logBean.writeLog("method:sendhtml, from=" + from + ",subject=" + subject + ",to=" + to);
            logBean.writeLog(e);
            return false;
        }
    }
    
    /**
     * 发送HTML邮件(用于流程、日程、会议提醒等调用，向上抛出异常！！)
     * 此方法内部不处理任何异常，会直接向上抛出！！！
     *
     * @param from  邮件的发件人参数
     * @param to    邮件的收件人参数，多个收件人之间用逗号隔开
     * @param cc    邮件的抄送人参数，多个抄送人之间用逗号隔开
     * @param bcc   邮件的暗送人参数，多个暗送人之间用逗号隔开
     * @param subject    邮件的主题参数（采用ISO8859编码格式）
     * @param body       邮件的正文参数（采用ISO8859编码格式）
     * @param char_set   编码方式 1：iso-8859-1 2：big5 3：GBK
     * @param priority   邮件的重要性参数 3：普通 2：重要 4：紧急
     *
     * @return boolean 邮件发送成功，返回 true，否则，返回false
     */
    public boolean sendhtmlForRemind(String from, String to, String cc, String bcc, String subject, String body, int char_set, String priority) throws Exception {
        Session _session = null;
        if (from == null || "".equals(from)) {
            return false;
        }
        if(to == null && cc == null && bcc == null) {
            return false;
        }
        Properties props = new Properties();
        props.put("mail.smtp.from", from);
        props.put("mail.from", from);

        _session = setSSLConnectMsg(_session, props);
        MimeMessage msg = new MimeMessage(_session);
        msg.setFrom(new InternetAddress(from, this.accountName));
        if(to != null) {
        	msg.setRecipients(RecipientType.TO, InternetAddress.parse(to));
        }
        if (cc != null) {
            msg.setRecipients(RecipientType.CC, InternetAddress.parse(cc, true));
        }
        if (bcc != null) {
            msg.setRecipients(RecipientType.BCC, InternetAddress.parse(bcc, true));
        }
        msg.setSubject(subject, "UTF-8");
        msg.setSentDate(new Date());
        msg.setHeader("X-Mailer", "weaver");
        if (null != this.needReceipt && "1".equals(this.needReceipt)) {// 需要回执
            msg.setHeader("Disposition-Notification-To", from); // from为发送者邮件地址
        }
        if (priority != null) {
            msg.setHeader("X-Priority", priority);
        }

        String charset = null;
        switch (char_set) {
            case 1: {
                charset = "iso-8859-1";
                break;
            }
            case 2: {
                charset = "big5";
                break;
            }
            case 3: {
                charset = "UTF-8";
                break;
            }
            default: {
                charset = "UTF-8";
                break;
            }
        }
        
        int tmppos = body.indexOf(IMAGE_FLAG);
        ArrayList list = new ArrayList();
        String docimageNum = "0";
        int tmppos2 = 0;
        while (tmppos != -1) {
            tmppos2 = tmppos + IMAGE_FLAG.length();
            tmppos = body.indexOf("\"", tmppos2);
            docimageNum = body.substring(tmppos2, tmppos);
            tmppos = body.indexOf("src=", tmppos);
            int startpos = body.indexOf("\"", tmppos);
            int endpos = body.indexOf("\"", startpos + 1);
            String tempStr = body.substring(startpos + 1, endpos);
            String replaceStr = "cid:img" + docimageNum + "@www.weaver.com.cn";

            if (tempStr.indexOf("weaver.email.FileDownloadLocation") != -1 || tempStr.indexOf("weaver.file.FileDownload") != -1) {
                tmppos = body.indexOf(IMAGE_FLAG, startpos + tempStr.length());
                continue;
            }
            body = Util.StringReplace(body, tempStr, replaceStr);
            tmppos = body.indexOf(IMAGE_FLAG, startpos + replaceStr.length());
            list.add(docimageNum);
        }
        this.processBodyImg(body);
        
        int n = list.size();// 这是由上传图片的CID计数开始。
        int groups;
        PatternMatcher matcher;
        PatternCompiler compiler;
        Pattern pattern;
        PatternMatcherInput input;
        MatchResult result;

        compiler = new Perl5Compiler();
        matcher  = new Perl5Matcher();
        
        ArrayList list2 = new ArrayList();
        pattern = compiler.compile("<img.*?src=['\"\\s]?(/.*?weaver.email.FileDownloadLocation\\?fileid=(\\d*)).*?>", Perl5Compiler.CASE_INSENSITIVE_MASK);
        input = new PatternMatcherInput(body);
        String mailResourceFileRealPath = "";
        while (matcher.contains(input, pattern)) {
            result = matcher.getMatch();
            body = Util.StringReplace(body, result.group(1), "cid:img" + n + "@www.weaver.com.cn");
            list2.add(result.group(2));
            n++;
        }
        this.processBodyImg(body);
        
        ArrayList list3 = new ArrayList();
        input = new PatternMatcherInput(body);
        pattern = compiler.compile("<img.*?src=['\"\\s]?(/.*?weaver.file.FileDownload\\?fileid=(\\d*)).*?>", Perl5Compiler.CASE_INSENSITIVE_MASK);
        while (matcher.contains(input, pattern)) {
            result = matcher.getMatch();
            body = Util.StringReplace(body, result.group(1), "cid:img" + n + "@www.weaver.com.cn");
            list3.add(result.group(2));
            n++;
        }
        this.processBodyImg(body);
        
        // 新建一个MimeMultipart对象用来存放BodyPart对象(事实上可以存放多个)
        MimeMultipart mm = new MimeMultipart();
        // 新建一个存放信件内容的BodyPart对象
        BodyPart mdp = new MimeBodyPart();
        // 给BodyPart对象设置内容和格式/编码方式
        mdp.setContent(body, "text/html;  charset=\"" + charset + "\"");
        mdp.addHeader("Content-Transfer-Encoding", "base64");
        org.apache.ws.commons.util.Base64 enc = new org.apache.ws.commons.util.Base64();
        body = enc.encode(body.getBytes());
        //这句很重要
        mm.setSubType("related");
        mm.addBodyPart(mdp);
        
        for (int j = 0; j < list.size(); j++) {
            // 新建一个存放附件的BodyPart
            mdp = new MimeBodyPart();
            mdp.setDataHandler(new DataHandler(new FileDataSource((String) list.get(j))));
            // mdp.setFileName(j+".jpg");
            mdp.setHeader("Content-ID", "<img" + j + "@www.weaver.com.cn>");
            // 将含有附件的BodyPart加入到MimeMultipart对象中
            mm.addBodyPart(mdp);
        }
        
        n = list.size();
        RecordSet rs = new RecordSet();
        for (int j = 0; j < list2.size(); j++) {
            try {
                String mailResourceFileId = (String)list2.get(j);
                rs.executeQuery("SELECT isaesencrypt,aescode,filerealpath FROM MailResourceFile WHERE id=?", mailResourceFileId);
                rs.next();
                mailResourceFileRealPath = rs.getString("filerealpath");
                String isaesencrypt = rs.getString("isaesencrypt");
                String aescode = rs.getString("aescode");

                File file = new File(mailResourceFileRealPath);
                if (!file.exists()) {
                    continue;
                }

                mdp = new MimeBodyPart();
                mdp.setDataHandler(new DataHandler(new FileDataSource(mailResourceFileRealPath, isaesencrypt, aescode)));
                mdp.setHeader("Content-Type", "image/gif");
                mdp.setHeader("Content-ID", "<img" + (n++) + "@www.weaver.com.cn>");
                mm.addBodyPart(mdp);
            } catch (NullPointerException nulle) {
                logBean.writeLog(nulle);
                continue;
            }
        }

        for (int j = 0; j < list3.size(); j++) {
            try {
                String imageFileid = (String) list3.get(j);
                mdp = new MimeBodyPart();
                mdp.setDataHandler(new DataHandler(new FileDataSource(imageFileid)));
                mdp.setHeader("Content-Type", "image/gif");
                mdp.setHeader("Content-ID", "<img" + (n++) + "@www.weaver.com.cn>");
                mm.addBodyPart(mdp);
            } catch (NullPointerException nulle) {
                logBean.writeLog(nulle);
                continue;
            }
        }
        
        //把mm作为消息对象的内容
        msg.setContent(mm);
        msg.saveChanges();
        //msg.writeTo(new BufferedOutputStream(new FileOutputStream(new File("D:/EMLS/" + System.currentTimeMillis() + ".eml"))));
        Transport.send(msg);
        return true;
    }

    /**
     * 发送HTML邮件,HTML内容中包含图片。
     *
     * @param from  邮件的发件人参数
     * @param to    邮件的收件人参数，多个收件人之间用逗号隔开
     * @param cc    邮件的抄送人参数，多个抄送人之间用逗号隔开
     * @param bcc   邮件的暗送人参数，多个暗送人之间用逗号隔开
     * @param subject    邮件的主题参数（采用ISO8859编码格式）
     * @param body       邮件的正文参数（采用ISO8859编码格式）
     * @param char_set   编码方式 1：iso-8859-1 2：big5 3：GBK
     * @param priority   邮件的重要性参数 3：普通 2：重要 4：紧急
     * @param imgnames   HTML内容中包含的图片
     *
     * @return boolean 邮件发送成功，返回 true，否则，返回false
     */
    public boolean sendhtml(String from, String to, String cc, String bcc, String subject, String body, int char_set, String priority, Hashtable imgnames, int mailid) {
        File file = null;
        Session _session = null;
        RecordSet rs = new RecordSet();

        Properties props = new Properties();
        props.put("mail.smtp.from", from);
        props.put("mail.from", from);

        _session = setSSLConnectMsg(_session, props);

        MimeMessage msg = new MimeMessage(_session);
        try {
            msg.setFrom(new InternetAddress(from, this.accountName));
            if(!this.isSendApart) {
                msg.setRecipients(RecipientType.TO, InternetAddress.parse(to));
                if (cc != null) {
                    msg.setRecipients(RecipientType.CC, InternetAddress.parse(cc, true));
                }
                if (bcc != null) {
                    msg.setRecipients(RecipientType.BCC, InternetAddress.parse(bcc, true));
                }
            }
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());
            msg.setHeader("X-Mailer", "weaver");
            if (this.needReceipt != null && "1".equals(this.needReceipt)) {// 需要回执
                msg.setHeader("Disposition-Notification-To", from); // from为发送者邮件地址
            }
            if (priority != null) {
                msg.setHeader("X-Priority", priority);
            }
            
            String charset = null;
            switch (char_set) {
                case 1: {
                    charset = "iso-8859-1";
                    break;
                }
                case 2: {
                    charset = "big5";
                    break;
                }
                case 3: {
                    charset = "UTF-8";
                    break;
                }
                default: {
                    charset = "UTF-8";
                    break;
                }
            }
            
            this.processBodyImg(body);
            int tmppos = body.indexOf(IMAGE_FLAG);
            ArrayList list = new ArrayList();
            String docimageNum = "0";
            int tmppos2 = 0;
            while (tmppos != -1) {
                tmppos2 = tmppos + IMAGE_FLAG.length();
                tmppos = body.indexOf("\"", tmppos2);
                docimageNum = body.substring(tmppos2, tmppos);
                tmppos = body.indexOf("src=", tmppos);
                int startpos = body.indexOf("\"", tmppos);
                int endpos = body.indexOf("\"", startpos + 1);
                String tempStr = body.substring(startpos + 1, endpos);
                String replaceStr = "cid:img" + docimageNum + "@www.weaver.com.cn";

                if (tempStr.indexOf("weaver.email.FileDownloadLocation") != -1 || tempStr.indexOf("weaver.file.FileDownload") != -1) {
                    tmppos = body.indexOf(IMAGE_FLAG, startpos + tempStr.length());
                    continue;
                }
                body = Util.StringReplace(body, tempStr, replaceStr);
                tmppos = body.indexOf(IMAGE_FLAG, startpos + replaceStr.length());
                list.add(docimageNum);
            }
            this.processBodyImg(body);

            // 将HTML内容中的图片路径替换为cid:格式后，更新邮件内容。
            if (mailid > 0 && !list.isEmpty()) {
                MailCommonUtils.updateMailContent(mailid, body);
            }

            //===========================================================================
            ArrayList list2 = new ArrayList();
            int groups;
            PatternMatcher matcher;
            PatternCompiler compiler;
            Pattern pattern;
            PatternMatcherInput input;
            MatchResult result;

            compiler = new Perl5Compiler();
            matcher  = new Perl5Matcher();

            pattern = compiler.compile("<img.*?src=['\"\\s]?(/.*?weaver.email.FileDownloadLocation\\?fileid=(\\d*)).*?>", Perl5Compiler.CASE_INSENSITIVE_MASK);

            input = new PatternMatcherInput(body);
            int n = list.size();// 这是由上传图片的CID计数开始。
            String mailResourceFileId = "";
            String mailResourceFileRealPath = "";
            while (matcher.contains(input, pattern)) {
                result = matcher.getMatch();
                body = Util.StringReplace(body, result.group(1), "cid:img" + n + "@www.weaver.com.cn");
                list2.add(result.group(2));
                n++;
            }
            
            this.processBodyImg(body);
            ArrayList list3 = new ArrayList();
            input = new PatternMatcherInput(body);
            pattern = compiler.compile("<img.*?src=['\"\\s]?(/.*?weaver.file.FileDownload\\?fileid=(\\d*)).*?>", Perl5Compiler.CASE_INSENSITIVE_MASK);
            while (matcher.contains(input, pattern)) {
                result = matcher.getMatch();
                body = Util.StringReplace(body, result.group(1), "cid:img" + n + "@www.weaver.com.cn");
                list3.add(result.group(2));
                n++;
            }
            this.processBodyImg(body);

            // 新建一个MimeMultipart对象用来存放BodyPart对象(事实上可以存放多个)
            MimeMultipart mm = new MimeMultipart();
            // 新建一个存放信件内容的BodyPart对象
            BodyPart mdp = new MimeBodyPart();
            // 给BodyPart对象设置内容和格式/编码方式

            mdp.setContent(body, "text/html;  charset=\"" + charset + "\"");
            mdp.addHeader("Content-Transfer-Encoding", "base64");
            org.apache.ws.commons.util.Base64 enc = new org.apache.ws.commons.util.Base64();
            body = enc.encode(body.getBytes());
            mm.setSubType("related");
            mm.addBodyPart(mdp);
        
            for (int j = 0; j < list.size(); j++) {
                try {
                    Object obj1 = imgnames.get(list.get(j));
                    String imgfilerealpath = (obj1 != null) ? (String) obj1 : (String) imgnames.get(String.valueOf(j));
                    
                    file = new File(imgfilerealpath);
                    String imgfilename = imgfilerealpath.substring(imgfilerealpath.lastIndexOf("\\") + 1);

                    String imageFileid = (String) list.get(j);
                    // 新建一个存放附件的BodyPart
                    mdp = new MimeBodyPart();
                    mdp.setDataHandler(new DataHandler(new FileDataSource(imageFileid)));
                    // mdp.setFileName(j+".jpg");
                    mdp.setHeader("Content-Type", "image/gif");
                    mdp.setHeader("Content-ID", "<img" + list.get(j) + "@www.weaver.com.cn>");
                    //将含有附件的BodyPart加入到MimeMultipart对象中 
                    mm.addBodyPart(mdp);

                    String imgsql = "INSERT INTO MailResourceFile (mailid,filename,attachfile,filetype,filerealpath,iszip,isencrypt,isfileattrachment,fileContentId,isEncoded,filesize) VALUES ("+mailid+",'"+imgfilename+"',null,'image/gif','"+imgfilerealpath+"','0','0','0','img"+list.get(j)+"@www.weaver.com.cn','0',0)";
                    rs.executeSql(imgsql);
                }catch(NullPointerException nulle){
                    logBean.writeLog(nulle);
                    continue;
                }
            }
            n = list.size();
            for (int j = 0; j < list2.size(); j++) {
                try {
                    mailResourceFileId = (String) list2.get(j);
                    rs.executeSql("SELECT isaesencrypt,aescode,filerealpath FROM MailResourceFile WHERE id=" + mailResourceFileId + "");
                    rs.next();
                    mailResourceFileRealPath = rs.getString("filerealpath");
                    String isaesencrypt = rs.getString("isaesencrypt");
                    String aescode = rs.getString("aescode");

                    file = new File(mailResourceFileRealPath);
                    if (!file.exists()) {
                        continue;
                    }

                    mdp = new MimeBodyPart();
                    mdp.setDataHandler(new DataHandler(new FileDataSource(mailResourceFileRealPath, isaesencrypt, aescode)));
                    mdp.setHeader("Content-Type", "image/gif");
                    mdp.setHeader("Content-ID", "<img" + (n++) + "@www.weaver.com.cn>");
                    mm.addBodyPart(mdp);
                } catch (NullPointerException nulle) {
                    logBean.writeLog(nulle);
                    continue;
                }
            }

            for (int j = 0; j < list3.size(); j++) {
                try {
                    mailResourceFileId = (String) list3.get(j);
                    mdp = new MimeBodyPart();
                    mdp.setDataHandler(new DataHandler(new FileDataSource(mailResourceFileId)));
                    mdp.setHeader("Content-Type", "image/gif");
                    mdp.setHeader("Content-ID", "<img" + (n++) + "@www.weaver.com.cn>");
                    mm.addBodyPart(mdp);
                } catch (NullPointerException nulle) {
                    logBean.writeLog(nulle);
                    continue;
                }
            }

            //把mm作为消息对象的内容
            msg.setContent(mm);
            
            if(this.isSendApart) {
                new Thread(new MailSendApartRunable(mailid, msg, to)).start();
            } else {
                msg.saveChanges();
                //msg.writeTo(new BufferedOutputStream(new FileOutputStream(new File("D:/EMLS/" + System.currentTimeMillis() + ".eml"))));
                Transport.send(msg);
            }

            return true;
        } catch (Exception e) {
            logBean.writeLog("method:sendhtml, from=" + from + ",subject=" + subject + ",to=" + to + ",mailid=" + mailid);
            logBean.writeLog(e);
            MailErrorMessageInfo merinfo =  new MailErrorFormat(e).getMailErrorMessageInfo();
            setErrorMess(merinfo.toString());
            return false;
        }
    }
    /**
     * Title:设置ssl连接 
     * @param _session
     * @param props
     * @return
     * @return Session
     */
    public Session setSSLConnectMsg(Session _session, Properties props) {
        String userId = logBean.getPropValue("openmailbasebean","hrm_id");
        String sendmailkey = logBean.getPropValue("openmailbasebean","sendMail_Debug_switch");
        props.setProperty("mail.smtp.port", smtpServerPort); //设置端口信息 
        props.put("mail.smtp.host", mailserver);
        props.put("mail.transport.protocol", "smtp");
        
        // 发送邮件建立连接和发送超时设置
        props.setProperty("mail.smtp.connectiontimeout", String.valueOf(1000 * 120));  //ConnectTimeout建立连接超时，10秒
        props.setProperty("mail.smtp.timeout", String.valueOf(1000 * 120));  //ReadTimeout 读取超时30秒

        // Java Mail 附件名太长导致接收端附件名解析出错(出现在javaxmail 1.5.3版本以后)
        System.getProperties().setProperty("mail.mime.splitlongparameters", "false"); // linux 会默认为 true，会截断附件名（ava Mail 附件名太长导致接收端附件名解析出错）
        System.setProperty("mail.mime.splitlongparameters", "false"); // linux 会默认为 true，会截断附件名（ava Mail 附件名太长导致接收端附件名解析出错）
        
        Authenticator auth = null;
        
        if (needauthsend) { // 需要发件认证
            props.put("mail.smtp.auth", "true");
            auth = new Email_Autherticator(username, password);
        }
        
        if("1".equals(this.needSSL)) {
            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider()); 
            final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory"; 
            
            props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY); 
            props.setProperty("mail.smtp.socketFactory.fallback", "false"); 
            props.setProperty("mail.smtp.socketFactory.port", smtpServerPort); 
            props.put("mail.smtp.auth", "true");
            
            _session = Session.getInstance(props, new Authenticator(){ 
                protected PasswordAuthentication getPasswordAuthentication() { 
                    return new PasswordAuthentication(username, password); 
                }});
        } else {
            // 是否启用了starttls方式
            if (this.isStartTls) {
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.ssl.checkserveridentity", "false");
                props.put("mail.smtp.ssl.trust", mailserver);
                
                props.setProperty("mail.smtp.socketFactory.fallback", "false");
                props.setProperty("mail.smtp.socketFactory.port", String.valueOf(smtpServerPort));
                
                _session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
            } else {
                _session = Session.getInstance(props, auth);
            }
        }
        
        //开启调试模式
        if(!"".equals(userId) && "1".equals(sendmailkey)){
            logBean.writeLog("发送邮件debug日志开启，发件人id为："+userId);
            _session.setDebug(true);
        }

        return _session;
    }

    /**
     * 发送带附件的邮件
     *
     * @param from  邮件的发件人参数
     * @param to    邮件的收件人参数，多个收件人之间用逗号隔开
     * @param cc    邮件的抄送人参数，多个抄送人之间用逗号隔开
     * @param bcc   邮件的暗送人参数，多个暗送人之间用逗号隔开
     * @param subject    邮件的主题参数（采用ISO8859编码格式）
     * @param body       邮件的正文参数（采用ISO8859编码格式）
     * @param char_set   编码方式 1：iso-8859-1 2：big5 3：GBK
     * @param filenames   所有附件的名称，用ArrayList存放，与内容一一对应
     * @param filecontents   所有附件内容的输入流InputStream ，用ArrayList存放，与名称一一对应
     * @param priority   邮件的重要性参数 3：普通 2：重要 4：紧急
     *
     * @return boolean 邮件发送成功，返回 true，否则，返回false
     */
    public boolean sendMiltipartHtml(String from, String to, String cc, String bcc, String subject, String body, int char_set, ArrayList filenames, ArrayList filecontents, String priority) {
        Session _session = null;

        Properties props = new Properties();
        props.put("mail.smtp.from", from);
        props.put("mail.from", from);

        _session = setSSLConnectMsg(_session, props);
        MimeMessage msg = new MimeMessage(_session);
        try {
            msg.setFrom(new InternetAddress(from, this.accountName));
            msg.setRecipients(RecipientType.TO, InternetAddress.parse(to, true));
            if (cc != null) {
                msg.setRecipients(RecipientType.CC, InternetAddress.parse(cc, true));
            }
            if (bcc != null) {
                msg.setRecipients(RecipientType.BCC, InternetAddress.parse(bcc, true));
            }
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());
            msg.setHeader("X-Mailer", "weaver");
            if (null != this.needReceipt && "1".equals(this.needReceipt)) {// 需要回执
                msg.setHeader("Disposition-Notification-To", from); // from为发送者邮件地址
            }
            if (priority != null) {
                msg.setHeader("X-Priority", priority);
            }
 
            String charset = null;
            switch (char_set) {
                case 1: {
                    charset = "iso-8859-1";
                    break;
                }
                case 2: {
                    charset = "big5";
                    break;
                }
                case 3: {
                    charset = "UTF-8";
                    break;
                }
                default: {
                    charset = "UTF-8";
                    break;
                }
            }
            
            this.processBodyImg(body);
            int tmppos = body.indexOf(IMAGE_FLAG);
            ArrayList list = new ArrayList();
            while (tmppos != -1) {
                tmppos = body.indexOf("src=\"", tmppos + 20);
                int startpos = body.indexOf("\"", tmppos);
                int endpos = body.indexOf("\"", startpos + 1);
                String tempStr = body.substring(startpos + 1, endpos);

                String replaceStr = "cid:img" + list.size() + "@www.weaver.com.cn";
                body = Util.StringReplace(body, tempStr, replaceStr);

                tmppos = body.indexOf(IMAGE_FLAG, startpos + replaceStr.length());
                list.add(tempStr);
            }
            this.processBodyImg(body);
            
            // 新建一个MimeMultipart对象用来存放BodyPart对象(事实上可以存放多个)
            MimeMultipart mm = new MimeMultipart();
            // 新建一个存放信件内容的BodyPart对象
            BodyPart mdp = new MimeBodyPart();
            // 给BodyPart对象设置内容和格式/编码方式
            mdp.setContent(body, "text/html;  charset=\"" + charset + "\"");
            mdp.addHeader("Content-Transfer-Encoding", "base64");
            org.apache.ws.commons.util.Base64 enc = new org.apache.ws.commons.util.Base64();
            body = enc.encode(body.getBytes());
            //这句很重要
            mm.setSubType("mixed");
            mm.addBodyPart(mdp);
            
            for (int j = 0; j < list.size(); j++) {
                // 新建一个存放附件的BodyPart
                mdp = new MimeBodyPart();
                mdp.setDataHandler(new DataHandler(new FileDataSource((String) list.get(j))));
                // mdp.setFileName(j+".jpg");
                mdp.setHeader("Content-ID", "<img" + j + "@www.weaver.com.cn>");
                //将含有附件的BodyPart加入到MimeMultipart对象中 
                mm.addBodyPart(mdp);
            }

            int bodyfilesize = filenames.size();
            for (int i = 0; i < bodyfilesize; i++) {
                mdp = new MimeBodyPart();
                InputStream is = (InputStream) filecontents.get(i);
                String filename = new String(((String) filenames.get(i)).getBytes("UTF-8"), "ISO8859_1");
                String ctype = FileTypeMap.getDefaultFileTypeMap().getContentType(filename.toLowerCase());

                mdp.setDataHandler(new DataHandler(new ByteArrayDataSource(is, ctype)));
                mdp.setFileName(filename);
                mm.addBodyPart(mdp);
            }

            msg.setContent(mm);
            msg.saveChanges();

            Transport.send(msg);
            
            for (int i = 0; i < bodyfilesize; i++) {
                InputStream is = (InputStream) filecontents.get(i);
                try {
                    is.close();
                } catch (Exception ec) {
                    logBean.writeLog(ec);
                }
            }

            return true;
        } catch (Exception e) {
            logBean.writeLog("method:sendMiltipartHtml, from=" + from + ",subject=" + subject + ",to=" + to);
            logBean.writeLog(e);
            return false;
        }
    }

    /**
     * 发送带附件的邮件,HTML内容中包含图片
     *
     * @param from  邮件的发件人参数
     * @param to    邮件的收件人参数，多个收件人之间用逗号隔开
     * @param cc    邮件的抄送人参数，多个抄送人之间用逗号隔开
     * @param bcc   邮件的暗送人参数，多个暗送人之间用逗号隔开
     * @param subject    邮件的主题参数（采用ISO8859编码格式）
     * @param body       邮件的正文参数（采用ISO8859编码格式）
     * @param char_set   编码方式 1：iso-8859-1 2：big5 3：GBK
     * @param filenames   所有附件的名称，用ArrayList存放，与内容一一对应
     * @param filecontents   所有附件内容的输入流InputStream ，用ArrayList存放，与名称一一对应
     * @param priority   邮件的重要性参数 3：普通 2：重要 4：紧急
     * @param imgnames   HTML内容中包含的图片
     *
     * @return boolean 邮件发送成功，返回 true，否则，返回false
     */
    public boolean sendMiltipartHtml(String from, String to, String cc, String bcc, String subject, String body, int char_set, ArrayList filenames, ArrayList filecontents, String priority, Hashtable imgnames, int mailid,Map map) {
        Session _session = null;
        RecordSet rs = new RecordSet();
        Properties props = new Properties();
        props.put("mail.smtp.from", from);
        props.put("mail.from", from);

        _session = setSSLConnectMsg(_session, props);
        MimeMessage msg = new MimeMessage(_session);
        try {
            msg.setFrom(new InternetAddress(from, this.accountName));
            if( !this.isSendApart ) {
                msg.setRecipients(RecipientType.TO, InternetAddress.parse(to, true));
                if (cc != null) {
                    msg.setRecipients(RecipientType.CC, InternetAddress.parse(cc, true));
                }
                if (bcc != null) {
                    msg.setRecipients(RecipientType.BCC, InternetAddress.parse(bcc, true));
                }
            }
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());
            msg.setHeader("X-Mailer", "weaver");
            if (null != this.needReceipt && "1".equals(this.needReceipt)) {// 需要回执
                msg.setHeader("Disposition-Notification-To", from); // from为发送者邮件地址
            }
            if (priority != null) {
                msg.setHeader("X-Priority", priority);
            }
 
            String charset = null;
            switch (char_set) {
                case 1: {
                    charset = "iso-8859-1";
                    break;
                }
                case 2: {
                    charset = "big5";
                    break;
                }
                case 3: {
                    charset = "UTF-8";
                    break;
                }
                default: {
                    charset = "UTF-8";
                    break;
                }
            }
            
            this.processBodyImg(body);

            int tmppos = body.indexOf(IMAGE_FLAG);
            ArrayList list = new ArrayList();
            String docimageNum = "0";
            int tmppos2 = 0;
            while (tmppos != -1) {
                tmppos2 = tmppos + IMAGE_FLAG.length();
                tmppos = body.indexOf("\"", tmppos2);
                docimageNum = body.substring(tmppos2, tmppos);
                tmppos = body.indexOf("src=", tmppos);
                int startpos = body.indexOf("\"", tmppos);
                int endpos = body.indexOf("\"", startpos + 1);
                String tempStr = body.substring(startpos + 1, endpos);
                String replaceStr = "cid:img" + docimageNum + "@www.weaver.com.cn";

                if (tempStr.indexOf("weaver.email.FileDownloadLocation") != -1 || tempStr.indexOf("weaver.file.FileDownload") != -1) {
                    tmppos = body.indexOf(IMAGE_FLAG, startpos + tempStr.length());
                    continue;
                }
                body = Util.StringReplace(body, tempStr, replaceStr);
                tmppos = body.indexOf(IMAGE_FLAG, startpos + replaceStr.length());
                list.add(docimageNum);
            }
            this.processBodyImg(body);
            
            // 将HTML内容中的图片路径替换为cid:格式后，更新邮件内容。
            if (mailid > 0 && !list.isEmpty()) {
                MailCommonUtils.updateMailContent(mailid, body);
            }

            //===========================================================================
            ArrayList list2 = new ArrayList();
            int groups;
            PatternMatcher matcher;
            PatternCompiler compiler;
            Pattern pattern;
            PatternMatcherInput input;
            MatchResult result;

            compiler = new Perl5Compiler();
            matcher = new Perl5Matcher();

            pattern = compiler.compile("<img.*?src=['\"\\s]?(/.*?weaver.email.FileDownloadLocation\\?fileid=(\\d*)).*?>", Perl5Compiler.CASE_INSENSITIVE_MASK);

            input = new PatternMatcherInput(body);
            int n = list.size();
            String mailResourceFileId = "";
            String mailResourceFileRealPath = "";
            while (matcher.contains(input, pattern)) {
                result = matcher.getMatch();
                body = Util.StringReplace(body, result.group(1), "cid:img" + n + "@www.weaver.com.cn");
                list2.add(result.group(2));
                n++;
            }

            ArrayList list3 = new ArrayList();
            input = new PatternMatcherInput(body);
            pattern = compiler.compile("<img.*?src=['\"\\s]?(/.*?weaver.file.FileDownload\\?fileid=(\\d*)).*?>", Perl5Compiler.CASE_INSENSITIVE_MASK);
            while (matcher.contains(input, pattern)) {
                result = matcher.getMatch();
                body = Util.StringReplace(body, result.group(1), "cid:img" + n + "@www.weaver.com.cn");
                list3.add(result.group(2));
                n++;
            }
            //===========================================================================

            // 新建一个MimeMultipart对象用来存放BodyPart对象(事实上可以存放多个)
            MimeMultipart mm = new MimeMultipart();
            // 新建一个存放信件内容的BodyPart对象
            BodyPart mdp = new MimeBodyPart();
            // 给BodyPart对象设置内容和格式/编码方式
            mdp.setContent(body, "text/html;  charset=\"" + charset + "\"");
            mdp.addHeader("Content-Transfer-Encoding", "base64");
            org.apache.ws.commons.util.Base64 enc = new org.apache.ws.commons.util.Base64();
            body = enc.encode(body.getBytes());
            //这句很重要
            mm.setSubType("mixed");
            mm.addBodyPart(mdp);
            
            for (int j = 0; j < list.size(); j++) {
                try {
                    String imgfilerealpath = (String) imgnames.get(list.get(j).toString());
                    String imgfilename = imgfilerealpath.substring(imgfilerealpath.lastIndexOf("\\") + 1);
                    // System.out.println("附件！！！"+imgfilename);
                    String imageFileid = (String) list.get(j);
                    // 新建一个存放附件的BodyPart
                    mdp = new MimeBodyPart();
                    mdp.setDataHandler(new DataHandler(new FileDataSource(imageFileid)));
                    // mdp.setFileName(j+".jpg");
                    mdp.setHeader("Content-Type", "image/gif");
                    mdp.setHeader("Content-ID", "<img" + list.get(j) + "@www.weaver.com.cn>");
                    //将含有附件的BodyPart加入到MimeMultipart对象中 
                    mm.addBodyPart(mdp);

                    String imgsql = "INSERT INTO MailResourceFile (mailid,filename,attachfile,filetype,filerealpath,iszip,isencrypt,isfileattrachment,fileContentId,isEncoded,filesize) VALUES ("+mailid+",'"+imgfilename+"',null,'image/gif','"+imgfilerealpath+"','0','0','0','img"+list.get(j)+"@www.weaver.com.cn','0',0)";
                    rs.executeSql(imgsql);
                } catch (NullPointerException nulle) {
                    logBean.writeLog(nulle);
                }
            }

            int bodyfilesize = filenames.size();
            /*
            for (int i = 0; i < bodyfilesize; i++) {
                mdp = new MimeBodyPart();
                InputStream is = (InputStream) filecontents.get(i);
                String filename = new String(((String)filenames.get(i)).getBytes("UTF-8"), "ISO8859_1")  ;
                String ctype = FileTypeMap.getDefaultFileTypeMap().getContentType(filename.toLowerCase());

                mdp.setDataHandler(new DataHandler(new ByteArrayDataSource(is, ctype)));
                mdp.setFileName(filename);
                mm.addBodyPart(mdp);
            }
            */
            
            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                mdp = new MimeBodyPart();
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                DataHandler value = (DataHandler) entry.getValue();
                mdp.setDataHandler(value);
                mdp.setFileName(key);
                mm.addBodyPart(mdp);
            }
            n = list.size();
            
            for (int j = 0; j < list2.size(); j++) {
                mailResourceFileId = (String) list2.get(j);
                rs.executeSql("SELECT isaesencrypt,aescode,filerealpath FROM MailResourceFile WHERE id=" + mailResourceFileId + "");
                rs.next();
                mailResourceFileRealPath = rs.getString("filerealpath");
                String isaesencrypt = rs.getString("isaesencrypt");
                String aescode = rs.getString("aescode");

                mdp = new MimeBodyPart();
                mdp.setDataHandler(new DataHandler(new FileDataSource(mailResourceFileRealPath, isaesencrypt, aescode)));
                mdp.setHeader("Content-Type", "image/gif");
                mdp.setHeader("Content-ID", "<img" + (n++) + "@www.weaver.com.cn>");
                mm.addBodyPart(mdp);
            }

            for (int j = 0; j < list3.size(); j++) {
                mailResourceFileId = (String) list3.get(j);
                mdp = new MimeBodyPart();
                mdp.setDataHandler(new DataHandler(new FileDataSource(mailResourceFileId)));
                mdp.setHeader("Content-Type", "image/gif");
                mdp.setHeader("Content-ID", "<img" + (n++) + "@www.weaver.com.cn>");
                mm.addBodyPart(mdp);
            }

            msg.setContent(mm);
            
            if(this.isSendApart) {
                new Thread(new MailSendApartRunable(mailid, msg, to)).start();
            } else {
                msg.saveChanges();
                Transport.send(msg);
            }
            //System.out.println("原邮件的messageid：");
            //System.out.println(msg.getMessageID());
            
            /*
            for (int i = 0; i < bodyfilesize; i++) {
                InputStream is = (InputStream) filecontents.get(i);
               try {
                    is.close();
                } catch (Exception ec) {
                    logBean.writeLog(ec);
                }
            }
            */
            
            return true;
        } catch (Exception e) {
            logBean.writeLog("method:sendMiltipartHtml, from=" + from + ",subject=" + subject + ",to=" + to + ",mailid=" + mailid);
            logBean.writeLog(e);
            MailErrorMessageInfo merinfo =  new MailErrorFormat(e).getMailErrorMessageInfo();
            setErrorMess(merinfo.toString());
            return false;
        }
    }

    /**
     * 发送带附件的邮件
     *
     * @param from  邮件的发件人参数
     * @param to    邮件的收件人参数，多个收件人之间用逗号隔开
     * @param cc    邮件的抄送人参数，多个抄送人之间用逗号隔开
     * @param bcc   邮件的暗送人参数，多个暗送人之间用逗号隔开
     * @param subject    邮件的主题参数（采用ISO8859编码格式）
     * @param body       邮件的正文参数（采用ISO8859编码格式）
     * @param filenames   所有附件的名称，用ArrayList存放，与内容一一对应
     * @param filecontents   所有附件内容的输入流InputStream ，用ArrayList存放，与名称一一对应
     * @param priority   邮件的重要性参数 3：普通 2：重要 4：紧急
     *
     * @return boolean 邮件发送成功，返回 true，否则，返回false
     */
    public boolean sendMiltipartText(String from, String to, String cc, String bcc, String subject, String body, ArrayList filenames, ArrayList filecontents, String priority,Map map) {
        Session _session = null;

        Properties props = new Properties();
        props.put("mail.smtp.from", from);
        props.put("mail.from", from);

        _session = setSSLConnectMsg(_session, props);
        MimeMessage msg = new MimeMessage(_session);
        try {
            msg.setFrom(new InternetAddress(from, this.accountName));
            if( !this.isSendApart ) {
                msg.setRecipients(RecipientType.TO, InternetAddress.parse(to, true));
                if (cc != null) {
                    msg.setRecipients(RecipientType.CC, InternetAddress.parse(cc, true));
                }
                if (bcc != null) {
                    msg.setRecipients(RecipientType.BCC, InternetAddress.parse(bcc, true));
                }
            }

            int bodyfilesize = filenames.size();

            MimeBodyPart mbp[] = new MimeBodyPart[bodyfilesize + 1];
            for (int i = 0; i < bodyfilesize + 1; i++) {
                mbp[i] = new MimeBodyPart();
            }

            Multipart mp = new MimeMultipart();

            mbp[0].setText(body);
            mp.addBodyPart(mbp[0]);

            /*
            for (int i = 0; i < bodyfilesize; i++) {
                InputStream is = (InputStream) filecontents.get(i);
                String filename = new String(((String)filenames.get(i)).getBytes("UTF-8"), "ISO8859_1")  ;
                String ctype = FileTypeMap.getDefaultFileTypeMap().getContentType(filename.toLowerCase());

                mbp[i + 1].setDataHandler(new DataHandler(new ByteArrayDataSource(is, ctype)));
                mbp[i + 1].setFileName(filename);
                mp.addBodyPart(mbp[i + 1]);
            }
            */
            
            Iterator it = map.entrySet().iterator();
            ArrayList dateList1 = new ArrayList();
            ArrayList dateList2 = new ArrayList();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                DataHandler value = (DataHandler) entry.getValue();
                dateList1.add(key);
                dateList2.add(value);
            }
            for (int i = 0; i < dateList1.size(); i++) {
                String key = (String) dateList1.get(i);
                DataHandler value = (DataHandler) dateList2.get(i);
                mbp[i + 1].setDataHandler(value);
                mbp[i + 1].setFileName(key);
                mp.addBodyPart(mbp[i + 1]);
            }

            msg.setContent(mp);
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());
            msg.setHeader("X-Mailer", "weaver");
            if (null != this.needReceipt && "1".equals(this.needReceipt)) {// 需要回执
                msg.setHeader("Disposition-Notification-To", from); // from为发送者邮件地址
            }
            if (priority != null) {
                msg.setHeader("X-Priority", priority);
            }

            if(this.isSendApart) {
                new Thread(new MailSendApartRunable(this.bindMailId, msg, to)).start();
            } else {
                Transport.send(msg);
            }

            /*
            for (int i = 0; i < bodyfilesize; i++) {
                InputStream is = (InputStream) filecontents.get(i);
                try {
                    is.close();
                } catch (Exception ec) {
                    logBean.writeLog(ec);
                }
            }
            */
            return true;
        } catch (Exception e) {
            logBean.writeLog("method:sendMiltipartText, from=" + from + ",subject=" + subject + ",to=" + to);
            logBean.writeLog(e);
            MailErrorMessageInfo merinfo =  new MailErrorFormat(e).getMailErrorMessageInfo();
            setErrorMess(merinfo.toString());
            return false;
        }
    }

    
    

  /**
     * 发送带附件的邮件
     *
     * @param from  邮件的发件人参数
     * @param to    邮件的收件人参数，多个收件人之间用逗号隔开
     * @param subject    邮件的主题参数（采用ISO8859编码格式）
     * @param body       邮件的正文参数（采用ISO8859编码格式）
     * @param filePathList   所有附件路径
     *
     * @return boolean 邮件发送成功，返回 true，否则，返回false
     */
    public boolean sendFileMail(String from, String to,  String subject, String body, ArrayList<String> filePathList) {
        
        Map file = getFileContentByPath(filePathList);
        ArrayList filenames = (ArrayList)file.get("fileNameList");
        Map map = (Map)file.get("fileMap");
        Session _session = null;

        Properties props = new Properties();
        props.put("mail.smtp.from", from);
        props.put("mail.from", from);

        _session = setSSLConnectMsg(_session, props);
        MimeMessage msg = new MimeMessage(_session);
        try {
            msg.setFrom(new InternetAddress(from, this.accountName));
            msg.setRecipients(RecipientType.TO, InternetAddress.parse(to, true));
            int bodyfilesize = filenames.size();
            MimeBodyPart mbp[] = new MimeBodyPart[bodyfilesize + 1];
            for (int i = 0; i < bodyfilesize + 1; i++) {
                mbp[i] = new MimeBodyPart();
            }
            Multipart mp = new MimeMultipart();

            mbp[0].setText(body);
            mp.addBodyPart(mbp[0]);

            Iterator it = map.entrySet().iterator();
            ArrayList dateList1 = new ArrayList();
            ArrayList dateList2 = new ArrayList();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                DataHandler value = (DataHandler) entry.getValue();
                dateList1.add(key);
                dateList2.add(value);
            }
            for (int i = 0; i < dateList1.size(); i++) {
                String key = (String) dateList1.get(i);
                DataHandler value = (DataHandler) dateList2.get(i);
                mbp[i + 1].setDataHandler(value);
                mbp[i + 1].setFileName(key);
                mp.addBodyPart(mbp[i + 1]);
            }

            msg.setContent(mp);
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());
            msg.setHeader("X-Mailer", "weaver");
            if (null != this.needReceipt && "1".equals(this.needReceipt)) {// 需要回执
                msg.setHeader("Disposition-Notification-To", from); // from为发送者邮件地址
            }
                msg.setHeader("X-Priority", "3");

            Transport.send(msg);

            return true;
        } catch (Exception e) {
            logBean.writeLog("method:sendMiltipartText, from=" + from + ",subject=" + subject + ",to=" + to);
            logBean.writeLog(e);
            return false;
        }
    }

    /**
     * 获得附件流对象
     * @param filePathList  附件路径
     * @return
     */
    private Map<String, Object> getFileContentByPath(ArrayList<String> filePathList) {
        
        ArrayList fileContentList = new ArrayList();
        ArrayList fileNameList = new ArrayList();
        Map fileMap = new HashMap();
        Map map = new HashMap();
        String fileName = "";
        String filePath = "";
        // 获取正确的附件留信息
        try {
            for(int i =0;i<filePathList.size();i++) {
                InputStream source = null;
                filePath = filePathList.get(i);
                File thefile = new File(filePath);
                source = new BufferedInputStream(new FileInputStream(thefile));
                fileName = thefile.getName();
                fileContentList.add(source);
                fileNameList.add(fileName);
            }
            for (int i = 0, bodyfilesize = fileNameList.size(); i < bodyfilesize; i++) {
                InputStream is = (InputStream) fileContentList.get(i);
                String filename = (String) fileNameList.get(i);
                String ctype = FileTypeMap.getDefaultFileTypeMap().getContentType(filename);
                sun.misc.BASE64Encoder enc = new sun.misc.BASE64Encoder();
                filename = "=?UTF-8?B?" + enc.encode(filename.getBytes("UTF-8")) + "?=";
                filename = filename.replace("\n", ""); // 当文件名过长时
                StringBuffer sb = new StringBuffer(filename.length()); // 需要
                for (int j = 0; j < filename.length(); j++) { // 过滤换行和无用符号
                    if (filename.getBytes()[j] == 13) {
                        continue;
                    }
                    sb.append(filename.charAt(j));
                }
                DataHandler da = new DataHandler(new ByteArrayDataSource(is, ctype));
                fileMap.put(sb.toString(), da);
            }
            map.put("fileNameList", fileNameList);
            map.put("fileMap", fileMap);
        } catch (Exception e) {
            writeLog("getFileContentByPath 获取附件留信息错误!");
            writeLog(e);
        }
        return map;
    }


    public boolean saveDraft(String body, Hashtable imgnames, int mailid) {
        RecordSet rs = new RecordSet();
        try {
            this.processBodyImg(body);
            int tmppos = body.indexOf(IMAGE_FLAG);
            ArrayList list = new ArrayList();
            String docimageNum = "0";
            int tmppos2 = 0;
            //int i=0;
            while (tmppos != -1) {
                tmppos2 = tmppos;
                tmppos = body.indexOf("src=\"",tmppos+20);
                //TD5450
                docimageNum = body.substring(tmppos2+19, tmppos-1);
                docimageNum = docimageNum.substring(1,docimageNum.indexOf('"'));

                int startpos = body.indexOf("\"", tmppos);
                int endpos = body.indexOf("\"", startpos + 1);
                String tempStr = body.substring(startpos + 1, endpos);
                String replaceStr = "cid:img"+docimageNum+"@www.weaver.com.cn";

                if(tempStr.indexOf("weaver.email.FileDownloadLocation")!=-1){
                    tmppos = body.indexOf(IMAGE_FLAG, startpos+tempStr.length());
                    continue;
                }
                body = Util.StringReplace(body, tempStr,replaceStr);
                tmppos = body.indexOf(IMAGE_FLAG, startpos+replaceStr.length());
                list.add(docimageNum);
                //i++;
            }
            this.processBodyImg(body);
            
            for(int j=0;j<list.size();j++){
                try{
                    String imgfilerealpath = (String)imgnames.get(list.get(j).toString());
                    String imgfilename = imgfilerealpath.substring(imgfilerealpath.lastIndexOf("\\")+1);

                    String imgsql = "INSERT INTO MailResourceFile (mailid,filename,attachfile,filetype,filerealpath,iszip,isencrypt,isfileattrachment,fileContentId,isEncoded,filesize) VALUES ("+mailid+",'"+imgfilename+"',null,'image/gif','"+imgfilerealpath+"','0','0','0','img"+list.get(j)+"@www.weaver.com.cn','0',0)";
                    rs.executeSql(imgsql);
                }catch(NullPointerException nulle){
                    logBean.writeLog(nulle);
                }
            }

            //将HTML内容中的图片路径替换为cid:格式后，更新邮件内容。
            if(mailid>0){
                rs.executeSql("UPDATE MailResource SET hasHtmlImage='1',content='"+body+"' WHERE id="+mailid+"");
                if(rs.getDBType().equals("oracle")){
                    rs.executeSql("UPDATE MailContent SET mailcontent='"+body+"' WHERE mailid="+mailid+"");
            }
            }
            return true;
        } catch (Exception e) {
            logBean.writeLog(e);
            return false;
        }
    }

    private List processBodyImg(String body){
        if(this.isDebug){
//          System.out.println("Body===============================");
//          System.out.println(body);
        }
        return null;
    }
    
    public boolean sendhtmlICS(String from, String to, String cc, String bcc, String subject, String body, int char_set, String priority) throws Exception {
        Session _session = null;
        if (from == null || "".equals(from)) {
            return false;
        }
        Properties props = new Properties();
        props.put("mail.smtp.from", from);
        props.put("mail.from", from);

        _session = setSSLConnectMsg(_session, props);
        MimeMessage msg = new MimeMessage(_session);
        msg.setFrom(new InternetAddress(from, this.accountName));
        msg.setRecipients(RecipientType.TO, InternetAddress.parse(to));
        if (cc != null) {
            msg.setRecipients(RecipientType.CC, InternetAddress.parse(cc, true));
        }
        if (bcc != null) {
            msg.setRecipients(RecipientType.BCC, InternetAddress.parse(bcc, true));
        }
        msg.setSubject(subject, "UTF-8");
        msg.setSentDate(new Date());
        msg.setHeader("X-Mailer", "weaver");
        if (null != this.needReceipt && "1".equals(this.needReceipt)) {// 需要回执
            msg.setHeader("Disposition-Notification-To", from); // from为发送者邮件地址
        }
        if (priority != null) {
            msg.setHeader("X-Priority", priority);
        }

        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setDataHandler(new DataHandler(new javax.mail.util.ByteArrayDataSource(body,
                "text/calendar;method=REQUEST;charset=\"UTF-8\"")));
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        msg.setContent(multipart);

        msg.saveChanges();

        Transport.send(msg);
        return true;
    }

    //iCalender ICS
    public boolean sendhtmlICS(String from, String to, String cc, String bcc, String subject, String body, int char_set, String priority, String description) throws Exception {
        logBean.writeLog("sendMailIcs发件箱："+from);
        Session _session = null;
        if (from == null || "".equals(from)) {
            return false;
        }
        Properties props = new Properties();
        props.put("mail.smtp.from", from);
        props.put("mail.from", from);

        _session = setSSLConnectMsg(_session, props);
        MimeMessage msg = new MimeMessage(_session);
        msg.setFrom(new InternetAddress(from, this.accountName));
        msg.setRecipients(RecipientType.TO, InternetAddress.parse(to));
        if (cc != null) {
            msg.setRecipients(RecipientType.CC, InternetAddress.parse(cc, true));
        }
        if (bcc != null) {
            msg.setRecipients(RecipientType.BCC, InternetAddress.parse(bcc, true));
        }
        msg.setSubject(subject, "UTF-8");
        msg.setSentDate(new Date());
        msg.setHeader("X-Mailer", "weaver");
        if (null != this.needReceipt && "1".equals(this.needReceipt)) {// 需要回执
            msg.setHeader("Disposition-Notification-To", from); // from为发送者邮件地址
        }
        if (priority != null) {
            msg.setHeader("X-Priority", priority);
        }
        
        MimeMultipart multipart = new MimeMultipart();
        multipart.setSubType("alternative");
        
        //富文本内容
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart = new MimeBodyPart();
        String htmlBody = description;
        messageBodyPart.setDataHandler(new DataHandler(new javax.mail.util.ByteArrayDataSource(htmlBody, "text/html;charset=\"UTF-8\"")));
        multipart.addBodyPart(messageBodyPart);
        
        // 会议格式部分
        messageBodyPart = new MimeBodyPart();
        messageBodyPart.setDataHandler(new DataHandler(new javax.mail.util.ByteArrayDataSource(body, "text/calendar;method=REQUEST;charset=UTF-8")));
        multipart.addBodyPart(messageBodyPart);
        msg.setContent(multipart);
        
        msg.saveChanges();

        Transport.send(msg);
        return true;
    }

}




