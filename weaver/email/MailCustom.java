package weaver.email;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.soa.workflow.request.RequestInfo;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.*;
import java.io.File;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class MailCustom extends BaseBean{

    private static String EMAIL_FROM="";
    private static RecordSet RS=new RecordSet();
    private static String DEFMAILUSER="";
    private static String DEFMAILPASSWORD="";
    private static String DEFMAILSERVER="";
    private static RequestInfo INFO=null;
    private static String DIR="";
    public MailCustom(RequestInfo info){
        EMAIL_FROM=getPropValue("CustomMail","email_from");
        DIR= getPropValue("CustomMail","tempLocation");
        RS.executeQuery("select * from systemset");
        if(RS.next()){
            DEFMAILUSER=RS.getString("defmailuser");
            DEFMAILPASSWORD=RS.getString("defmailpassword");
            DEFMAILSERVER=RS.getString("defmailserver");
        }
        DEFMAILPASSWORD=EmailEncoder.DecoderPassword(DEFMAILPASSWORD);
        writeLog(EMAIL_FROM+";"+DIR+";"+DEFMAILPASSWORD+";"+DEFMAILSERVER+";"+DEFMAILUSER);
        INFO=info;
    }

    public void sendMail(String[] to,String[] cc,String subject,String mailType,Map<String,String> params) throws Exception{
        Properties props = new Properties();
        //设置用户的认证方式
        //props.setProperty("mail.smtp.auth", "true");
        //设置传输协议
        //props.setProperty("mail.transport.protocol", "smtp");
        //设置发件人的SMTP服务器地址
        //props.setProperty("mail.smtp.host", DEFMAILSERVER);
        //2、创建定义整个应用程序所需的环境信息的 Session 对象
        //props.setProperty("mail.debug", "true");  //false
        // 发送服务器需要身份验证
        props.setProperty("mail.smtp.auth", "true");
        // 设置邮件服务器主机名
        props.setProperty("mail.smtp.host", "smtp.partner.outlook.cn");
        // 发送邮件协议名称
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.port", "587");
        props.setProperty("mail.smtp.starttls.enable", "true");
        //props.setProperty("username",DEFMAILUSER);
        //props.setProperty("password",DEFMAILPASSWORD);
        Session session = Session.getInstance(props);
        //设置调试信息在控制台打印出来
        session.setDebug(true);

        //3、创建邮件的实例对象
        writeLog("sendMailbegin");
        //writeLog(to[0]);
        Message msg = createMessage(session,EMAIL_FROM,to,cc,subject,mailType,params);
        writeLog("send");
        //4、根据session对象获取邮件传输对象Transport
        Transport transport = session.getTransport();
        //设置发件人的账户名和密码
        transport.connect(DEFMAILSERVER,DEFMAILUSER,DEFMAILPASSWORD);
        //transport.connect();
        //发送邮件，并发送到所有收件人地址，message.getAllRecipients() 获取到的是在创建邮件对象时添加的所有收件人, 抄送人, 密送人
        transport.sendMessage(msg,msg.getAllRecipients());
        //5、关闭邮件连接
        transport.close();
    }

    private MimeMessage createMessage(Session session,String from,String[] to,String[] cc,String subject,String mailType,Map<String,String> params) throws Exception {
        MimeMessage message = new MimeMessage(session);
        //指明邮件的发件人
        message.setFrom(new InternetAddress(from));
        //指明邮件的收件人，现在发件人和收件人是一样的，那就是自己给自己发
        InternetAddress[] toAddresses=new InternetAddress[to.length];
        for(int i=0;i<to.length;i++){
            String to1=to[i];
            writeLog("to1:"+to1);
            if(!to1.equals("")){
                writeLog("to11:"+to1);
                InternetAddress ia=new InternetAddress(to1);
                toAddresses[i]=ia;
            }else {
                continue;
            }
        }
        message.setRecipients(Message.RecipientType.TO,toAddresses);
        InternetAddress[] ccAddresses=new InternetAddress[cc.length];
        for(int i=0;i<cc.length;i++){
            String cc1=cc[i];
            if(!cc1.equals("")){
                InternetAddress ia=new InternetAddress(cc1);
                ccAddresses[i]=ia;
            }else {
                continue;
            }
        }
        message.setRecipients(Message.RecipientType.CC, ccAddresses);
        //邮件的标题
        writeLog("subject:"+subject);
        message.setSubject(subject);
        //邮件的文本内容
        //Map<String,String> params=new HashMap<String,String>();
        //String tablename=getTablename(INFO);
        //String requestid=INFO.getRequestid();
        //String sql="select * from "+tablename+" where requestid='"+requestid+"'";
        //writeLog(sql);
        //RS.executeQuery(sql);
        //RS.next();
        String k_fs=getPropValue("CustomMail",mailType+"_kf");
        writeLog(mailType+"_kf:"+k_fs);
        MimeMultipart mm_text_image = new MimeMultipart("related");
        MimeMultipart mm = new MimeMultipart();
        MimeBodyPart image = new MimeBodyPart();
        if(!k_fs.trim().equalsIgnoreCase("")){
            String[] temps=k_fs.split(",");
            for(String temp:temps){
                String key=temp.substring(0,temp.indexOf(":"));
                String field=temp.substring(temp.indexOf(":")+1,temp.length());
                writeLog(key+";"+field);
                if(key.contains("src")){
                    String filename=field;
                    //MimeBodyPart image = new MimeBodyPart();
                    // 读取本地文件
                    DataHandler dh = new DataHandler(new FileDataSource(DIR+filename));
                    // 将图片数据添加到"节点"
                    image.setDataHandler(dh);
                    // 为"节点"设置一个唯一编号（在文本"节点"将引用该ID）
                    image.setHeader("Content-ID","<"+filename+">");
                    //mm_text_image.addBodyPart(image);
                    params.put("src","cid:"+filename);
                }else if(key.contains("attach")){
                    String filename=field;
                    MimeBodyPart attachment = new MimeBodyPart();
                    String path=DIR+filename;
                    writeLog("path1:"+path);
                    DataSource ds1 = new FileDataSource(new File(path));
                    //数据处理器
                    DataHandler dh1 = new DataHandler(ds1);
                    //设置第一个附件的数据
                    attachment.setDataHandler(dh1);
                    //设置第一个附件的文件名
                    attachment.setFileName(filename);

                    // 读取本地文件
                    //DataHandler dh2 = new DataHandler(new javax.activation.FileDataSource(new File(path)));
                    // 将附件数据添加到"节点"
                    //attachment.setDataHandler(dh2);
                    // 设置附件的文件名（需要编码）
                    //attachment.setFileName(MimeUtility.encodeText(dh2.getName()));
                    mm.addBodyPart(attachment);
                }
            }
        }
        //writeLog("params:"+params);
        String content=getMailContent(mailType,params);
        writeLog("content:"+content);
        if(mailType.equalsIgnoreCase("template1") || mailType.equalsIgnoreCase("template2") || mailType.equalsIgnoreCase("template5") || mailType.equalsIgnoreCase("template6")){
            //MimeBodyPart htmlPart = new MimeBodyPart();
            message.setContent(content, "text/html;charset=UTF-8");
           // mm.addBodyPart(htmlPart);
            //message.setContent(mm);
            writeLog(message.getContent().toString());
            //message.setText(content);
            return message;
        }else{

            MimeBodyPart text = new MimeBodyPart();
            text.setContent(content,"text/html;charset=UTF-8");
            if(mailType.equalsIgnoreCase("template3")){
                mm_text_image.addBodyPart(text);
                mm_text_image.addBodyPart(image);
                //mm_text_image.setSubType("related");    // 关联关系
//
//        // 8. 将 文本+图片 的混合"节点"封装成一个普通"节点"
//        // 最终添加到邮件的 Content 是由多个 BodyPart 组成的 Multipart, 所以我们需要的是 BodyPart,
//        // 上面的 mailTestPic 并非 BodyPart, 所有要把 mm_text_image 封装成一个 BodyPart
                //MimeBodyPart text_image = new MimeBodyPart();
                //text_image.setContent(mm_text_image);
//       // message.setContent(content, "text/html;charset=UTF-8");
//        //返回创建好的邮件对象
//
               // mm.addBodyPart(text_image);
                //mm.setSubType("mixed");
                message.setContent(mm_text_image);
            }else if(mailType.equalsIgnoreCase("template4")){
                mm.addBodyPart(text);
                //mm.setSubType("mixed");
                message.setContent(mm);
            }
//             // 如果有多个附件，可以创建多个多次添加
                   // 混合关系

            return message;
        }
        //return message;

    }

    private String getMailContent(String mailType, Map<String,String> params) {
        StringWriter stringWriter = new StringWriter(); // velocity引擎
        VelocityEngine velocityEngine = new VelocityEngine(); // 设置文件路径属性
        Properties properties = new Properties();
        //String dir = SendMailSenderServiceImpl.class.getResource("/").getPath();
        //String dir= getPropValue("CustomMail","tempLocation");
        writeLog(DIR);
        properties.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, DIR); // 引擎初始化属性配置
        try {
            velocityEngine.init(properties); // 加载指定模版
            String filename=getPropValue("CustomMail",mailType);
            Template template = velocityEngine.getTemplate(filename, "utf-8"); // 填充模板内容
            VelocityContext velocityContext = new VelocityContext();
            Iterator iter=params.entrySet().iterator();
            while(iter.hasNext()){
                Map.Entry<String,String> entry=(Map.Entry)iter.next();
                velocityContext.put(entry.getKey(),entry.getValue());
            }
            //velocityContext.put("ename", ename);
            //velocityContext.put("cname", cname);
            //velocityContext.put("place", place);
            //writeLog(velocityContext);
            template.merge(velocityContext, stringWriter);
            return stringWriter.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "fail";
        }

    }

    private String getTablename(RequestInfo requestInfo){
        String tablename=requestInfo.getRequestManager().getBillTableName();
        if(tablename.equals("")){
            String sql="select tablename from workflow_bill where id in (select formid from workflow_base where id in (select workflowid from workflow_requestbase where requestid="+requestInfo.getRequestid()+"))";
            RecordSet rs=new RecordSet();
            rs.executeQuery(sql);
            if (rs.next()){
                tablename= Util.null2String(rs.getString(1));
            }else{
                tablename="workflow_form";
            }
        }
        return tablename;
    }

}
