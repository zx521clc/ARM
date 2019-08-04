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

public class TransMailAction2 extends BaseBean implements Action {
    private RecordSet rs=new RecordSet();
    private RecordSet rs1=new RecordSet();
    @Override
    public String execute(RequestInfo requestInfo) {
        writeLog("==========start===========");
        try{
            MailCustom mc=new MailCustom(requestInfo);
            String requestId=requestInfo.getRequestid();
            String tableName=getTablename(requestInfo);
            String sql="select  * from "+tableName+" where requestid='"+requestId+"'";
            rs.executeQuery(sql);
            rs.next();
            Map<String,String> params=new HashMap<String,String>();
            String gsfx=rs.getString("gsfx");
            String newtitle=rs.getString("newtitle");
            String firstname=rs.getString("mpy");
            String familyName=rs.getString("xpy");
            String englishname=rs.getString("employeename");
            String onboardDay=rs.getString("effectivedate");
            String location =rs.getString("xbgdd");
            String lineManager=rs.getString("newlinemanager");
            String armcbzx=rs.getString("armcbzx");
            String birthday=rs.getString("birthday");
            String armid=rs.getString("armid");
            String lastday=rs.getString("lastday");
            String firstday=rs.getString("firstday");
            String kgsfxcbgsfsdyxdz=rs.getString("kgsfxcbgsfsdyxdz");
            String kgsfxgscbfsdyxdz=rs.getString("kgsfxgscbfsdyxdz");
            if(gsfx.equals("0")){//hire
                params.put("title",new JobTitlesComInfo().getJobTitlesname(newtitle));
                params.put("firstName",firstname);
                params.put("familyName",familyName);
                params.put("ename",new ResourceComInfo().getLastname(englishname));
                params.put("gender",new ResourceComInfo().getSexs(englishname).equals("0")?"Male":"Female");
                params.put("onboardDay",onboardDay);
                params.put("location",location);
                params.put("lineManager",lineManager);
                params.put("mobile",new ResourceComInfo().getMobile(englishname));
                params.put("costCode",armcbzx);
                params.put("birthday",birthday);
                sql="select field6 from cus_fielddata where scope='HrmCustomFieldByInfoType' and scopeid='1' and id='"+englishname+"'";
                rs1.executeQuery(sql);
                rs1.next();
                String nationality=rs1.getString(1);
                params.put("nationality",nationality);
                String[] to=kgsfxcbgsfsdyxdz.split(",");
                String[] cc={};
                mc.sendMail(to,cc,"JVGS New Hire","template2",params);
            }else if(gsfx.equals("1")){//fire
                params.put("name",new ResourceComInfo().getLastname(englishname));
                params.put("sfz",armid);
                params.put("lastworkingdate",lastday);
                params.put("terminationdate",firstday);
                String[] to=kgsfxgscbfsdyxdz.split(",");
                String[] cc={};
                mc.sendMail(to,cc,"JVGS New Hire","template6",params);
            }
            writeLog(params);
        }catch (Exception e){
            e.printStackTrace();
            return FAILURE_AND_CONTINUE;
        }
        return SUCCESS;
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
