package com.gy.thomas.smartbt;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.gy.thomas.smartbt.ble.Code;
import com.gy.thomas.smartbt.ble.originV2.BluetoothLeInitialization;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.inuker.bluetooth.library.Code.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.STATUS_DEVICE_CONNECTED;

public class ScanActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private com.inuker.bluetooth.library.BluetoothClient mBluetoothClient;
    private ListView lsDevice;
    private List<Map<String, String>> devices = new ArrayList<>();
    private ProgressDialog mProgressDialog;
    private SimpleAdapter mSimpleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        initView();
        search();
    }

    private void initView() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mBluetoothClient = BluetoothLeInitialization.getInstance(this);
//        if(!mBluetoothClient.isBluetoothOpened())
        mBluetoothClient.openBluetooth();

        lsDevice = findViewById(R.id.ls_device);
        mSimpleAdapter = new SimpleAdapter(this, devices, R.layout.item_devices,
                new String[]{"name", "mac"}, new int[]{R.id.tv_name, R.id.tv_mac});
        lsDevice.setAdapter(mSimpleAdapter);
        lsDevice.setOnItemClickListener(this);
    }

    private void search() {
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(3000, 3)   // 先扫BLE设备3次，每次3s
                .build();
        devices.clear();
        mProgressDialog.setMessage("正在搜索蓝牙设备...");
        mProgressDialog.show();
        mBluetoothClient.search(request, new SearchResponse() {
            @Override
            public void onSearchStarted() {

            }

            @Override
            public void onDeviceFounded(SearchResult res) {

                for(Map<String,String> item :devices){
                    if(item.get("mac").equals(res.getAddress()))
                        return;
                }
                Map<String, String> device = new HashMap<>();
                device.put("name", res.getName());
                device.put("mac", res.getAddress());
                devices.add(device);
                mSimpleAdapter.notifyDataSetChanged();
            }

            @Override
            public void onSearchStopped() {

                mProgressDialog.dismiss();
            }

            @Override
            public void onSearchCanceled() {
                mProgressDialog.dismiss();
            }
        });
    }

    private void connect(final String  mac, final String name) {
        if(mBluetoothClient.getConnectStatus(mac)==STATUS_DEVICE_CONNECTED){
            startActivity(new Intent(ScanActivity.this,MainActivity.class));
            finish();
        }
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(3)   // 连接如果失败重试3次
                .setConnectTimeout(30000)   // 连接超时30s
                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                .build();
        mProgressDialog.setMessage("正在连接蓝牙设备...");
        mProgressDialog.show();
        mBluetoothClient.connect(mac,options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile profile) {
                if (code == REQUEST_SUCCESS) {
                    SharedPreferences sp = getSharedPreferences("sp",MODE_PRIVATE);
                    sp.edit().putString("mac",mac).apply();
                    sp.edit().putString("name",name).apply();
                    startActivity(new Intent(ScanActivity.this,MainActivity.class));
                    finish();
                }else {

                    Toast.makeText(ScanActivity.this, "连接失败，请重试:"+Code.toString(code), Toast.LENGTH_LONG).show();
                    mProgressDialog.dismiss();
                }
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        connect(this.devices.get(position).get("mac"),this.devices.get(position).get("name"));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        search();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothClient.stopSearch();

    }
}
