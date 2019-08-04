package weaver.interfaces.workflow.action;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.soa.workflow.request.RequestInfo;

public class HRMRZAction extends BaseBean implements Action {
    @Override
    public String execute(RequestInfo requestInfo) {
        String requestId=requestInfo.getRequestid();
        String tableName=getTablename(requestInfo);
        String sql="select dlm,zwm,jtlxdh_new,gj,hjdz,permanentcountry,major,university,"+
                "bu,organisation,costcentercode,onboarddate,cjgzrq,leavecalculatestartdate,"+
                "dzyj,sfzh,presentcountry,presentcity from "+tableName+" where requestid='"+requestId+"'";
        RecordSet rs=new RecordSet();
        rs.executeQuery(sql);
        String loginId="";
        String zwm="";
        String jtlxdh="";
        String gj="";
        String hjdz="";
        String permanentcountry="";
        String major="";
        String university="";
        String bu="";
        String organisation="";
        String costcentercode="";
        String onboarddate="";
        String cjgzrq="";
        String leavecalculatestartdate="";
        String dzyj="";
        String certificatenum="";
        String presentcountry="";
        String presentcity="";
        if(rs.next()){
            loginId=Util.null2String(rs.getString(1));
            zwm=Util.null2String(rs.getString(2));
            jtlxdh=Util.null2String(rs.getString(3));
            gj=Util.null2String(rs.getString(4));
            hjdz=Util.null2String(rs.getString(5));
            permanentcountry=Util.null2String(rs.getString(6));
            major=Util.null2String(rs.getString(7));
            university=Util.null2String(rs.getString(8));
            bu=Util.null2String(rs.getString(9));
            organisation=Util.null2String(rs.getString(10));
            costcentercode=Util.null2String(rs.getString(11));
            onboarddate=Util.null2String(rs.getString(12));
            cjgzrq=Util.null2String(rs.getString(13));
            leavecalculatestartdate=Util.null2String(rs.getString(14));
            dzyj=Util.null2String(rs.getString(15));
            certificatenum=Util.null2String(rs.getString(16));
            presentcountry=Util.null2String(rs.getString(17));
            presentcity=Util.null2String(rs.getString(18));
        }
        sql="select id from hrmresource where loginid='"+loginId+"'";
        writeLog("sql3:"+sql);
        String userId="";
        rs.executeQuery(sql);
        if(rs.next()){
            userId=rs.getString(1);
        }
        writeLog("userid:"+userId);
        sql="update hrmresource set companystartdate='"+leavecalculatestartdate+"',workstartdate='"+cjgzrq+"',certificatenum='"+certificatenum+"' where id='"+userId+"'";
        rs.executeUpdate(sql);
        String maxid=getMaxId();
        sql="insert into cus_fielddata (scope,scopeid,id,field9,field15,field0,field3,field6,field7,field8,field10,field4,field5) values ('HrmCustomFieldByInfoType',1,"+userId+",'"+zwm+"','"+jtlxdh+"','"+gj+"','"+hjdz+"','"+permanentcountry+"','"+major+"','"+university+"','"+dzyj+"','"+presentcountry+"','"+presentcity+"')";
        writeLog("sql1:"+sql);
        rs.executeUpdate(sql);
        maxid=getMaxId();
        sql="insert into cus_fielddata (scope,scopeid,id,field13,field2,field11,field12) values ('HrmCustomFieldByInfoType',3,"+userId+",'"+bu+"','"+organisation+"','"+costcentercode+"','"+onboarddate+"')";
        writeLog("sql2:"+sql);
        rs.executeUpdate(sql);
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

    private String getMaxId(){
        String sql="select max(seqorder) as maxid from cus_fielddata";
        RecordSet rs=new RecordSet();
        rs.executeQuery(sql);
        int maxid=-1;
        rs.next();
        maxid=Util.getIntValue(rs.getString(1),-1)+1;
        return maxid+"";
    }
}
