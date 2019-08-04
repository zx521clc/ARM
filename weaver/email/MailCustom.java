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
        //�����û�����֤��ʽ
        //props.setProperty("mail.smtp.auth", "true");
        //���ô���Э��
        //props.setProperty("mail.transport.protocol", "smtp");
        //���÷����˵�SMTP��������ַ
        //props.setProperty("mail.smtp.host", DEFMAILSERVER);
        //2��������������Ӧ�ó�������Ļ�����Ϣ�� Session ����
        //props.setProperty("mail.debug", "true");  //false
        // ���ͷ�������Ҫ�����֤
        props.setProperty("mail.smtp.auth", "true");
        // �����ʼ�������������
        props.setProperty("mail.smtp.host", "smtp.partner.outlook.cn");
        // �����ʼ�Э������
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.port", "587");
        props.setProperty("mail.smtp.starttls.enable", "true");
        //props.setProperty("username",DEFMAILUSER);
        //props.setProperty("password",DEFMAILPASSWORD);
        Session session = Session.getInstance(props);
        //���õ�����Ϣ�ڿ���̨��ӡ����
        session.setDebug(true);

        //3�������ʼ���ʵ������
        writeLog("sendMailbegin");
        //writeLog(to[0]);
        Message msg = createMessage(session,EMAIL_FROM,to,cc,subject,mailType,params);
        writeLog("send");
        //4������session�����ȡ�ʼ��������Transport
        Transport transport = session.getTransport();
        //���÷����˵��˻���������
        transport.connect(DEFMAILSERVER,DEFMAILUSER,DEFMAILPASSWORD);
        //transport.connect();
        //�����ʼ��������͵������ռ��˵�ַ��message.getAllRecipients() ��ȡ�������ڴ����ʼ�����ʱ��ӵ������ռ���, ������, ������
        transport.sendMessage(msg,msg.getAllRecipients());
        //5���ر��ʼ�����
        transport.close();
    }

    private MimeMessage createMessage(Session session,String from,String[] to,String[] cc,String subject,String mailType,Map<String,String> params) throws Exception {
        MimeMessage message = new MimeMessage(session);
        //ָ���ʼ��ķ�����
        message.setFrom(new InternetAddress(from));
        //ָ���ʼ����ռ��ˣ����ڷ����˺��ռ�����һ���ģ��Ǿ����Լ����Լ���
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
        //�ʼ��ı���
        writeLog("subject:"+subject);
        message.setSubject(subject);
        //�ʼ����ı�����
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
                    // ��ȡ�����ļ�
                    DataHandler dh = new DataHandler(new FileDataSource(DIR+filename));
                    // ��ͼƬ������ӵ�"�ڵ�"
                    image.setDataHandler(dh);
                    // Ϊ"�ڵ�"����һ��Ψһ��ţ����ı�"�ڵ�"�����ø�ID��
                    image.setHeader("Content-ID","<"+filename+">");
                    //mm_text_image.addBodyPart(image);
                    params.put("src","cid:"+filename);
                }else if(key.contains("attach")){
                    String filename=field;
                    MimeBodyPart attachment = new MimeBodyPart();
                    String path=DIR+filename;
                    writeLog("path1:"+path);
                    DataSource ds1 = new FileDataSource(new File(path));
                    //���ݴ�����
                    DataHandler dh1 = new DataHandler(ds1);
                    //���õ�һ������������
                    attachment.setDataHandler(dh1);
                    //���õ�һ���������ļ���
                    attachment.setFileName(filename);

                    // ��ȡ�����ļ�
                    //DataHandler dh2 = new DataHandler(new javax.activation.FileDataSource(new File(path)));
                    // ������������ӵ�"�ڵ�"
                    //attachment.setDataHandler(dh2);
                    // ���ø������ļ�������Ҫ���룩
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
                //mm_text_image.setSubType("related");    // ������ϵ
//
//        // 8. �� �ı�+ͼƬ �Ļ��"�ڵ�"��װ��һ����ͨ"�ڵ�"
//        // ������ӵ��ʼ��� Content ���ɶ�� BodyPart ��ɵ� Multipart, ����������Ҫ���� BodyPart,
//        // ����� mailTestPic ���� BodyPart, ����Ҫ�� mm_text_image ��װ��һ�� BodyPart
                //MimeBodyPart text_image = new MimeBodyPart();
                //text_image.setContent(mm_text_image);
//       // message.setContent(content, "text/html;charset=UTF-8");
//        //���ش����õ��ʼ�����
//
               // mm.addBodyPart(text_image);
                //mm.setSubType("mixed");
                message.setContent(mm_text_image);
            }else if(mailType.equalsIgnoreCase("template4")){
                mm.addBodyPart(text);
                //mm.setSubType("mixed");
                message.setContent(mm);
            }
//             // ����ж�����������Դ������������
                   // ��Ϲ�ϵ

            return message;
        }
        //return message;

    }

    private String getMailContent(String mailType, Map<String,String> params) {
        StringWriter stringWriter = new StringWriter(); // velocity����
        VelocityEngine velocityEngine = new VelocityEngine(); // �����ļ�·������
        Properties properties = new Properties();
        //String dir = SendMailSenderServiceImpl.class.getResource("/").getPath();
        //String dir= getPropValue("CustomMail","tempLocation");
        writeLog(DIR);
        properties.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, DIR); // �����ʼ����������
        try {
            velocityEngine.init(properties); // ����ָ��ģ��
            String filename=getPropValue("CustomMail",mailType);
            Template template = velocityEngine.getTemplate(filename, "utf-8"); // ���ģ������
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
