package com.zhuandian.bluetooth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;

    private TextView txtIsConnected;

    private Button btnPairedDevices;

    private BluetoothAdapter mBluetoothAdapter;
    private ConnectedThread mConnectedThread;
    private TextView tvData;
    private LineChart mChart;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtIsConnected = (TextView) findViewById(R.id.txtIsConnected);
        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaveDataDialog();
            }
        });

        mChart = (LineChart) findViewById(R.id.chart1);
//        mChart.setOnChartGestureListener(this);
//        mChart.setOnChartValueSelectedListener(this);

        initLineChar();
        btnPairedDevices = (Button) findViewById(R.id.btnPairedDevices);
        tvData = (TextView) findViewById(R.id.tv_data);

        btnPairedDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 获取蓝牙适配器
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter == null) {
                    Toast.makeText(getApplicationContext(), "该设备不支持蓝牙", Toast.LENGTH_SHORT).show();
                }

                //请求开启蓝牙
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }

                //进入蓝牙设备连接界面
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), DevicesListActivity.class);
                startActivity(intent);

            }
        });

    }

    private void initLineChar() {
        Description desc = new Description();
        desc.setText("电阻值数据表");
        mChart.setNoDataText("暂无数据");
        //得到X轴
        XAxis xAxis = mChart.getXAxis();
        //设置X轴的位置（默认在上方)
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //设置X轴的值（最小值、最大值、然后会根据设置的刻度数量自动分配刻度显示）
        xAxis.setAxisMinimum(1f);
        //不显示网格线
//        xAxis.setDrawGridLines(false);
        //设置X轴坐标之间的最小间隔
        xAxis.setGranularity(1f);
        mChart.setDescription(desc);
    }

    private void showSaveDataDialog() {
        EditText editText = new EditText(this);
        editText.setHint("请输入要保存的数据名称");
        new AlertDialog.Builder(this)
                .setView(editText)
                .setTitle("输入名称")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DataEntity dataEntity = new DataEntity();
                         fileName = editText.getText().toString();
                        if (TextUtils.isEmpty(fileName)) {
                            Toast.makeText(MainActivity.this, "请输入数据名称", Toast.LENGTH_SHORT).show();
                            return;
                        }
//                        Toast.makeText(MainActivity.this, "上传中...", Toast.LENGTH_SHORT).show();
//                        dataEntity.setName(s);
//                        dataEntity.setList(itemEntity);
//                        dataEntity.save(new SaveListener<String>() {
//                            @Override
//                            public void done(String s, BmobException e) {
//                                if (e == null) {
//                                    Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
//                                    itemEntity.clear();
//                                    datas.clear();
//                                    initLineChar();
////                                    setNewData(datas);
//                                    mChart.clear();
//                                    tvData.setText("");
//                                }
//                            }
//                        });


                        export();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setCancelable(false)
                .show();

    }


    StringBuilder stringBuilder = new StringBuilder();
    List<String> datas = new ArrayList<>();
    List<DataEntity.ItemEntity> itemEntity = new ArrayList<>();

    List<ExeclDataEntity> execlDataEntityList = new ArrayList<>();

    @Override
    protected void onResume() {
        super.onResume();
        //回到主界面后检查是否已成功连接蓝牙设备
        if (BluetoothUtils.getBluetoothSocket() == null || mConnectedThread != null) {
            txtIsConnected.setText("未连接");
            return;
        }

        txtIsConnected.setText("已连接");

        //已连接蓝牙设备，则接收数据，并显示到接收区文本框
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case ConnectedThread.MESSAGE_READ:
                        byte[] buffer = (byte[]) msg.obj;
                        int length = msg.arg1;
                        for (int i = 0; i < length; i++) {
                            char c = (char) buffer[i];
                            if ((c <= 57 && c >= 48) || c == 46 || c == 45) {
                                if ('-' == c) {
                                    datas.add(stringBuilder.toString());
//                                    itemEntity.add(new DataEntity.ItemEntity(stringBuilder.toString(), TimeUtils.getTimeStrNow()));
                                    execlDataEntityList.add(new ExeclDataEntity(stringBuilder.toString(), TimeUtils.getTimeStrNow()));

                                    tvData.setText("");
                                    tvData.setText("当前电阻值：" + stringBuilder.toString());

                                    stringBuilder.delete(0, stringBuilder.length());
                                    setNewData(datas);


                                } else {
                                    stringBuilder.append(c);
                                }
                            }
                        }
                        break;
                }

            }
        };

        //启动蓝牙数据收发线程25.8-
        mConnectedThread = new ConnectedThread(BluetoothUtils.getBluetoothSocket(), handler);
        mConnectedThread.start();

    }


    private void setNewData(List<String> datas) {
        //1.设置x轴和y轴的点
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {

            if (datas.get(i).length() > 0) {
                try {
                    entries.add(new Entry(i, Float.parseFloat(datas.get(i))));
                } catch (Exception e) {

                }

            }

        }
        LineDataSet dataSet = new LineDataSet(entries, "电阻数据表");
        LineData lineData = new LineData(dataSet);
        dataSet.setLineWidth(3.0f);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setColor(Color.RED);
        dataSet.setCircleColor(Color.BLACK);
        mChart.setData(lineData);
        mChart.invalidate(); // refresh
    }


    /**
     * 导出表格的操作
     * "新的运行时权限机制"只在应用程序的targetSdkVersion>=23时生效，并且只在6.0系统之上有这种机制，在低于6.0的系统上应用程序和以前一样不受影响。
     * 当前应用程序的targetSdkVersion小于23（为22），系统会默认其尚未适配新的运行时权限机制，安装后将和以前一样不受影响：即用户在安装应用程序的时候默认允许所有被申明的权限
     */
    private void export() {
        if (this.getApplicationInfo().targetSdkVersion >= 23 && Build.VERSION.SDK_INT >= 23) {
            requestPermission();
        } else {
            writeExcel();
        }
    }

    /**
     * 动态请求读写权限
     */
    private void requestPermission() {
        if (!checkPermission()) {//如果没有权限则请求权限再写
            ActivityCompat.requestPermissions(this, pess, 100);
        } else {//如果有权限则直接写
            writeExcel();
        }
    }


    /**
     * 检测权限
     *
     * @return
     */
    private boolean checkPermission() {
        for (String permission : pess) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                return false;
            }
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            boolean isAllGranted = true;
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }
            if (isAllGranted) {//请求到权限了，写Excel
                writeExcel();
            } else {//权限被拒绝，不能写
                Toast.makeText(this, "读写手机存储权限被禁止，请在权限管理中心手动打开权限", Toast.LENGTH_LONG).show();
            }
        }
    }


    private String excelFilePath = "";
    private String[] colNames = new String[]{"电阻", "时间"};
    String[] pess = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    /**
     * 将数据写入excel表格
     */
    private void writeExcel() {
        if (getExternalStoragePath() == null) return;
        String savePath = null;
        try {
            savePath = isExistDir("fileStorage");
        } catch (IOException e) {
            e.printStackTrace();
        }
        excelFilePath = savePath + File.separator + fileName+".xls";
        if (checkFile(excelFilePath)) {
            deleteByPath(excelFilePath);//如果文件存在则先删除原有的文件
        }
//        File file = new File(getExternalStoragePath() + "/ExportExcel");
//        makeDir(file);
        ExcelUtil.initExcel(excelFilePath, "中文版", colNames);//需要写入权限
        ExcelUtil.writeObjListToExcel(getTravelData(), excelFilePath, this);


        execlDataEntityList.clear();
        datas.clear();
        initLineChar();
//       setNewData(datas);
        mChart.clear();
        tvData.setText("");
    }


    /**
     * @param saveDir
     * @return
     * @throws IOException
     * 判断下载目录是否存在
     */
    private String isExistDir(String saveDir) throws IOException {
        // 下载位置
        File downloadFile = new File(Environment.getExternalStorageDirectory(), saveDir);
        if (!downloadFile.mkdirs()) {
            downloadFile.createNewFile();
        }
        String savePath = downloadFile.getAbsolutePath();
        return savePath;
    }

    private ArrayList<ArrayList<String>> getTravelData() {

        ArrayList<ArrayList<String>> datas = new ArrayList<>();
        ArrayList<String> data = null;
        for (ExeclDataEntity execlDataEntity:execlDataEntityList){
            data = new ArrayList<>();
            data.clear();
            data.add(execlDataEntity.getLevel());
            data.add(execlDataEntity.getTime());
            datas.add(data);
        }

        return datas;
    }

    /**
     * 根据路径生成文件夹
     *
     * @param filePath
     */
    public static void makeDir(File filePath) {
        if (!filePath.getParentFile().exists()) {
            makeDir(filePath.getParentFile());
        }
        filePath.mkdir();
    }

    /**
     * 获取外部存储路径
     *
     * @return
     */
    public String getExternalStoragePath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
            return sdDir.toString();
        } else {
            Toast.makeText(this, "找不到外部存储路径，读写手机存储权限被禁止，请在权限管理中心手动打开权限", Toast.LENGTH_LONG).show();
            return null;
        }
    }


    /**
     * 根据文件路径检测文件是否存在,需要读取权限
     *
     * @param filePath 文件路径
     * @return true存在
     */
    private boolean checkFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isFile()) return true;
            else return false;
        } else {
            return false;
        }
    }


    /**
     * 根据文件路径删除文件
     *
     * @param filePath
     */
    private void deleteByPath(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isFile())
                file.delete();
        }
    }


}
