package weaver.interfaces.workflow.action;

import weaver.conn.RecordSet;
import weaver.email.MailCustom;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.job.JobTitlesComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.soa.workflow.request.RequestInfo;

import java.util.HashMap;
import java.util.Map;

public class MailAction1 extends BaseBean implements Action {

    private String mailType="";

    public String getMailType() {
        return mailType;
    }

    public void setMailType(String mailType) {
        this.mailType = mailType;
    }

    private RecordSet rs=new RecordSet();
    @Override
    public String execute(RequestInfo requestInfo) {
        String requestId=requestInfo.getRequestid();
        String tableName=getTablename(requestInfo);
        writeLog("mailType:"+mailType);
        try{
            String sql = "select * from " + tableName + " where requestid='" + requestId + "'";
            rs.executeQuery(sql);
            String dzyj = "";
            String fbwgssfsdyxdz="";
            String fbwcbsfsdyxdz="";
            String lcjshfsdyxdz="";
            String armchinadzyj="";
            String armemail="";
            String xm="";
            String zwm="";
            String gw="";
            String xb="";
            String onboardDay="";
            String location="";
            String lineManager="";
            String mobile="";
            String costCode="";
            String birthday="";
            String nationality="";
            String fb="";
            String bh="";
            if (rs.next()) {
                dzyj = rs.getString("dzyj");
                fbwgssfsdyxdz=rs.getString("fbwgssfsdyxdz");
                xm=rs.getString("xm");
                zwm=rs.getString("zwm");
                gw=rs.getString("gwnew");
                xb=rs.getString("xb");
                onboardDay=rs.getString("onboarddate");
                location=rs.getString("bgdd");
                lineManager=rs.getString("zjsj");
                mobile=rs.getString("yddh");
                costCode=rs.getString("costcentercode");
                birthday=rs.getString("csrq");
                nationality=rs.getString("gj");
                fb=rs.getString("fb");
                fbwcbsfsdyxdz=rs.getString("fbwcbsfsdyxdz");
                lcjshfsdyxdz=rs.getString("lcjshfsdyxdz");
                armchinadzyj=rs.getString("armchinadzyj");
                armemail=rs.getString("armemail");
                bh=rs.getString("bh");
            }
            writeLog("email:"+dzyj+";"+fbwgssfsdyxdz);
            writeLog("fb:"+fb);
            String[] types=mailType.split(",");
            for(String type:types) {
                writeLog(type);

                Map<String,String> params=new HashMap<String,String>();
                //String[] to={};
                String[] cc={};
                if (type.equalsIgnoreCase("template1")) {
                    MailCustom mc = new MailCustom(requestInfo);
                    String[] to={dzyj};
                    params.put("cname",zwm);
                    params.put("ename",xm);
                    writeLog("to:"+to[0]);
                    mc.sendMail(to, cc, "Welcome to ARMChina", type,params);
                }else if(type.equalsIgnoreCase("template2")){
                    if(fb.trim().equalsIgnoreCase("8")){
                        MailCustom mc = new MailCustom(requestInfo);
                        String[] to=fbwgssfsdyxdz.split(",");
                        String title=new JobTitlesComInfo().getJobTitlesname(gw);
                        params.put("title",title);
                        String firstName=xm.substring(0,xm.indexOf(" "));
                        params.put("firstName",firstName);
                        String familyName=xm.substring(xm.indexOf(" ")+1,xm.length());
                        params.put("familyName",familyName);
                        params.put("ename",xm);
                        params.put("gender",xb.equals("1")?"Male":"Female");
                        params.put("onboardDay",onboardDay);
                        rs.executeQuery("select locationname from hrmlocations where id='"+location+"'");
                        rs.next();
                        params.put("location",rs.getString(1));
                        params.put("lineManager",new ResourceComInfo().getLastname(lineManager));
                        params.put("mobile",mobile);
                        params.put("costCode",costCode);
                        params.put("birthday",birthday);
                        params.put("nationality",nationality);
                        mc.sendMail(to, cc, "JVGS New Hires", type,params);
                    }
                }else if(type.equalsIgnoreCase("template3")){
                    if(fb.trim().equalsIgnoreCase("8")){
                        MailCustom mc = new MailCustom(requestInfo);
                        String str=armemail+","+fbwcbsfsdyxdz;
                        writeLog("temp3:"+str);
                        String[] to=str.split(",");
                        params.put("armemail",armchinadzyj);
                        mc.sendMail(to, cc, "Welcome to ARMChina", type,params);
                    }
                }else if(type.equalsIgnoreCase("template4")){
                    if(fb.trim().equalsIgnoreCase("6")){
                        MailCustom mc = new MailCustom(requestInfo);
                        String str=armchinadzyj+","+fbwcbsfsdyxdz+","+lcjshfsdyxdz;
                        writeLog("temp4:"+str);
                        String[] to=str.split(",");
                        params.put("ename",xm);
                        params.put("EEID",bh);
                        mc.sendMail(to, cc, "Welcome to ARMChina", type,params);
                    }
                }
                writeLog("params:"+params);

            }
            return SUCCESS;
        }catch(Exception e){
            e.printStackTrace();
            requestInfo.getRequestManager().setMessageid("0");
            requestInfo.getRequestManager().setMessagecontent(e.getMessage());
            return FAILURE_AND_CONTINUE;
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
