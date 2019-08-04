package weaver.interfaces.workflow.action;

import weaver.conn.RecordSet;
import weaver.email.MailCustom;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.excelimport.HrmJobTitle;
import weaver.hrm.job.JobTitlesComInfo;
import weaver.hrm.location.LocationComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.soa.workflow.request.RequestInfo;

import java.util.HashMap;
import java.util.Map;

public class MailActionLZ extends BaseBean implements Action {

    private String mailType="";

    public String getMailType() {
        return mailType;
    }

    public void setMailType(String mailType) {
        this.mailType = mailType;
    }

    private RecordSet rs=new RecordSet();
    private RecordSet rs1=new RecordSet();
    @Override
    public String execute(RequestInfo requestInfo) {
        String requestId=requestInfo.getRequestid();
        String tableName=getTablename(requestInfo);
        writeLog("mailType:"+mailType);
        try{
            String sql = "select * from " + tableName + " a,hrmresource b where a.employeename=b.id and a.requestid='" + requestId + "'";
            rs.executeQuery(sql);
            //String dzyj = "";
            String fbwgssfsdyxdz="";
            //String fbwcbsfsdyxdz="";
            //String lcjshfsdyxdz="";
            //String armchinadzyj="";
            //String armemail="";
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
            String sfzh="";
            String terminationdate="";
            String lastworkingdate="";
            //String dyry="";
            if (rs.next()) {
                //dzyj = rs.getString("dzyj");
                fbwgssfsdyxdz=rs.getString("fbwgssfsdyxdz");
                String resourceid=rs.getString("employeename");
                xm=new ResourceComInfo().getLastname(resourceid);
                zwm=rs.getString("zwm");
                gw=new JobTitlesComInfo().getJobTitlesname(rs.getString("jobtitle"));
                xb=rs.getString("gender");
                onboardDay=rs.getString("companystartdate");
                location=new LocationComInfo().getLocationname(rs.getString("location"));
                lineManager=new ResourceComInfo().getLastname(rs.getString("manager"));
                mobile=rs.getString("mobile");
                costCode=getValue("3","field11",resourceid);
                birthday=rs.getString("birthday");
                nationality=getValue("1","field0",resourceid);
                fb=rs.getString("subcompanyid1");
                sfzh=rs.getString("certificatenum");
                terminationdate=rs.getString("terminationdate");
                lastworkingdate=rs.getString("lastworkingdate");
                //fbwcbsfsdyxdz=rs.getString("fbwcbsfsdyxdz");
                //lcjshfsdyxdz=rs.getString("lcjshfsdyxdz");
                //armchinadzyj=rs.getString("armchinadzyj");
                //armemail=rs.getString("armemail");
                //dyry=rs.getString("dyry_new");
            }
            //writeLog("email:"+dzyj+";"+fbwgssfsdyxdz);
            writeLog("fb:"+fb);
            String[] types=mailType.split(",");
            for(String type:types) {
                writeLog(type);

                Map<String,String> params=new HashMap<String,String>();
                //String[] to={};
                String[] cc={};
                if(type.equalsIgnoreCase("template2")){
                    if(fb.trim().equalsIgnoreCase("8")){
                        MailCustom mc = new MailCustom(requestInfo);
                        String[] to=fbwgssfsdyxdz.split(",");
                        //String title=new JobTitlesComInfo().getJobTitlesname(gw);
                        params.put("title",gw);
                        String firstName=xm.substring(0,xm.indexOf(" "));
                        params.put("firstName",firstName);
                        String familyName=xm.substring(xm.indexOf(" ")+1,xm.length());
                        params.put("familyName",familyName);
                        params.put("ename",xm);
                        params.put("gender",xb.equals("0")?"Male":"Female");
                        params.put("onboardDay",onboardDay);
                        rs.executeQuery("select locationname from hrmlocations where id='"+location+"'");
                        rs.next();
                        params.put("location",rs.getString(1));
                        params.put("lineManager",new ResourceComInfo().getLastname(lineManager));
                        params.put("mobile",mobile);
                        params.put("costCode",costCode);
                        params.put("birthday",birthday);
                        params.put("nationality",nationality);
                        //params.put("name",new ResourceComInfo().getLastname(dyry));
                        mc.sendMail(to, cc, "Leave Noticify", type,params);
                    }
                }else if(type.equalsIgnoreCase("template6")){
                    if(fb.trim().equalsIgnoreCase("8")){
                        params.put("name",xm);
                        params.put("sfz",sfzh);
                        params.put("terminationdate",terminationdate);
                        params.put("lastworkingdate",lastworkingdate);
                        MailCustom mc = new MailCustom(requestInfo);
                        String[] to=fbwgssfsdyxdz.split(",");
                        mc.sendMail(to, cc, "Leave Noticify", type,params);
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

    private String getValue(String type,String field,String resourceid){
        String sql="select "+field+" from cus_fielddata where scope='HrmCustomFieldByInfoType' and scopeid='"+type+"' and id='"+resourceid+"'";
        writeLog("sql:"+sql);
        rs1.executeQuery(sql);
        rs1.next();
        return Util.null2String(rs1.getString(1));
    }
}
