package weaver.interfaces.workflow.action;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.resource.ResourceComInfo;
import weaver.soa.workflow.request.RequestInfo;

public class DocSubjectAction extends BaseBean implements Action {
    private RecordSet rs=new RecordSet();
    @Override
    public String execute(RequestInfo requestInfo) {
        String title=getDocSubject(requestInfo);
        String sql="select b.fieldname from workflow_createdoc a inner join workflow_billfield b on a.flowDocField=b.id " +
                   "where a.workflowId='"+requestInfo.getWorkflowid()+"'";
        rs.executeQuery(sql);
        String fieldname="";
        if(rs.next()){
            fieldname=rs.getString(1);
        }
        String tablename=getTablename(requestInfo);
        sql="update docdetail set docsubject='"+title+"' where id=(select "+fieldname+" from "+tablename+" where requestid='"+requestInfo.getRequestid()+"')";
        boolean flag=rs.executeUpdate(sql);
        if(flag){
            return SUCCESS;
        }else{
            return FAILURE_AND_CONTINUE;
        }
    }

    private String getDocSubject(RequestInfo info){
        try {
            String workflowid=info.getWorkflowid();
            String sql="select formid from workflow_base where id='"+workflowid+"'";
            String tablename=getTablename(info);
            String requestid=info.getRequestid();
            RecordSet rs=new RecordSet();
            rs.executeQuery(sql);
            rs.next();
            String formid=rs.getString(1);
            String fieldstr=getPropValue("DocAction",formid);
            writeLog("fieldstr:"+fieldstr);
            if(Util.null2String(fieldstr).equals("")){
                return info.getRequestManager().getRequestname();
            }
            String[] fields=null;
            if(fieldstr.indexOf(",")>0){
                fields=fieldstr.split(",");
            }else {
                fields=new String[1];
                fields[0]=fieldstr;
            }
            String pdfield=getPropValue("DocAction",formid+"_pdfield");
            String title="";
            String pdfieldvalue=getPropValue("DocAction",formid+"_pdvalue");
            writeLog(pdfield+";"+pdfieldvalue);
            if(!pdfield.equals("")){
                String[] temps=null;
                if(pdfieldvalue.indexOf(",")>0){
                    temps=pdfieldvalue.split(",");
                }else{
                    temps=new String[1];
                    temps[0]=pdfieldvalue;
                }
                sql="select "+fieldstr+","+pdfield+" from "+tablename+" where requestid='"+requestid+"'";
                writeLog(sql);
                rs.executeQuery(sql);
                if(rs.next()){
                    String pdvalue=rs.getString(pdfield);
                    for(String temp:temps){
                        String value=temp.substring(0,temp.indexOf(":"));
                        if(pdvalue.equals(value)){
                            title=temp.substring(temp.indexOf(":")+1,temp.length())+"-";
                            break;
                        }
                    }
                    writeLog(title);
                    for(String field:fields){
                        String value=rs.getString(field);
                        if(ishrm(field,formid)){
                            value=new ResourceComInfo().getLastnames(value);
                        }
                        if(title.contains("${"+field+"}")){
                            title=title.replaceAll("${"+field+"}",value);
                        }else{
                            title=title+value+"-";
                        }

                    }
                    title=title.substring(0,title.length()-1);
                    writeLog(title);
                }
            }else{
                title=pdfieldvalue.substring(pdfieldvalue.indexOf(":")+1,pdfieldvalue.length())+"-";
                sql="select "+fieldstr+" from "+tablename+" where requestid='"+requestid+"'";
                rs.executeQuery(sql);
                if(rs.next()){
                    for(String field:fields){
                        String value=rs.getString(field);
                        if(ishrm(field,formid)){
                            value=new ResourceComInfo().getLastnames(value);
                        }
                        if(title.contains("${"+field+"}")){
                            title=title.replaceAll("${"+field+"}",value);
                        }else{
                            title=title+value+"-";
                        }
                    }
                    title=title.substring(0,title.length()-1);
                }
            }
            return title;
        }catch (Exception e){
            e.getStackTrace();
            return info.getRequestManager().getRequestname();
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

    private boolean ishrm(String fieldid,String formid){
        String sql="select fieldhtmltype,type from workflow_billfield where fieldname='"+fieldid+"' and billid='"+formid+"'";
        RecordSet rs=new RecordSet();
        rs.executeQuery(sql);
        String fieldhtmltype="";
        String type="";
        if(rs.next()){
            fieldhtmltype=rs.getString(1);
            type=rs.getString(2);
        }
        if(fieldhtmltype.equals("3") && type.equals("1")){
            return true;
        }else{
            return false;
        }
    }
}
