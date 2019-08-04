package test;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.ArrayUtils;

public class test1 {

    public static void main(String[] args) {
        String[] temps=new String[3];
        temps[0]="1";
        temps[1]="2";
        temps[2]="3";
        System.out.println(ArrayUtils.toString(temps));
    }
}
