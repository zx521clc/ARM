package weaver.interfaces.workflow.action;


import com.alibaba.fastjson.JSONObject;
import com.engine.personalIncomeTax.biz.RecordsBiz;
import com.sun.net.httpserver.Authenticator;
import org.apache.commons.io.FileUtils;
import weaver.conn.RecordSet;

import weaver.docs.docs.DocImageManager;

import weaver.docs.webservices.DocInfo;
import weaver.docs.webservices.DocServiceImpl;
import weaver.file.ImageFileManager;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.UserManager;
import weaver.hrm.resource.ResourceComInfo;
import weaver.soa.workflow.request.RequestInfo;
import weaver.systeminfo.SystemEnv;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DocActionNew extends BaseBean implements Action {
    @Override
    public String execute(RequestInfo requestInfo) {
        String requestid=requestInfo.getRequestid();
        int userId=requestInfo.getRequestManager().getCreater();
        String tablename=getTablename(requestInfo);
        //String sql1="select ndam from "+tablename+" where requestid='"+requestid+"'";
        RecordSet rs=new RecordSet();
        //rs.executeQuery(sql1);
        //rs.next();
        //String ndam=rs.getString(1);
        //if(ndam.trim().equalsIgnoreCase("")){
            String workflowid=requestInfo.getWorkflowid();
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sdf1=new SimpleDateFormat("HH:mm:ss");
            Date now=new Date();
            String date = sdf.format(now);
            String time= sdf1.format(now);
            String sql="select flowDocCatField,flowDocField from workflow_createdoc where workflowid="+workflowid;

            rs.executeQuery(sql);
            String fieldid="";
            String fieldid1="";
            if(rs.next()){
                fieldid=Util.null2String(rs.getString(1));
                fieldid1=Util.null2String(rs.getString(2));
            }
            sql="select fieldname from workflow_billfield where id='"+fieldid+"'";
            rs.executeQuery(sql);
            String fieldname="";
            if(rs.next()){
                fieldname=Util.null2String(rs.getString(1));
            }
            writeLog("fieldname:"+fieldname);
            sql="select fieldname from workflow_billfield where id='"+fieldid1+"'";
            rs.executeQuery(sql);
            String fieldname1="";
            if(rs.next()){
                fieldname1=Util.null2String(rs.getString(1));
            }
            sql="select "+fieldname1+" from "+tablename+" where requestid='"+requestid+"'";
            rs.executeQuery(sql);
            String clcvalue="";
            if(rs.next()){
                clcvalue=Util.null2String(rs.getString(1));
            }
            if(clcvalue.equalsIgnoreCase("")){
                String mouldid="";
                int seccategory=-1;
                if(!fieldname.equals("")){
                    sql="select "+fieldname+" from "+tablename+" where requestid='"+requestid+"'";
                    rs.executeQuery(sql);
                    String fieldvalue1="";
                    if(rs.next()){
                        fieldvalue1=Util.null2String(rs.getString(1));
                    }
                    sql="select mouldid,seccategory from workflow_mould where visible=1 and mouldtype in (3,4) and workflowid='"+workflowid+"' and selectvalue='"+fieldvalue1+"'";
                    rs.executeQuery(sql);
                    if(rs.next()){
                        mouldid=Util.null2String(rs.getString(1));
                        seccategory=Util.getIntValue(rs.getString(2),-1);
                    }
                }else{
                    sql="select mouldid,seccategory from workflow_mould where visible=1 and mouldtype in (3,4) and workflowid='"+workflowid+"' and isnull(selectvalue,-1)=-1";
                    rs.executeQuery(sql);
                    if(rs.next()){
                        mouldid=Util.null2String(rs.getString(1));
                        seccategory=Util.getIntValue(rs.getString(2),-1);
                    }
                }
                writeLog("mouldid:"+mouldid);
                String mouldpath="";
                sql="select mouldpath from docmouldfile where id='"+mouldid+"'";
                rs.executeQuery(sql);
                if(rs.next()){
                    mouldpath=Util.null2String(rs.getString(1));
                }
                if(mouldpath.equals("")){
                    sql="select filerealpath from ImageFile where imagefileid in (select imagefileid from docmouldfile where id='"+mouldid+"')";
                    rs.executeQuery(sql);
                    if(rs.next()){
                        mouldpath=Util.null2String(rs.getString(1));
                    }
                }
                writeLog("mouldpath:"+mouldpath);
                File file=new File(mouldpath);
                if(!file.exists()){
                    requestInfo.getRequestManager().setMessageid("0");
                    requestInfo.getRequestManager().setMessagecontent("没有找到模板文件，请联系系统管理员!");
                    return Action.FAILURE_AND_CONTINUE;
                }else{
                    byte[] buffer = null;
                    try {
                        String extname=mouldpath.substring(mouldpath.lastIndexOf(".")+1,mouldpath.length());
                        String rand= UUID.randomUUID().toString();
                        File file1=null;
                        if(extname.equalsIgnoreCase("zip")){

                            file1=unzip(mouldpath,"d:\\weaver\\ecology\\tmpfile","test"+rand+".doc");

                        }else{
                            file1=new File("d:\\weaver\\ecology\\tmpfile\\test"+rand+".doc");
                            boolean iscopy=copyFile(file,file1);
                            writeLog("iscopy:"+iscopy);
                        }
                        FileInputStream fis = new FileInputStream(file1);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream((int)file.length());
                        byte[] b = new byte[(int)file.length()];
                        int n;
                        while ((n = fis.read(b)) != -1) {
                            bos.write(b, 0, n);
                            bos.flush();
                        }
                        buffer = bos.toByteArray();
                        fis.close();
                        bos.close();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    ImageFileManager ifm=new ImageFileManager();
                    ifm.setImagFileName(requestInfo.getRequestManager().getRequestname()+".doc");
                    ifm.setData(buffer);
                    int bresult=ifm.saveImageFile();
                    writeLog("imagefileid:"+bresult);
                    UserManager manager=new UserManager();
                    User user=manager.getUserByUserIdAndLoginType(userId, "1");
                    writeLog("test:"+user.getUID());
                    writeLog("Loginid:"+user.getLoginid());
                    writeLog("lastname:"+user.getLastname());
                    DocInfo info=new DocInfo();
                    writeLog("test1:docinfo");
                    info.setDoccreaterid(userId);
                    info.setDoccreatedate(date);
                    info.setDoccreatetime(time);
                    info.setDoclastmoduserid(userId);
                    info.setDoclastmoddate(date);
                    info.setDoclastmodtime(time);
                    //info.setDocSubject(requestInfo.getRequestManager().getRequestname());
                    info.setDocSubject(getDocSubject(requestInfo));
                    //info.setMaincategory(6);
                    info.setSeccategory(seccategory);
                    info.setId(0);
                    info.setDocType(2);
                    info.setDocStatus(1);
                    //info.setSubcategory(10);
                    info.setImagefileId(bresult);
                    DocServiceImpl service=new DocServiceImpl();
                    try {
                        int result=service.createDocByUser(info, user);
                        writeLog("doc-result:"+result);
                        if(result>0){
                            rs.executeUpdate("update docdetail set doctype=2,maindoc='"+result+"',docExtendName='doc' where id="+result);
                        }
                        DocImageManager dim=new DocImageManager();
                        dim.setDocid(result);
                        dim.setImagefileid(bresult);
                        dim.setDocfiletype("3");
                        dim.setImagefilename(requestInfo.getRequestManager().getRequestname()+".doc");
                        dim.AddDocImageInfo();
                        boolean flag1 = rs.executeUpdate("update "+tablename+" set "+fieldname1+"='"+result+"' where requestid="+requestid);
                        writeLog("fj-"+flag1);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        writeLog(e.getMessage());
                        writeLog(e.getStackTrace());
                        requestInfo.getRequestManager().setMessageid("0");
                        requestInfo.getRequestManager().setMessagecontent("DocService ERROR!"+SystemEnv.getHtmlLabelName(17024, 7));
                        return FAILURE_AND_CONTINUE;
                    }
                }
                return SUCCESS;
            }else{
                return SUCCESS;
            }


        //}else {
        //    return SUCCESS;
        //}
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

    private File unzip(String src,String dest,String filename) throws Exception{
        writeLog("src:"+src);
        writeLog("dest:"+dest);
        File file=new File(src);
        if(!file.exists()){
            return null;
        }else {
            ZipFile zf = new ZipFile(file);
            Enumeration entries = zf.entries();
            File f = null;
            if (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                //String path=dest + File.separator + entry.getName().substring(entry.getName().lastIndexOf("\\")+1,entry.getName().length());
                String path=dest + File.separator + filename;
                writeLog("path:"+path);
                f = new File(path);
                //boolean flag=f.createNewFile();
                //writeLog("flag:"+flag);
                InputStream is = zf.getInputStream(entry);
                FileOutputStream fos = new FileOutputStream(f);
                int count;
                byte[] buf = new byte[8192];
                while ((count = is.read(buf)) != -1) {
                    fos.write(buf, 0, count);
                }
                is.close();
                fos.close();
            }
            return f;
        }
    }

    public boolean copyFile(File oldfile,File newfile) {
        //newPath=newPath.replace("/", ".");
        //new BaseBean().writeLog(oldPath+"&&&&&"+newPath);
        boolean flag=false;
        try {
            //File oldfile = new File(oldPath);
            //File newfile = new File(newPath);
            FileUtils.copyFile(oldfile, newfile);
            flag=true;
//			int bytesum = 0;
//			int byteread = 0;
//			File oldfile = new File(oldPath);
//			File newfile = new File(newPath);
//			if (oldfile.exists()) { //文件存在时
//				InputStream inStream = new FileInputStream(oldPath); //读入原文件
//				//FileOutputStream fs = new FileOutputStream(newPath);
//				FileOutputStream fs = new FileOutputStream(newPath);
//				byte[] buffer = new byte[8192];
//				int length;
//				while ( (byteread = inStream.read(buffer)) != -1) {
//					bytesum += byteread; //字节数 文件大小
//					System.out.println(bytesum);
//					fs.write(buffer, 0, byteread);
//				}
//				fs.flush();
//				inStream.close();
//				flag=true;
//			}
        }
        catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();
            flag=false;
        }
        return flag;
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
