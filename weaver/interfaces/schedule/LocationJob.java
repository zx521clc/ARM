package weaver.interfaces.schedule;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LocationJob extends BaseCronJob {
    private BaseBean bb =new BaseBean();
    private RecordSet rs=new RecordSet();
    private RecordSet rs1=new RecordSet();
    private SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
    @Override
    public void execute() {
        bb.writeLog("=======LocationJob========");
        String date=sdf.format(new Date());
        String sql="select a.employeename,a.ybgdd,a.xbgdd from formtable_main_19 a left outer join workflow_requestbase b on a.requestid=b.requestid "+
                   "where b.currentnodetype=3 and a.effectivedate='"+date+"'";
        rs.executeQuery(sql);
        while(rs.next()){
            String hrid=rs.getString(1);
            String bgdd=rs.getString(3);
            sql="update hrmresource set locationid='"+bgdd+"' where id='"+hrid+"'";
            bb.writeLog(sql);
            rs1.executeUpdate(sql);
        }
    }
}
