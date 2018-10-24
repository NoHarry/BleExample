package cc.noharry.bleexample;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnClickListener {

  private Button mBtnScan;
  private static final int REQUSET_CODE=100;
  private ScanCallback mScanCallback;
  private LeScanCallback mLeScanCallback;
  private BluetoothDevice mBluetoothDevice;
  private Button mBtnStopScan;
  private Button mBtnConnect;
  private Button mBtnDisconnect;
  private Button mBtnRead;
  private Button mBtnWrite;
  private Button mBtnNotify;
  private BluetoothGattCallback mBluetoothGattCallback;
  private BluetoothGatt mBluetoothGatt;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    initView();
    initCallback();
    initEvent();
    checkPermission();
  }

  @SuppressLint("NewApi")
  private void initCallback() {
    mScanCallback = new ScanCallback() {
     @Override
     public void onScanResult(int callbackType, ScanResult result) {
       L.i("onScanResult:"+" callbackType:"+callbackType+" result:"+result);
     }
   };
    mLeScanCallback = new LeScanCallback() {
      @Override
      public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        L.i("onLeScan:"+" name:"+device.getName()+" mac:"+device.getAddress()+" rssi:"+rssi);
        if ("Ble Server".equals(device.getName())){
          L.i("发现Ble Server");
          mBluetoothDevice = device;
          stopScan();
        }
      }
    };
    mBluetoothGattCallback = new BluetoothGattCallback() {
      @Override
      public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        L.i("onConnectionStateChange status:" + status + " newState:" + newState);
        mBluetoothGatt = gatt;
      }

      @Override
      public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        L.i("onServicesDiscovered status:" + status);
      }

      @Override
      public void onCharacteristicRead(BluetoothGatt gatt,
          BluetoothGattCharacteristic characteristic, int status) {
        L.i("onCharacteristicRead status:" + status + " value:"
            + byte2HexStr(characteristic.getValue()));
      }

      @Override
      public void onCharacteristicWrite(BluetoothGatt gatt,
          BluetoothGattCharacteristic characteristic, int status) {
        L.i("onCharacteristicWrite status:" + status + " value:"
            + byte2HexStr(characteristic.getValue()));
      }

      @Override
      public void onCharacteristicChanged(BluetoothGatt gatt,
          BluetoothGattCharacteristic characteristic) {
        L.i("onCharacteristicChanged value:" + byte2HexStr(characteristic.getValue()));
      }

      @Override
      public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
          int status) {
        L.i("onDescriptorRead status:" + status + " value:" + byte2HexStr(descriptor.getValue()));
      }

      @Override
      public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
          int status) {
        L.i("onDescriptorWrite status:" + status + " value:" + byte2HexStr(descriptor.getValue()));
      }
    };
  }

  private void initEvent() {
    mBtnScan.setOnClickListener(this);
    mBtnStopScan.setOnClickListener(this);
    mBtnConnect.setOnClickListener(this);
    mBtnDisconnect.setOnClickListener(this);
    mBtnRead.setOnClickListener(this);
    mBtnWrite.setOnClickListener(this);
    mBtnNotify.setOnClickListener(this);

  }

  private void initView() {
    mBtnScan = findViewById(R.id.btn_scan);
    mBtnStopScan = findViewById(R.id.btn_stop_scan);
    mBtnConnect = findViewById(R.id.btn_connect);
    mBtnDisconnect = findViewById(R.id.btn_disconnect);
    mBtnRead = findViewById(R.id.btn_read);
    mBtnWrite = findViewById(R.id.btn_write);
    mBtnNotify = findViewById(R.id.btn_notify);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()){
      case R.id.btn_scan:
        scan();
        break;
      case R.id.btn_stop_scan:
        stopScan();
        break;
      case R.id.btn_connect:
        if (mBluetoothDevice!=null){
          connect(mBluetoothDevice);
        }
        break;
      case R.id.btn_disconnect:
        disConnect();
        break;
      case R.id.btn_read:
        break;
      case R.id.btn_write:
        break;
      case R.id.btn_notify:
        break;
        default:
    }
  }

  /************************************蓝牙操作相关 开始*********************************************/

  /**
   * 新的扫描方法
   */
  @TargetApi(VERSION_CODES.LOLLIPOP)
  private void scanNew() {
    BluetoothManager bluetoothManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    ScanSettings settings=new ScanSettings
        .Builder()
        .build();
    List<ScanFilter> scanFilters=new ArrayList<>();
    bluetoothManager
        .getAdapter()
        .getBluetoothLeScanner()
        .startScan(scanFilters,settings,mScanCallback);
  }

  /**
   * 扫描(可适配低版本)
   */
  private void scan(){
    L.i("start scan");
    BluetoothManager bluetoothManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    bluetoothManager.getAdapter().startLeScan(mLeScanCallback);
  }

  private void stopScan(){
    L.e("stopScan");
    BluetoothManager bluetoothManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    bluetoothManager.getAdapter().stopLeScan(mLeScanCallback);
  }

  private void stopNewScan(){
    BluetoothManager bluetoothManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    bluetoothManager.getAdapter().getBluetoothLeScanner().stopScan(mScanCallback);
  }


  /**
   * 连接设备
   * @param device 需要连接的蓝牙设备
   */
  private void connect(BluetoothDevice device){
    device.connectGatt(this, false,mBluetoothGattCallback);
  }

  private void disConnect(){
    if (mBluetoothGatt!=null){
      mBluetoothGatt.disconnect();
      mBluetoothGatt.close();
    }
  }

  /************************************蓝牙操作相关 结束*********************************************/


  /*************************************开启蓝牙相关 开始**************************************************/

  /**
   * 判断蓝牙是否开启
   * @return
   */
  public boolean isEnable(){
    BluetoothManager bluetoothManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
    return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
  }

  /**
   * 打开蓝牙
   * @param listener
   * @return
   */
  public boolean openBT(OnBTOpenStateListener listener){
    btOpenStateListener=listener;
    BTStateReceiver receiver=new BTStateReceiver();
    BluetoothManager bluetoothManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
    registerBtStateReceiver(this,receiver);
    if (mBluetoothAdapter.isEnabled()){
      btOpenStateListener.onBTOpen();
      return true;
    }
    if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()){
      return mBluetoothAdapter.enable();
    }
    return false;
  }

  private void registerBtStateReceiver(Context context,BTStateReceiver btStateReceiver) {
    IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    context.registerReceiver(btStateReceiver, filter);
  }

  private void unRegisterBtStateReceiver(Context context,BTStateReceiver btStateReceiver) {
    try {
      context.unregisterReceiver(btStateReceiver);
    } catch (Exception e) {
      e.printStackTrace();
    } catch (Throwable e) {
      e.printStackTrace();
    }

  }

  private OnBTOpenStateListener btOpenStateListener = null;

  public interface OnBTOpenStateListener {
    void onBTOpen();
  }

  private class BTStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context arg0, Intent intent) {
      String action = intent.getAction();
      L.i("action=" + action);
      if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
        int state = intent
            .getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
        L.i("state=" + state);
        switch (state) {
          case BluetoothAdapter.STATE_TURNING_ON:
            L.i("ACTION_STATE_CHANGED:  STATE_TURNING_ON");
            break;
          case BluetoothAdapter.STATE_ON:
            L.i("ACTION_STATE_CHANGED:  STATE_ON");
            if (null != btOpenStateListener){
              btOpenStateListener.onBTOpen();
            }
            unRegisterBtStateReceiver(MainActivity.this,this);
            break;
          default:
        }
      }
    }
  }

  /*************************************开启蓝牙相关 结束**************************************************/








  // 执行权限检查的方法
  private void checkPermission() {
    if ((ContextCompat.checkSelfPermission(this,
        permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED)) {
      ActivityCompat.requestPermissions(this,
          new String[]{permission.ACCESS_COARSE_LOCATION},
          REQUSET_CODE);
    }
  }


  @Override
  public void onRequestPermissionsResult(int requestCode,
      String permissions[], int[] grantResults) {
    switch (requestCode) {
      case REQUSET_CODE: {
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
           ) {
          L.i("权限请求成功");

        } else {
          L.i("权限请求失败");

        }
        return;
      }
    }
  }


  public  String byte2HexStr(byte[] value){
    char[] chars = "0123456789ABCDEF".toCharArray();
    StringBuilder sb = new StringBuilder("");
    int bit;

    for (int i = 0; i < value.length; i++) {
      bit = (value[i] & 0x0F0) >> 4;
      sb.append(chars[bit]);
      bit = value[i] & 0x0F;
      sb.append(chars[bit]);
      if (i!=value.length-1){
        sb.append('-');
      }

    }
    return "(0x) "+sb.toString().trim();
  }

}
