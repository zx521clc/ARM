package weaver.interfaces.workflow.action;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.MathUtil;
import weaver.general.Util;
import weaver.soa.workflow.request.RequestInfo;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LZAction extends BaseBean implements Action {
    private RecordSet rs=new RecordSet();
    private SimpleDateFormat sdf=new SimpleDateFormat("yyyy");
    @Override
    public String execute(RequestInfo requestInfo) {
        String requestid=requestInfo.getRequestid();
        String tablename=getTablename(requestInfo);
        String sql="select * from "+tablename+" where requestid='"+requestid+"'";
        rs.executeQuery(sql);
        String employeename="";
        String terminationdate="";
        int month=0;
        int year=0;
        if(rs.next()){
            employeename=rs.getString("employeename");
            terminationdate=rs.getString("terminationdate");
        }
        String belongyear=terminationdate.substring(0,4);
        sql="select baseamount,baseamount2 from kq_balanceOfLeave where resourceid='"+employeename+"' and belongyear='"+belongyear+"'";
        //rs.executeQuery(sql);
        double baseamount=Util.getDoubleValue("-1.0");
        double baseamount2=Util.getDoubleValue("-1.0");
        rs.executeQuery(sql);
        if(rs.next()){
            baseamount=Util.getFloatValue(rs.getString(1),0);
            baseamount2=Util.getFloatValue(rs.getString(2),0);
        }
        month=Integer.parseInt(terminationdate.substring(5,7));
        year=Integer.parseInt(terminationdate.substring(0,4))-1;
        writeLog("year:"+year);
        //法定假核定
        double yxfdj=(baseamount*month)/12;
        double yxflj=(baseamount2*month)/12;
        yxfdj=MathUtil.round(yxfdj,1);
        yxflj=MathUtil.round(yxflj,1);
        if(yxfdj-Math.floor(yxfdj)>0.5){
            yxfdj=Math.floor(yxfdj)+1;
        }else if(yxfdj-Math.floor(yxfdj)<=0.5 && yxfdj-Math.floor(yxfdj)>0){
            yxfdj=Math.floor(yxfdj)+0.5;
        }
        if(yxflj-Math.floor(yxflj)>0.5){
            yxflj=Math.floor(yxflj)+1;
        }else if(yxflj-Math.floor(yxflj)<=0.5 && yxflj-Math.floor(yxflj)>0){
            yxflj=Math.floor(yxflj)+0.5;
        }
        writeLog("yxfdj:"+yxfdj+";yxflj:"+yxflj);

        double carryout=0;
        sql="select baseamount2 from kq_balanceOfLeave where status=0 and leaverulesid=2 and belongyear='"+year+"' and resourceid='"+employeename+"'";
        rs.executeQuery(sql);
        if(rs.next()){
            carryout=Util.getDoubleValue(rs.getString(1),0);
        }
        writeLog("carryout:"+carryout);
        sql="select sum(b.qjts) as ts from workflow_requestbase a,formtable_main_2 b where a.currentnodetype in (1,2,3) and b.qjksrq>='"+sdf.format(new Date())+"-01-01' and a.requestid=b.requestid and b.sqr='"+employeename+"'";
        rs.executeQuery(sql);
        rs.next();
        double qjts=Util.getDoubleValue(rs.getString(1),0);
        writeLog("dnqjts:"+qjts);
        double result=yxfdj+yxflj+carryout-qjts;
        //String template="截止离职当天应该享受的"+yxfdj+",截止离职当天应该享受的"+yxflj+",上一年带过来的假期剩余天数 - 已休的Statuatory - 已休的Company leave；";
        sql="update "+tablename+" set jzlzdtygxsdstatuatory='"+yxfdj+"',jzlzdtygxsdcompanyleave='"+yxflj+"',syndgldjqsyts='"+carryout+"' where requestid='"+requestid+"'";
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
}
