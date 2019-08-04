package weaver.interfaces.schedule;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CalcHolidayJob extends BaseCronJob{
    private SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
    private RecordSet rs=new RecordSet();
    @Override
    public void execute() {
        Date date=new Date();
        String datestr=sdf.format(date);
        String year="";
        String sql="";
        double baseamount=0;
        double usedamount=0;
        double baseamount2=0;
        double usedamount2=0;
        String resourceid="";
        String id="";
        if(datestr.endsWith("-01-01")){
            year=Util.null2String((Integer.parseInt(datestr.substring(0,4))-1));
            sql="select baseAmount,usedAmount,baseAmount2,usedAmount2,resourceid,id from kq_balanceofleave where leaverulesid=2 and status=0 and belongyear='"+year+"'";
            rs.executeQuery(sql);
            if(rs.next()){
                baseamount= Util.getDoubleValue(rs.getString(1),0);
                usedamount= Util.getDoubleValue(rs.getString(2),0);
                baseamount2= Util.getDoubleValue(rs.getString(3),0);
                usedamount2= Util.getDoubleValue(rs.getString(4),0);
                resourceid=Util.null2String(rs.getString(5));
                id=Util.null2String(rs.getString(6));
            }
            sql="update kq_balanceofleave set status=1 where id='"+id+"'";
            new BaseBean().writeLog("lzsql:"+sql);
            rs.executeUpdate(sql);
            double result=baseamount+baseamount2-usedamount-usedamount2;
            if(result>8){
                result=8;
            }
            sql="insert into kq_balanceofleave (leaverulesid,resourceid,belongyear,baseamount,extraamount,usedamount,baseamount2,extraamount2,usedamount2,status) values (2,'"+resourceid+"','"+year+"',0,0,0,'"+result+"',0,0,0)";
            new BaseBean().writeLog("lzsql1:"+sql);
            rs.executeUpdate(sql);
        }else if(datestr.endsWith("-07-01")){
            year=Util.null2String((Integer.parseInt(datestr.substring(0,4))-1));
            String eyear=datestr.substring(0,4);
            sql="select id from kq_balanceofleave where status=0 and leaverulesid=2 and belongyear='"+year+"'";
            new BaseBean().writeLog("lzsql2:"+sql);
            rs.executeQuery(sql);
            if(rs.next()){
                id=rs.getString(1);
            }
            sql="update kq_balanceofleave set status=1,expirationdate='"+eyear+"-06-30' where id='"+id+"'";
            rs.executeUpdate(sql);
        }

    }
}
