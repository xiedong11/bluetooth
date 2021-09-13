package com.zhuandian.bluetooth;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: MyCrashHandler
 * @Description: TODO 自定义的 异常处理类 , 实现了 UncaughtExceptionHandler接口
 */
public class MyCrashHandler implements UncaughtExceptionHandler {
    // 需求是 整个应用程序 只有一个 MyCrash-Handler
    private static MyCrashHandler myCrashHandler;
    private Context context;
    @SuppressWarnings("unused")
    private SimpleDateFormat dataFormat = new SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss");

    // 1.私有化构造方法
    private MyCrashHandler() {

    }

    // 2. 保证单例模式
    public static synchronized MyCrashHandler getInstance() {
        if (myCrashHandler != null) {
            return myCrashHandler;
        } else {
            myCrashHandler = new MyCrashHandler();
            return myCrashHandler;
        }
    }

    public void init(Context context) {
        this.context = context;
    }

    /*
     * 程序异常的处理方法
     *
     * @see
     * java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang
     * .Thread, java.lang.Throwable)
     */
    @Override
    public void uncaughtException(Thread arg0, Throwable arg1) {
        // System.out.println("程序挂掉了 ");
        // 1.获取当前程序的版本号. 版本的id
        @SuppressWarnings("unused")
        String versioninfo = getVersionInfo();

        // 2.获取手机的硬件信息.
        @SuppressWarnings("unused")
        String mobileInfo = getMobileInfo();

        // 3.把错误的堆栈信息 获取出来
        @SuppressWarnings("unused")
        String errorinfo = getErrorInfo(arg1);

        // 4.把所有的信息 还有信息对应的时间 提交到服务器
        try {
            // System.out.println("捕获到全局异常啦");
            arg1.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 保存崩溃日志
        saveErrorLog(arg1);
        /**
         * 在用户许可下,将异常信息上传到网络.
         */

        //        // 重新启动
        //
//        Intent intent = new Intent(context, MainActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(intent);

        // 0 表示正常退出 1表示强制退出
        System.exit(1);
        // 干掉当前的程序
    }

    /**
     * 获取错误的信息
     *
     * @param arg1
     * @return
     */
    private String getErrorInfo(Throwable arg1) {
        Writer writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        arg1.printStackTrace(pw);
        pw.close();
        String error = writer.toString();
        return error;
    }

    /**
     * 获取手机的硬件信息
     *
     * @return
     */
    private String getMobileInfo() {
        StringBuffer sb = new StringBuffer();
        // 通过反射获取系统的硬件信息
        try {

            Field[] fields = Build.class.getDeclaredFields();
            for (Field field : fields) {
                // 暴力反射 ,获取私有的信息
                field.setAccessible(true);
                String name = field.getName();
                String value = field.get(null).toString();
                sb.append(name + "=" + value);
                sb.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * 获取手机的版本信息
     *
     * @return
     */
    private String getVersionInfo() {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (Exception e) {
            e.printStackTrace();
            return "版本号未知";
        }
    }

    private Map<String, String> info = new HashMap<String, String>();// 用来存储设备信息和异常信息

    /**
     * 保存异常日志
     *
     * @param excp
     */
    public void saveErrorLog(Throwable excp) {

        String fileName = "/log.html";
        String logFilePath = "";
        FileWriter fw = null;
        PrintWriter pw = null;
        try {
            // 判断是否挂载了SD卡
            String storageState = Environment.getExternalStorageState();
            if (storageState.equals(Environment.MEDIA_MOUNTED)) {

                File downloadFile = new File(Environment.getExternalStorageDirectory(), "log");
                if (!downloadFile.mkdirs()) {
                    downloadFile.createNewFile();
                }
                logFilePath = downloadFile.getAbsolutePath() + fileName;

            }
            // 没有挂载SD卡，无法写文件
            if (logFilePath == "") {
                return;
            }
            File logFile = new File(logFilePath);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            fw = new FileWriter(logFile, true);
            pw = new PrintWriter(fw);
            pw.println("\r\n\r\n\r\n---------------"
                    + (new Date().toLocaleString()) + "**********版本号:"
                    + info.get("versionName") + "------------------");
            excp.printStackTrace(pw);
            pw.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                }
            }
        }

    }
}
