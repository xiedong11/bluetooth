package com.zhuandian.bluetooth;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtils {

    public static String getTimeStrNow() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateNowStr = sdf.format(new Date());
//        System.out.println("格式化后的日期：" + dateNowStr);
        return dateNowStr;
    }
}