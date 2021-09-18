package com.zhuandian.bluetooth.ble;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.zhuandian.bluetooth.DataEntity;
import com.zhuandian.bluetooth.ExcelUtil;
import com.zhuandian.bluetooth.ExeclDataEntity;
import com.zhuandian.bluetooth.R;
import com.zhuandian.bluetooth.TimeUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {


    private TextView txtIsConnected;
    private BluetoothAdapter mBtAdapter = null;
    private TextView tvData;
    private LineChart mChart;
    private String fileName;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int UART_PROFILE_CONNECTED = 20;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService2 mService = null;
    private BluetoothDevice mDevice = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtIsConnected = (TextView) findViewById(R.id.txtIsConnected);
        mChart = (LineChart) findViewById(R.id.chart1);
//        mChart.setOnChartGestureListener(this);
//        mChart.setOnChartValueSelectedListener(this);

        initLineChar();
        tvData = (TextView) findViewById(R.id.tv_data);

        requestPermission();
        Init_service();

    }


    private void Init_service() {
        System.out.println("Init_service");
        Intent bindIntent = new Intent(this, UartService2.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        registerReceiver(UARTStatusChangeReceiver,
                makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService2.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService2.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService2.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService2.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService2.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.device_list:
                // 创建一个蓝牙适配器对象
                mBtAdapter = BluetoothAdapter.getDefaultAdapter();
                // 如果未打开蓝牙就弹出提示对话框提示用户打开蓝牙
                if (!mBtAdapter.isEnabled()) {
                    Toast.makeText(this, "对不起，蓝牙还没有打开", Toast.LENGTH_SHORT).show();
                    System.out.println("蓝牙还没有打开");
                    // 弹出请求打开蓝牙对话框
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    /**
                     * 当"scan"按钮点击后，进入DeviceListActivity.class类，弹出该类对应的窗口
                     * ，并自动在窗口内搜索周围的蓝牙设备
                     */
                    Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                }
                break;
            case R.id.save_data:
                showSaveDataDialog();
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_SELECT_DEVICE:
                // 如果选择搜索到的蓝牙设备页面操作成功（即选择远程设备成功，并返回所选择的远程设备地址信息）
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                    System.out.println("远程蓝牙Address：" + mDevice);
                    System.out.println("mserviceValue:" + mService);
                    boolean isconnected = mService.connect(deviceAddress);
                    System.out.println("已连接吗？" + isconnected);
                }
                break;
            case REQUEST_ENABLE_BT:
                // 如果请求打开蓝牙页面操作成功（蓝牙成功打开）
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "蓝牙已经成功打开", Toast.LENGTH_SHORT).show();
                } else {
                    // 请求打开蓝牙页面操作不成功（蓝牙为打开或者打开错误）
                    // Log.d(TAG, "蓝牙未打开");
                    System.out.println("蓝牙未打开");
                    Toast.makeText(this, "打开蓝牙时发生错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                System.out.println("wrong request code");
                break;
        }
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


    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final Intent mIntent = intent;
            // 建立连接
            if (action.equals(UartService2.ACTION_GATT_CONNECTED)) {
                System.out.println("BroadcastReceiver:ACTION_GATT_CONNECTED");
                txtIsConnected.setText("已建立连接");
                String currentDateTimeString = java.text.DateFormat.getTimeInstance().format(new Date());
//                btnScan.setText("断开");
//                editText_sendMessage.setEnabled(true);
//                checkBox_autoSend.setEnabled(true);
//                editText_sendIntervalVal.setEnabled(true);
//                btnSend.setEnabled(true);
//                listAdapter.add("[" + currentDateTimeString + "] Connected to: " + mDevice.getName());
//                messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                mState = UART_PROFILE_CONNECTED;
            }
            // 断开连接
            if (action.equals(UartService2.ACTION_GATT_DISCONNECTED)) {
                System.out.println("BroadcastReceiver:ACTION_GATT_DISCONNECTED");
                txtIsConnected.setText("已断开连接");
                String currentDateTimeString = java.text.DateFormat.getTimeInstance().format(new Date());
//                btnScan.setText("搜索");
//                editText_sendMessage.setEnabled(false);
//                checkBox_autoSend.setEnabled(false);
//                editText_sendIntervalVal.setEnabled(false);
//                btnSend.setEnabled(false);
//                listAdapter.add("[" + currentDateTimeString + "] Disconnected to: " + mDevice.getName());
                mState = UART_PROFILE_DISCONNECTED;
                mService.close();
            }
            // 有数据可以接收
            if ((action.equals(UartService2.ACTION_DATA_AVAILABLE))) {
                byte[] rxValue = intent.getByteArrayExtra(UartService2.EXTRA_DATA);
//                if (radioReASCII.isChecked()) {
                if (true) {
                    //发送ASCII
                    try {
                        String Rx_str = new String(rxValue, "UTF-8");
//                        listAdapter.add("[" + java.text.DateFormat.getTimeInstance().format(new Date()) + "] RX: " + Rx_str);
//                        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);

                        //数据：R=12-
//                        toastMessage("数据："+Rx_str);
                        String data = Rx_str.trim().replace("R=","");




                        byte[] buffer = (byte[]) data.getBytes();
                        int length = data.length();
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


                    } catch (Exception e) {
                        System.out.println(e.toString());
                    }
                } else {
                    //发送HEX
                    String Rx_str = "";
                    for (int i = 0; i < rxValue.length; i++) {
                        if (rxValue[i] >= 0)
                            Rx_str = Rx_str + Integer.toHexString(rxValue[i]) + " ";
                        else
                            Rx_str = Rx_str + Integer.toHexString(rxValue[i] & 0x0ff) + " ";
                    }
//                    listAdapter.add("[" + DateFormat.getTimeInstance().format(new Date()) + "] RX: " + Rx_str);
//                    messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                }

            }
            // 未知功能1
            if (action.equals(UartService2.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }
            // 未知功能2
            if (action.equals(UartService2.DEVICE_DOES_NOT_SUPPORT_UART)) {
                toastMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }
        }
    };
    // UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        // 与UART服务的连接建立
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService2.LocalBinder) rawBinder).getService();
            System.out.println("uart服务对象：" + mService);
            if (!mService.initialize()) {
                System.out.println("创建蓝牙适配器失败");
                // 因为创建蓝牙适配器失败，导致下面的工作无法进展，所以需要关闭当前uart服务
                finish();
            }
        }

        // 与UART服务的连接失去
        public void onServiceDisconnected(ComponentName classname) {
            // mService.disconnect(mDevice);
            mService = null;
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("onDestroy");
        try {
            // 解注册广播过滤器
            unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            System.out.println(ignore.toString());
        }
        // 解绑定服务
        unbindService(mServiceConnection);
        // 关闭服务对象
        mService.stopSelf();
        mService = null;
    }

    private void toastMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        //回到主界面后检查是否已成功连接蓝牙设备
//        if (BluetoothUtils.getBluetoothSocket() == null || mConnectedThread != null) {
//            txtIsConnected.setText("未连接");
//            return;
//        }
//
//        txtIsConnected.setText("已连接");
//
//        //已连接蓝牙设备，则接收数据，并显示到接收区文本框
//        Handler handler = new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                super.handleMessage(msg);
//                switch (msg.what) {
//                    case ConnectedThread.MESSAGE_READ:
//                        byte[] buffer = (byte[]) msg.obj;
//                        int length = msg.arg1;
//                        for (int i = 0; i < length; i++) {
//                            char c = (char) buffer[i];
//                            if ((c <= 57 && c >= 48) || c == 46 || c == 45) {
//                                if ('-' == c) {
//                                    datas.add(stringBuilder.toString());
////                                    itemEntity.add(new DataEntity.ItemEntity(stringBuilder.toString(), TimeUtils.getTimeStrNow()));
//                                    execlDataEntityList.add(new ExeclDataEntity(stringBuilder.toString(), TimeUtils.getTimeStrNow()));
//
//                                    tvData.setText("");
//                                    tvData.setText("当前电阻值：" + stringBuilder.toString());
//
//                                    stringBuilder.delete(0, stringBuilder.length());
//                                    setNewData(datas);
//
//
//                                } else {
//                                    stringBuilder.append(c);
//                                }
//                            }
//                        }
//                        break;
//                }
//
//            }
//        };
//
//        //启动蓝牙数据收发线程25.8-
//        mConnectedThread = new ConnectedThread(BluetoothUtils.getBluetoothSocket(), handler);
//        mConnectedThread.start();
//
//    }


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

        //如果没有位置权限就申请一下
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            //申请权限
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            //申请权限
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
        }
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
            savePath = isExistDir("BLE_DATA");
        } catch (IOException e) {
            e.printStackTrace();
        }
        excelFilePath = savePath + File.separator + fileName + ".xls";
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
     * @throws IOException 判断下载目录是否存在
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
        for (ExeclDataEntity execlDataEntity : execlDataEntityList) {
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
