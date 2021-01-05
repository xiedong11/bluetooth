package com.zhuandian.bluetooth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;

    private TextView txtIsConnected;

    private Button btnPairedDevices;

    private BluetoothAdapter mBluetoothAdapter;
    private ConnectedThread mConnectedThread;
    private TextView tvData;
    private LineChart mChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtIsConnected = (TextView) findViewById(R.id.txtIsConnected);

        mChart = (LineChart) findViewById(R.id.chart1);
//        mChart.setOnChartGestureListener(this);
//        mChart.setOnChartValueSelectedListener(this);

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


    StringBuilder stringBuilder = new StringBuilder();
    List<String> datas = new ArrayList<>();

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
        LineDataSet dataSet = new LineDataSet(entries, "电阻");
        LineData lineData = new LineData(dataSet);
        mChart.setData(lineData);
        mChart.invalidate(); // refresh
    }

}
