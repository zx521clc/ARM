package weaver.interfaces.workflow.action;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.soa.workflow.request.RequestInfo;

public class ORGAction extends BaseBean implements Action {
    @Override
    public String execute(RequestInfo requestInfo) {
        String requestid=requestInfo.getRequestid();
        String tablename=getTablename(requestInfo);
        String sql="select fb,bm from "+tablename+" where requestid='"+requestid+"'";
        String fb="";
        String bm="";
        String fbcheck="";
        RecordSet rs=new RecordSet();
        rs.executeQuery(sql);
        if(rs.next()){
            fb=rs.getString(1);
            bm=rs.getString(2);
        }
        sql="select subcompanyid1 from hrmdepartment where id='"+bm+"'";
        rs.executeQuery(sql);
        if(rs.next()){
            fbcheck=rs.getString(1);
        }
        if(fb.equals(fbcheck)){
            return SUCCESS;
        }else{
            requestInfo.getRequestManager().setMessageid("0");
            requestInfo.getRequestManager().setMessagecontent("分部与部门不在同一分部");
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
