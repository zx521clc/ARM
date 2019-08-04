package weaver.interfaces.workflow.action;

import weaver.conn.RecordSet;
import weaver.email.MailCustom;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.resource.ResourceComInfo;
import weaver.soa.workflow.request.RequestInfo;

import java.util.HashMap;
import java.util.Map;

public class TransMailAction1 extends BaseBean implements Action {
    private RecordSet rs=new RecordSet();
    private RecordSet rs1=new RecordSet();

    private String mailType="";

    public String getMailType() {
        return mailType;
    }

    public void setMailType(String mailType) {
        this.mailType = mailType;
    }
    @Override
    public String execute(RequestInfo requestInfo) {
        try{
            MailCustom mc=new MailCustom(requestInfo);
            String requestId=requestInfo.getRequestid();
            String tableName=getTablename(requestInfo);
            String sql="select  * from "+tableName+" where requestid='"+requestId+"'";
            rs.executeQuery(sql);
            rs.next();
            String effectivedate=rs.getString("effectivedate");
            String userid=rs.getString("employeename");
            String employeeid=new ResourceComInfo().getWorkcode(userid);
            String employname=new ResourceComInfo().getLastname(userid);
            String newcostcenter=rs.getString("newcostcenter");
            rs1.executeQuery("select field11 from cus_fielddata where id='"+userid+"' and scopeid=3 and scope='HrmCustomFieldByInfoType'");
            rs1.next();
            String oldcostcenter=rs1.getString(1);
            String homelinemanager=new ResourceComInfo().getLastname(rs.getString("homelinemanager"));
            String newlinemanager=new ResourceComInfo().getLastname(rs.getString("newlinemanager"));
            String kgsfxcbgsfsdyxdz=rs.getString("kgsfxcbgsfsdyxdz");
            String kgsfxgscbfsdyxdz=rs.getString("kgsfxgscbfsdyxdz");
            String gsfx=rs.getString("gsfx");
            Map<String,String> params=new HashMap<String,String>();
            params.put("effectivedate",effectivedate);
            params.put("employeeid",employeeid);
            params.put("employname",employname);
            params.put("newcostcenter",newcostcenter);
            params.put("oldcostcenter",oldcostcenter);
            params.put("homelinemanager",homelinemanager);
            params.put("newlinemanager",newlinemanager);
            String[] to=null;
            if(gsfx.equals("0") || gsfx.equals("1") || gsfx.equals("2")){
                to=kgsfxcbgsfsdyxdz.split(",");
            }else{
                to=kgsfxgscbfsdyxdz.split(",");
            }
            String[] cc={};
            writeLog(kgsfxcbgsfsdyxdz);
            writeLog(params);
            mc.sendMail(to,cc,"Transfer Notice",mailType,params);
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
