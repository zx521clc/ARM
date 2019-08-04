package weaver.interfaces.schedule;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TransferJob extends BaseCronJob {
    private RecordSet rs=new RecordSet();
    private RecordSet rs1=new RecordSet();
    private BaseBean bb =new BaseBean();
    private SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
    @Override
    public void execute() {
        bb.writeLog("=====Transfer=====");
        String workflowId=bb.getPropValue("transfer","workflowId");
        String tableName=getTableName(workflowId);
        String date=sdf.format(new Date());
        String sql="select * from "+tableName+" where effectivedate='"+date+"'";
        rs.executeQuery(sql);
        while(rs.next()){
            String employeename=rs.getString("employeename");
            String hostdepartment=rs.getString("hostdepartment");
            String newlinemanager=rs.getString("newlinemanager");
            String newcostcenter=rs.getString("newcostcenter");
            String newtitle=rs.getString("newtitle");
            sql="update hrmresource set departmentid='"+hostdepartment+"',manager='"+newlinemanager+"',jobtitle='"+newtitle+"' where id='"+employeename+"'";
            rs1.executeUpdate(sql);
            sql="update cus_fielddata set field11='"+newcostcenter+"' where scope='HrmCustomFieldByInfoType' and scopeid=3 and id='"+employeename+"'";
            rs1.executeUpdate(sql);
        }
    }

    private String getTableName(String workflowid){
        String sql="select tablename from workflow_bill where id in (select formid from workflow_base where isvalid=1 and id='"+workflowid+"')";
        rs.executeQuery(sql);
        rs.next();
        String tableName=rs.getString(1);
        return tableName;
    }
}
