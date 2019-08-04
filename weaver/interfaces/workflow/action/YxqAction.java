package weaver.interfaces.workflow.action;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.soa.workflow.request.RequestInfo;

public class YxqAction extends BaseBean implements Action {
    @Override
    public String execute(RequestInfo requestInfo) {
        String requestid=requestInfo.getRequestid();
        String tablename=getTablename(requestInfo);
        String sql="select yxq,yxqzs,yxqnyr,yxqqt from "+tablename+" where requestid='"+requestid+"'";
        RecordSet rs=new RecordSet();
        rs.executeQuery(sql);
        String yxq="";
        String yxqzs="";
        String yxqnyr="";
        String yxqqt="";
        String result="";
        if(rs.next()){
            yxq=Util.null2String(rs.getString(1));
            yxqzs=Util.null2String(rs.getString(2));
            yxqnyr=Util.null2String(rs.getString(3));
            yxqqt=Util.null2String(rs.getString(4));
        }
        if(yxq.trim().equalsIgnoreCase("0")){
            result="无固定期限";
        }else if(yxq.trim().equalsIgnoreCase("1")){
            sql="select selectname from workflow_selectitem where selectvalue='"+yxqnyr+"' and fieldid in (select id from workflow_billfield where fieldname='yxqnyr' and billid in (select formid from workflow_base where id='"+requestInfo.getWorkflowid()+"'))";
            rs.executeQuery(sql);
            String selectname="";
            if(rs.next()){
                selectname=Util.null2String(rs.getString(1));
            }
            result=yxqzs+selectname;
        }else{
            result=yxqqt;
        }
        boolean flag=rs.executeUpdate("update "+tablename+" set yxqnew='"+result+"' where requestid='"+requestid+"'");
        if(!flag){
            requestInfo.getRequestManager().setMessageid("0");
            requestInfo.getRequestManager().setMessagecontent("有效期处理失败");
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
