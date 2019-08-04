package weaver.interfaces.schedule;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.workflow.webservices.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class RZCheckJob extends BaseCronJob {

    private RecordSet rs=new RecordSet();
    private BaseBean bb =new BaseBean();
    private SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
    private Calendar cal=Calendar.getInstance();
    @Override
    public void execute() {
        String tablename=bb.getPropValue("rz","tablename");
        String fieldname=bb.getPropValue("rz","fieldname");
        String nodeid=bb.getPropValue("rz","nodeid");
        String sql="select "+fieldname+",requestid from "+tablename+" where requestid in " +
                "(select requestid from workflow_requestbase where currentnodeid='"+nodeid+"')";
        bb.writeLog("sql:"+sql);
        rs.executeQuery(sql);
        while(rs.next()){
            try{
                String onboarddate= Util.null2String(rs.getString(1));
                String requestid= Util.null2String(rs.getString(2));
                if(onboarddate.equals("")){
                    continue;
                }else{
                    Date date=sdf.parse(onboarddate);
                    cal.setTime(date);
                    cal.add(Calendar.DATE,-3);
                    long date1=cal.getTime().getTime();
                    long date2=sdf.parse(sdf.format(new Date())).getTime();
                    //String datestr=sdf.format(cal.getTime());
                    //String datenow=sdf.format(new Date());
                    bb.writeLog("checkdate-n-c:"+date2+";"+date1);
                    if(date1>date2){
                        continue;
                    }else{
                        String flag=submitWF(requestid,tablename);
                        bb.writeLog("submit_result:"+flag);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private String submitWF(String requestid,String tablename){
        String sql="select * from workflow_requestbase where requestid='"+requestid+"'";
        rs.executeQuery(sql);
        String createrid="";
        String workflowid="";
        if(rs.next()){
            createrid=rs.getString("creater");
            workflowid=rs.getString("workflowid");
        }
        WorkflowService service=new WorkflowServiceImpl();
        WorkflowRequestInfo info=new WorkflowRequestInfo();
        info.setRequestId(requestid);
        info.setCreatorId(createrid);
        WorkflowBaseInfo baseInfo = new WorkflowBaseInfo();
        baseInfo.setWorkflowId(workflowid);
        info.setWorkflowBaseInfo(baseInfo);
        WorkflowMainTableInfo tableInfo=new WorkflowMainTableInfo();
        tableInfo.setTableDBName(tablename);
        sql="select userid from workflow_currentoperator where (isremark in ('0')) and requestid='"+requestid+"'";
        rs.executeQuery(sql);
        rs.next();
        int userid=rs.getInt(1);
        String flag=service.submitWorkflowRequest(info,Integer.parseInt(requestid),userid,"submit","AutoSubmit");
        return flag;
    }
}
