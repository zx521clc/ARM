package weaver.interfaces.workflow.action;

import weaver.conn.RecordSet;
import weaver.docs.category.*;
import weaver.docs.docs.DocComInfo;
import weaver.docs.docs.DocManager;
import weaver.docs.docs.DocSeccategoryUtil;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.soa.workflow.request.RequestInfo;
import weaver.workflow.webservices.WorkflowService;

public class DirAction extends BaseBean implements Action {
    @Override
    public String execute(RequestInfo requestInfo) {
        String requestid=requestInfo.getRequestid();
        String workflowid=requestInfo.getWorkflowid();
        String tablename=getTablename(requestInfo);
        RecordSet rs=new RecordSet();
        String sql="select flowDocCatField from workflow_createdoc where workflowid="+workflowid;
        rs.executeQuery(sql);
        String fieldid="";
        if(rs.next()){
            fieldid=Util.null2String(rs.getString(1));
        }
        sql="select fieldname from workflow_billfield where id="+fieldid;
        rs.executeQuery(sql);
        String fieldname="";
        if(rs.next()){
            fieldname=Util.null2String(rs.getString(1));
        }
        sql="select "+fieldname+",htlx,xdfgsqc,htbh,ndam,bzwgzpdffj,fbhtfj,gzhfj from "+tablename+" where requestid="+requestid;
        String fieldvalue="";
        String htlx="";
        String xdfgzqc="";
        String htbh="";
        String ndam="";
        String bzwgzpdffj="";
        String fbhtfj="";
        String gzhfj="";
        rs.executeQuery(sql);
        if(rs.next()){
            fieldvalue=Util.null2String(rs.getString(1));
            htlx=Util.null2String(rs.getString(2));
            xdfgzqc=Util.null2String(rs.getString(3));
            htbh=Util.null2String(rs.getString(4));
            ndam=Util.null2String(rs.getString(5));
            bzwgzpdffj=Util.null2String(rs.getString(6));
            fbhtfj=Util.null2String(rs.getString(7));
            gzhfj=Util.null2String(rs.getString(8));
        }
        sql="select doccategory from workflow_selectitem where selectvalue='"+fieldvalue+"' and fieldid='"+fieldid+"'";
        rs.executeQuery(sql);
        String doccategory="";
        if(rs.next()){
            doccategory=Util.null2String(rs.getString(1));
        }
        writeLog("doccategory:"+doccategory);
        //String topseccategory=getMainCategory(doccategory);
        String topseccategory="15";
        writeLog("topseccategory:"+topseccategory);
        sql="select selectname from workflow_SelectItem where selectvalue="+htlx+" and " +
                "fieldid=(select id from workflow_billfield where fieldname='htlx' and " +
                "billid=(select formid from workflow_base where id="+workflowid+")) ";
        rs.executeQuery(sql);
        String selectname="";
        if(rs.next()){
            selectname=Util.null2String(rs.getString(1));
        }
        sql="select * from DocSecCategory where categoryname='"+selectname+"'";
        String dirid="";
        rs.executeQuery(sql);
        if(rs.next()){
            dirid=rs.getString("id");
        }else{
            dirid=addCategory(topseccategory,selectname);
        }
        String dirid1="";
        if(xdfgzqc.equals("")){
            requestInfo.getRequestManager().setMessageid("0");
            requestInfo.getRequestManager().setMessagecontent("供应商名称不能为空");
            return FAILURE_AND_CONTINUE;
        }else{
            sql="select * from DocSecCategory where categoryname='"+xdfgzqc+"'";
            rs.executeQuery(sql);
            if(rs.next()){
                dirid1=rs.getString("id");
            }else{
                dirid1=addCategory(dirid,xdfgzqc);
            }
        }
        sql="select id from DocSecCategory where categoryname='NDA' and parentid="+dirid1;
        rs.executeQuery(sql);
        String dirid2="";
        if(rs.next()){
            dirid2=rs.getString("id");
        }else{
            dirid2=addCategory(dirid1,"NDA");
        }
        sql="select id from DocSecCategory where categoryname='"+htbh+"' and parentid="+dirid2;
        rs.executeQuery(sql);
        String dirid3="";
        if(rs.next()){
            dirid3=rs.getString("id");
        }else{
            dirid3=addCategory(dirid2,htbh);
        }
        sql="update docdetail set seccategory="+dirid3+" where id in ("+ndam+","+bzwgzpdffj+","+gzhfj+","+(fbhtfj.equals("")?"-1":fbhtfj)+")";
        boolean flag=rs.executeUpdate(sql);
        if(flag){
            rs.executeUpdate("update docdetail set docsubject='"+htbh+"-signed' where id="+gzhfj);
            rs.executeUpdate("update docdetail set docsubject='"+htbh+"-pdf' where id="+bzwgzpdffj);
            rs.executeUpdate("update docdetail set docsubject='"+htbh+"-doc' where id="+ndam);
            DocComInfo dc=new DocComInfo();
            dc.removeDocCache();
            SecCategoryDocPropertiesComInfo scdpc=new SecCategoryDocPropertiesComInfo();
            sql="delete from DocSecCategoryDocProperty where secCategoryId='"+dirid3+"' and id>(select top 1 id from (select top 25 id from DocSecCategoryDocProperty where secCategoryId='"+dirid3+"' order by id) a order by a.id desc)";
            rs.executeUpdate(sql);
            scdpc.removeCache();
            return SUCCESS;
        }

        return FAILURE_AND_CONTINUE;
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

    private String getMainCategory(String seccategory){
        String parentid="";
        String tempid=seccategory;
        while(!parentid.equals("0")){
            String sql="select parentid from DocSecCategory where id="+tempid;
            RecordSet rs=new RecordSet();
            rs.executeQuery(sql);
            if(rs.next()){
                parentid=Util.null2String(rs.getString(1));
            }
            writeLog("parentid:"+parentid);
            if(!parentid.equals("0")){
                tempid=parentid;
            }
        }
        writeLog("tempid:"+tempid);
        return tempid;
    }

    private String addCategory(String parentid,String categoryname){
        char flag=Util.getSeparator();

        int subcategoryid=0;
        String docmouldid="0";
        String publishable = "";
        String replyable = "";
        //String action = "";
        String hasaccessory = "";
        String accessorynum = "";
        String hasasset = "";
        String assetlabel = "";
        String hasitems = "";
        String itemlabel = "";
        String hashrmres = "";
        String shareable = "";
        String hrmreslabel = "";
        String hascrm = "";
        String crmlabel = "";
        String cusertype = "";
        int cuserseclevel = 0;
        int cdepartmentid1 = 0;
        int cdepseclevel1 = 0;
        int cdepartmentid2 = 0;
        int cdepseclevel2 = 0;
        int croleid1 = 0;
        String crolelevel1 = "";
        int croleid2 = 0;
        String crolelevel2 = "";
        int croleid3 = 0;
        String crolelevel3 = "";
        String hasproject = "";
        String projectlabel = "";
        String hasfinance = "";
        String financelabel = "";
        String approveworkflowid = "";
        String markable = "";
        String markAnonymity = "";
        String orderable = "";
        String defaultLockedDoc = "";
        String allownModiMShareL = "";
        String allownModiMShareW = "";
        String maxUploadFileSize = "";
        String wordmouldid = "";
        String isSetShare = "";
        String nodownload = "";
        String norepeatedname = "";
        String iscontroledbydir = "";
        String puboperation = "";
        String childdocreadremind = "";
        String readoptercanprint = "";
        String isLogControl = "";
        String sql="select * from DocSecCategory where id="+parentid;
        RecordSet rs=new RecordSet();
        rs.executeQuery(sql);
        if(rs.next()){
            //subcategoryid = Util.getIntValue(rs.getString("subcategoryid"),-1);
            accessorynum = rs.getInt("accessorynum")>0?rs.getInt("accessorynum")+"":"0";
            docmouldid=Util.getIntValue(rs.getString("docmouldid"),0)+"";
            publishable=Util.null2String(rs.getString("publishable"));
            replyable=Util.null2String(rs.getString("replyable"));
            shareable=Util.null2String(rs.getString("shareable"));
            cusertype=Util.getIntValue(rs.getString("cusertype"),0)+"";
            cuserseclevel=Util.getIntValue(rs.getString("cuserseclevel"),0);
            cdepartmentid1=Util.getIntValue(rs.getString("cdepartmentid1"),0);
            cdepseclevel1=Util.getIntValue(rs.getString("cdepseclevel1"),0);
            cdepartmentid2=Util.getIntValue(rs.getString("cdepartmentid2"),0);
            cdepseclevel2=Util.getIntValue(rs.getString("cdepseclevel2"),0);
            croleid1=Util.getIntValue(rs.getString("croleid1"),0);
            crolelevel1=rs.getString("crolelevel1");
            croleid2=Util.getIntValue(rs.getString("croleid2"),0);
            crolelevel2=rs.getString("crolelevel2");
            croleid3=Util.getIntValue(rs.getString("croleid3"),0);
            crolelevel3=rs.getString("crolelevel3");
            hasaccessory=rs.getString("hasaccessory");
            hasasset=rs.getString("hasasset");
            assetlabel=rs.getString("assetlabel");
            hasitems=rs.getString("hasitems");
            itemlabel=rs.getString("itemlabel");
            hashrmres=rs.getString("hashrmres");
            hrmreslabel=rs.getString("hrmreslabel");
            hascrm=rs.getString("hascrm");
            crmlabel=rs.getString("crmlabel");
            hasproject=rs.getString("hasproject");
            projectlabel=rs.getString("projectlabel");
            hasfinance=rs.getString("hasfinance");
            financelabel= rs.getString("financelabel");
            approveworkflowid=rs.getInt("approveworkflowid")+"";
            markable=rs.getString("markable");
            markAnonymity=rs.getString("markAnonymity");
            orderable=rs.getString("orderable");
            defaultLockedDoc=rs.getInt("defaultLockedDoc")+"";
            allownModiMShareL=rs.getInt("allownModiMShareL")+"";
            allownModiMShareW=rs.getInt("allownModiMShareW")+"";
            maxUploadFileSize=rs.getInt("maxUploadFileSize")+"";
            wordmouldid=rs.getInt("wordmouldid")+"";
            isSetShare=Util.getIntValue(rs.getString("isSetShare"),0)+"";
            nodownload=Util.getIntValue(rs.getString("noDownload"),0)+"";
            norepeatedname="1";
            iscontroledbydir=Util.getIntValue(rs.getString("isControledByDir"))+"";
            puboperation=Util.getIntValue(rs.getString("pubOperation"))+"";
            childdocreadremind=Util.getIntValue(rs.getString("childDocReadRemind"))+"";
            readoptercanprint= Util.getIntValue(rs.getString("readOpterCanPrint"),0)+"";
            isLogControl=rs.getString("isLogControl");
        }
        String param=subcategoryid+""+flag+categoryname+flag+docmouldid+flag+publishable+flag+replyable+flag+
                shareable+flag+cusertype+flag+cuserseclevel+flag+cdepartmentid1+flag+cdepseclevel1+flag+
                cdepartmentid2+flag+cdepseclevel2+flag+croleid1+flag+crolelevel1+flag+croleid2+flag+crolelevel2+flag+
                croleid3+flag+crolelevel3+flag+hasaccessory+flag+accessorynum+flag+hasasset+flag+assetlabel+flag+
                hasitems+flag+itemlabel+flag+hashrmres+flag+hrmreslabel+flag+hascrm+flag+crmlabel+flag+hasproject+flag+
                projectlabel+flag+hasfinance+flag+financelabel+flag+approveworkflowid+flag+markable+flag+
                markAnonymity+flag+orderable+flag+defaultLockedDoc+flag+allownModiMShareL+flag+
                allownModiMShareW+flag+maxUploadFileSize+flag+wordmouldid+flag+isSetShare+flag+
                nodownload+flag+norepeatedname+flag+iscontroledbydir+flag+puboperation+flag+
                childdocreadremind+flag+readoptercanprint+flag+isLogControl;
        //writeLog("param:"+param);
        SecCategoryManager scm=new SecCategoryManager();
        //param=scm.copyAttrFromParent(param,parentid,categoryname,1);
        //RecordSet rs=new RecordSet();
        rs.executeProc("Doc_SecCategory_Insert",param);
        rs.next();
        String id=rs.getString(1);
        //writeLog("docid:"+id);
        rs.executeUpdate("update DocSecCategory set parentid="+parentid+" where id="+id);
        //SecCategoryComInfo scc=new SecCategoryComInfo();
        //int level =scc.getLevel(parentid,true);
        //scm.copyOtherInfoFromParent(Integer.parseInt(id),parentid,level);
        //SecCategoryDocPropertiesComInfo scdpc=new SecCategoryDocPropertiesComInfo();
        //scdpc.addDefaultDocProperties(Integer.parseInt(id));
        //scdpc.removeCache();
        //scc.removeCache();
        return id;
    }
}
