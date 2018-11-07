package cc.noharry.bleexample;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private final static UUID UUID_SERVER=UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb");
  private final static UUID UUID_CHARREAD=UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb");
  private final static UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
  private final static UUID UUID_CHARWRITE=UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
  private final static UUID UUID_ADV_SERVER=UUID.fromString("0000ffe3-0000-1000-8000-00805f9b34fb");
  private BluetoothGattCharacteristic mCharacteristic;
  private Button mBtnDisableNotify;
  private TextView mTvScanState;
  private TextView mTvConnState;
  private TextView mTvReadData;
  private TextView mTvWriteData;
  private TextView mTvNotifyData;
  private EditText mEtWrite;
  private AtomicBoolean isScanning=new AtomicBoolean(false);
  private Handler mHandler=new Handler();
  private static final int REQUEST_ENABLE_BT=100;

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
        if (isScanning.get()){
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mTvScanState.setText("扫描中");
            }
          });
        }

       if ("Ble Server".equals(result.getDevice().getName())){
         L.i("发现Ble Server");
         mBluetoothDevice = result.getDevice();
         stopNewScan();
       }
     }
   };
    mLeScanCallback = new LeScanCallback() {
      @Override
      public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        L.i("onLeScan:"+" name:"+device.getName()+" mac:"+device.getAddress()+" rssi:"+rssi);
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            mTvScanState.setText("扫描中");
          }
        });
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
        if (newState==BluetoothProfile.STATE_DISCONNECTED){
          L.i("STATE_DISCONNECTED");

          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mTvConnState.setText("断开连接");
            }
          });
          gatt.close();
        }else if (newState==BluetoothProfile.STATE_CONNECTED){
          L.i("STATE_CONNECTED");
          L.i("start discoverServices");
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mTvConnState.setText("已连接");
            }
          });

          gatt.discoverServices();
        }else {
          mTvConnState.setText(newState);
        }
      }

      @Override
      public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        L.i("onServicesDiscovered status:" + status);
        BluetoothGattService service = gatt.getService(UUID_SERVER);
        if (service!=null){
          mCharacteristic = service.getCharacteristic(UUID_CHARWRITE);
          if (mCharacteristic !=null){
            L.i("获取到目标特征");
          }
        }
      }

      @Override
      public void onCharacteristicRead(BluetoothGatt gatt,
          final BluetoothGattCharacteristic characteristic, final int status) {
        L.i("onCharacteristicRead status:" + status + " value:"
            + byte2HexStr(characteristic.getValue()));
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            mTvReadData.setText("statu:"+status+" hexValue:"+byte2HexStr(characteristic.getValue())+" ,str:"
                +new String(characteristic.getValue()));
          }
        });

      }

      @Override
      public void onCharacteristicWrite(BluetoothGatt gatt,
          final BluetoothGattCharacteristic characteristic, final int status) {
        L.i("onCharacteristicWrite status:" + status + " value:"
            + byte2HexStr(characteristic.getValue()));
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            mTvWriteData.setText("statu:"+status+" hexValue:"+byte2HexStr(characteristic.getValue())+" ,str:"
                +new String(characteristic.getValue()));
          }
        });

      }

      @Override
      public void onCharacteristicChanged(BluetoothGatt gatt,
          final BluetoothGattCharacteristic characteristic) {
        L.i("onCharacteristicChanged value:" + byte2HexStr(characteristic.getValue()));
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            mTvNotifyData.setText(" hexValue:"+byte2HexStr(characteristic.getValue())+" ,str:"
                +new String(characteristic.getValue()));
          }
        });

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
    mBtnDisableNotify.setOnClickListener(this);
  }

  private void initView() {
    mBtnScan = findViewById(R.id.btn_scan);
    mBtnStopScan = findViewById(R.id.btn_stop_scan);
    mBtnConnect = findViewById(R.id.btn_connect);
    mBtnDisconnect = findViewById(R.id.btn_disconnect);
    mBtnRead = findViewById(R.id.btn_read);
    mBtnWrite = findViewById(R.id.btn_write);
    mBtnNotify = findViewById(R.id.btn_notify);
    mBtnDisableNotify = findViewById(R.id.btn_disable_notify);
    mTvScanState = findViewById(R.id.tv_scan_state);
    mTvConnState = findViewById(R.id.tv_connect_state);
    mTvReadData = findViewById(R.id.tv_read_data);
    mTvWriteData = findViewById(R.id.tv_write_data);
    mTvNotifyData = findViewById(R.id.tv_notify_data);
    mEtWrite = findViewById(R.id.et_write);
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
        read();
        break;
      case R.id.btn_write:
        String data = mEtWrite.getText().toString().trim();
        write(data.getBytes());
        break;
      case R.id.btn_notify:
        enableNotify();
        break;
      case R.id.btn_disable_notify:
        disableNotify();
        break;
        default:
    }
  }

  /************************************蓝牙操作相关 开始*********************************************/

  /**
   * 新的扫描方法
   */
  private void scanNew() {
    mTvScanState.setText("开始扫描");
    BluetoothManager bluetoothManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    ScanSettings settings=new ScanSettings
        .Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
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
    mTvScanState.setText("开始扫描");
    BluetoothManager bluetoothManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//    UUID[] uuids=new UUID[]{UUID_ADV_SERVER};
    bluetoothManager.getAdapter().startLeScan(/*uuids,*/mLeScanCallback);
  }

  private void stopScan(){
    L.e("stopScan");
    BluetoothManager bluetoothManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    bluetoothManager.getAdapter().stopLeScan(mLeScanCallback);
    //扫描真正停止很多时候有点延迟
    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        mTvScanState.setText("停止扫描");
      }
    },500);

  }

  private void stopNewScan(){
    BluetoothManager bluetoothManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    bluetoothManager.getAdapter().getBluetoothLeScanner().stopScan(mScanCallback);
    //扫描真正停止很多时候有点延迟
    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        mTvScanState.setText("停止扫描");
      }
    },500);
  }


  /**
   * 连接设备
   * @param device 需要连接的蓝牙设备
   */
  private void connect(BluetoothDevice device){
    mBluetoothGatt = device.connectGatt(this, false, mBluetoothGattCallback);
  }

  /**
   * 断开连接
   */
  private void disConnect(){
    if (mBluetoothGatt!=null){
      mBluetoothGatt.disconnect();
//      mBluetoothGatt.close();
    }
  }

  /**
   * 读特征
   */
  private void read(){
    if (mBluetoothGatt!=null&&mCharacteristic!=null){
      L.i("开始读 uuid："+mCharacteristic.getUuid().toString());
      mBluetoothGatt.readCharacteristic(mCharacteristic);
    }else {
      L.e("读失败！");
    }
  }

  /**
   * 写特征
   * @param data 最大20byte
   */
  private void write(byte[] data){
    if (mBluetoothGatt!=null&&mCharacteristic!=null){
      L.i("开始写 uuid："+mCharacteristic.getUuid().toString()+" hex:"+byte2HexStr(data)+" str:"+new String(data));

      mCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

//      mCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

      mCharacteristic.setValue(data);
      mBluetoothGatt.writeCharacteristic(mCharacteristic);
    }else {
      L.e("写失败");
    }
  }

  /**
   * 开启通知
   */
  private void enableNotify(){
    if (mBluetoothGatt!=null&&mCharacteristic!=null){
      BluetoothGattDescriptor descriptor = mCharacteristic
          .getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
      boolean local = mBluetoothGatt.setCharacteristicNotification(mCharacteristic, true);
      L.i("中央设备开启通知 结果:"+local);
      if (descriptor!=null){
        int parentWriteType = mCharacteristic.getWriteType();
        mCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        boolean remote = mBluetoothGatt.writeDescriptor(descriptor);
        mCharacteristic.setWriteType(parentWriteType);
        L.i("外围设备开启通知 结果:"+remote);
      }
    }else {
      L.e("开启通知失败");
    }
  }

  /**
   * 关闭通知
   */
  private void disableNotify(){
    if (mBluetoothGatt!=null&&mCharacteristic!=null){
      BluetoothGattDescriptor descriptor = mCharacteristic
          .getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
      boolean local = mBluetoothGatt.setCharacteristicNotification(mCharacteristic, false);
      L.i("中央设备关闭通知 结果:"+local);
      if (descriptor!=null){
        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        boolean remote = mBluetoothGatt.writeDescriptor(descriptor);
        L.i("外围设备关闭通知 结果:"+remote);
      }
    }else {
      L.e("关闭通知失败");
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

  public void openBtByUser(){
    BluetoothManager bluetoothManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
    if (!mBluetoothAdapter.isEnabled()) {

      Intent enableBtIntent = new Intent(
          BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
  }



  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode){
      case REQUEST_ENABLE_BT:
        if (resultCode==Activity.RESULT_OK){
          L.i("打开蓝牙成功");
        }else if (resultCode==Activity.RESULT_CANCELED){
          L.i("打开蓝牙失败");
        }
        break;
    }
  }

  /**
   * 打开蓝牙
   * @param listener
   * @return
   */
  public boolean openBt(OnBTOpenStateListener listener){
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

  /**
   * 用于监听蓝牙开启状态广播
   */
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
