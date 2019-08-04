package weaver.custom;

import org.apache.commons.lang3.ArrayUtils;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomUtil extends BaseBean {
    private RecordSet rs=new RecordSet();
    public Map<String,Map<String,String>> getResults(String workflowId,String requestId){
        String[] results=getTableName(workflowId);
        String tableName=results[0];
        String detailTableName=results[1];
        String detailKeyField=results[2];
        String sql="select fieldName,viewType,detailTable from workflow_billField where billId in (select formId from workflow_base where id='"+workflowId+"')";
        rs.executeQuery(sql);
        Map<String,Map<String,String>> resultset=new HashMap<String,Map<String,String>>();
        if(detailTableName.equals("") && detailKeyField.equals("")){
            String[] fields=new String[rs.getCounts()];
            int i=0;
            while(rs.next()){
                String field=rs.getString(1);
                fields[i]=field;
                i++;
            }
            String sqlFields = ArrayUtils.toString(fields);
            sqlFields=sqlFields.substring(1,sqlFields.length()-1);
            sql="select "+sqlFields+" from "+tableName+" where requestid='"+requestId+"'";
            rs.executeQuery(sql);
            Map<String,String> map=new HashMap<String,String>();
            if(rs.next()){
                for(String field:fields){
                    String value=Util.null2String(rs.getString(field));
                    map.put(field,value);
                }
            }
            resultset.put(tableName,map);
        }else if(detailTableName.equals("") && !detailKeyField.equals("")){

        }else if(!detailTableName.equals("")){

        }

        return null;
    }

    private String[] getTableName(String workflowId){
        String sql="select tableName,detailTableName,detailKeyField from workflow_bill where id in (select formid from workflow_base where id='"+workflowId+"')";
        rs.executeQuery(sql);
        String tableName="";
        String detailTableName="";
        String detailKeyField="";
        if(rs.next()){
            tableName= Util.null2String(rs.getString(1));
            detailTableName= Util.null2String(rs.getString(2));
            detailKeyField= Util.null2String(rs.getString(3));
        }
        String[] results=new String[3];
        results[0]=tableName;
        results[1]=detailTableName;
        results[2]=detailKeyField;
        return results;
    }
}
