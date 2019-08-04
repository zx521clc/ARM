package weaver.interfaces.workflow.action;

import com.alibaba.fastjson.JSONObject;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.workflow.webservices.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class ContractServer extends HttpServlet {
    private RecordSet rs=new RecordSet();
    private BaseBean bb=new BaseBean();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HttpSession session=req.getSession();
        User user=(User)session.getAttribute("weaver_user@bean");
        boolean flag=false;
        String userId=req.getParameter("userid");
        bb.writeLog("userId1:"+user.getUID());
        bb.writeLog("userId:"+userId);
        String sql="select 1 from formtable_main_21 where employeename='"+userId+"' and requestid in (select requestid from workflow_requestbase where currentnodetype in (1,2) and workflowid=15)";
        rs.executeQuery(sql);
        String requestid="";
        if(rs.getCounts()>0){
            flag=false;
        }else{
            sql="select * from hrmresource where id='"+userId+"'";
            rs.executeQuery(sql);
            rs.next();
            //String subcompanyid=rs.getString("subcompanyid1");
            String contractstartdate=rs.getString("startdate");
            String contractenddate=rs.getString("enddate");
            String hostdepartment=rs.getString("departmentid");
            String linemanager=rs.getString("managerid");
            String title=rs.getString("jobtitle");
            String xb=rs.getString("sex");
            String sfzhm=rs.getString("certificatenum");
            String lxdz=rs.getString("residentplace");
            String lxfs=rs.getString("mobile");
            String jjlxr=rs.getString("homeaddress");
            sql="select * from cus_fielddata where id='"+userId+"' and scopeid='1' and scope='HrmCustomFieldByInfoType'";
            rs.executeQuery(sql);
            rs.next();
            String zwm=rs.getString("field9");
            String hjdz=rs.getString("field3");
            String jjlxdh=rs.getString("field15");
            sql="select * from cus_fielddata where id='"+userId+"' and scopeid='3' and scope='HrmCustomFieldByInfoType'";
            rs.executeQuery(sql);
            rs.next();
            String costcenter=rs.getString("field11");

            try{
                WorkflowService service=new WorkflowServiceImpl();
                WorkflowRequestInfo info=new WorkflowRequestInfo();
                info.setCreatorId(user.getUID()+"");
                info.setRequestName("Contract Renew-"+user.getLastname());
                WorkflowBaseInfo wbi=new WorkflowBaseInfo();
                wbi.setWorkflowId("15");
                info.setWorkflowBaseInfo(wbi);
                WorkflowMainTableInfo mainTableInfo=new WorkflowMainTableInfo();
                mainTableInfo.setTableDBName("formtable_main_21");
                WorkflowRequestTableRecord[] records=new WorkflowRequestTableRecord[1];
                records[0]=new WorkflowRequestTableRecord();
                WorkflowRequestTableField[] fields=new WorkflowRequestTableField[17];
                fields[0]=new WorkflowRequestTableField();
                fields[0].setFieldName("contractstartdate");
                fields[0].setFieldValue(contractstartdate);
                fields[0].setView(true);
                fields[0].setEdit(true);
                fields[1]=new WorkflowRequestTableField();
                fields[1].setFieldName("contractenddate");
                fields[1].setFieldValue(contractenddate);
                fields[1].setView(true);
                fields[1].setEdit(true);
                fields[2]=new WorkflowRequestTableField();
                fields[2].setFieldName("hostdepartment");
                fields[2].setFieldValue(hostdepartment);
                fields[2].setView(true);
                fields[2].setEdit(true);
                fields[3]=new WorkflowRequestTableField();
                fields[3].setFieldName("linemanager");
                fields[3].setFieldValue(linemanager);
                fields[3].setView(true);
                fields[3].setEdit(true);
                fields[4]=new WorkflowRequestTableField();
                fields[4].setFieldName("title");
                fields[4].setFieldValue(title);
                fields[4].setView(true);
                fields[4].setEdit(true);
                fields[5]=new WorkflowRequestTableField();
                fields[5].setFieldName("xb");
                fields[5].setFieldValue(xb);
                fields[5].setView(true);
                fields[5].setEdit(true);
                fields[6]=new WorkflowRequestTableField();
                fields[6].setFieldName("sfzhm");
                fields[6].setFieldValue(sfzhm);
                fields[6].setView(true);
                fields[6].setEdit(true);
                fields[7]=new WorkflowRequestTableField();
                fields[7].setFieldName("lxdz");
                fields[7].setFieldValue(lxdz);
                fields[7].setView(true);
                fields[7].setEdit(true);
                fields[8]=new WorkflowRequestTableField();
                fields[8].setFieldName("lxfs");
                fields[8].setFieldValue(lxfs);
                fields[8].setView(true);
                fields[8].setEdit(true);
                fields[9]=new WorkflowRequestTableField();
                fields[9].setFieldName("jjlxr");
                fields[9].setFieldValue(jjlxr);
                fields[9].setView(true);
                fields[9].setEdit(true);
                fields[10]=new WorkflowRequestTableField();
                fields[10].setFieldName("zwm");
                fields[10].setFieldValue(zwm);
                fields[10].setView(true);
                fields[10].setEdit(true);
                fields[11]=new WorkflowRequestTableField();
                fields[11].setFieldName("hjdz");
                fields[11].setFieldValue(hjdz);
                fields[11].setView(true);
                fields[11].setEdit(true);
                fields[12]=new WorkflowRequestTableField();
                fields[12].setFieldName("jjlxdh");
                fields[12].setFieldValue(jjlxdh);
                fields[12].setView(true);
                fields[12].setEdit(true);
                fields[13]=new WorkflowRequestTableField();
                fields[13].setFieldName("employeename");
                fields[13].setFieldValue(userId);
                fields[13].setView(true);
                fields[13].setEdit(true);
                fields[14]=new WorkflowRequestTableField();
                fields[14].setFieldName("costcenter");
                fields[14].setFieldValue(costcenter);
                fields[14].setView(true);
                fields[14].setEdit(true);
                fields[15]=new WorkflowRequestTableField();
                fields[15].setFieldName("managerrenewopinion");
                fields[15].setFieldValue("0");
                fields[15].setView(true);
                fields[15].setEdit(true);
                fields[16]=new WorkflowRequestTableField();
                fields[16].setFieldName("employeerenewopinion");
                fields[16].setFieldValue("0");
                fields[16].setView(true);
                fields[16].setEdit(true);
                records[0].setWorkflowRequestTableFields(fields);
                mainTableInfo.setRequestRecords(records);
                info.setWorkflowMainTableInfo(mainTableInfo);
                info.setIsnextflow("0");
                requestid=service.doCreateWorkflowRequest(info,user.getUID());
                bb.writeLog("requestid:"+requestid);
                if(Integer.parseInt(Util.getIntValue(requestid,0)+"")>0){
                    flag=true;
                }
            }catch (Exception e){
                e.printStackTrace();
                flag=true;
            }
        }

        JSONObject jo=new JSONObject();
        jo.put("flag", flag);
        jo.put("requestid",Integer.parseInt(Util.getIntValue(requestid,0)+""));
        bb.writeLog(jo.toJSONString());
        PrintWriter out =res.getWriter();
        out.println(jo.toJSONString());
        out.flush();
        out.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }
}
