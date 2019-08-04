package test;

import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;

public class test {
    public static void main (String[] args){
        //String result=getMailContent("test.html","王磊","wang lei");
        //System.out.println("ename:xm".substring("ename:xm".indexOf(":")+1,"ename:xm".length()));
        String abcd="#abcd#";
        abcd=abcd.replaceAll("#abcd#","dcba");
        System.out.println(abcd);
    }

    private static String getMailContent(String mailContent, String cname, String ename) {
        StringWriter stringWriter = new StringWriter(); // velocity引擎
        VelocityEngine velocityEngine = new VelocityEngine(); // 设置文件路径属性
        Properties properties = new Properties();
        //String dir = SendMailSenderServiceImpl.class.getResource("/").getPath();
        String dir=test.class.getResource("/").getPath();
        System.out.println(dir);
        properties.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, dir); // 引擎初始化属性配置
        try {
            velocityEngine.init(properties); // 加载指定模版
            Template template = velocityEngine.getTemplate(mailContent, "utf-8"); // 填充模板内容
            VelocityContext velocityContext = new VelocityContext();
            velocityContext.put("ename", ename);
            velocityContext.put("cname", cname);
            //velocityContext.put("place", place);
            template.merge(velocityContext, stringWriter);
            return stringWriter.toString();
        } catch (Exception e) {
            System.out.println("Get Mail Content failed.{}"+ e);
            return "fail";
        }

    }
}
