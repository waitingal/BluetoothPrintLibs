package com.gl.BluetoothPrint;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by waiting on 2017/12/21.
 */

public class PrintManage {

    private static PrintManage instance = null;
    private Context mContext;
    private ConnStateHandler connStateHandler;
    private BluetoothDeviceLintent mBluetoothDeviceLintent;//蓝牙状态监听
    private Map<Integer,BlePrintConnListener> mConnListenerList;
    private PrintManage(Context c){
        mContext = c.getApplicationContext();
        InitConnStateHandler();
        BluetoothRegister();
    }

    public static PrintManage getInstance(Context c) {
        if(instance == null)
            instance = new PrintManage(c);
        return instance;
    }

    /**
     * 销毁
     */
    public void ManageDestroy(){
        UnregisterBluetooth();
        if(mConnListenerList!=null && mConnListenerList.size()>0){
            mConnListenerList.clear();
            mConnListenerList=null;
        }
        if(connStateHandler!=null){
            connStateHandler.removeCallbacksAndMessages(null);
        }
    }

    public class ConnStateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            switch (data.getInt("flag")) {
                //连接失败时接收到消息
                case Contants.FLAG_FAIL_CONNECT:
                    ConnNotice(Contants.FLAG_FAIL_CONNECT);
                    break;
                //连接成功时接收到消息
                case Contants.FLAG_SUCCESS_CONNECT:
                    ConnNotice(Contants.FLAG_SUCCESS_CONNECT);
                    break;

            }
        }
    }

    public void InitConnStateHandler(){
        if(connStateHandler!=null){
            connStateHandler.removeCallbacksAndMessages(null);
            connStateHandler=null;
        }
        connStateHandler = new ConnStateHandler();
        BluetoothPrintControl.getInstance().setHandler(connStateHandler);
    }

    private void ConnNotice(int tag){
        if(mConnListenerList!=null && mConnListenerList.size()>0){
            for (Integer i : mConnListenerList.keySet()){
                BlePrintConnListener listener=mConnListenerList.get(i);
                if(tag == Contants.FLAG_SUCCESS_CONNECT){
                    listener.ConnectSuccess();
                }else if(tag == Contants.FLAG_FAIL_CONNECT){
                    listener.ConnectFail();
                }
            }
        }
    }

    /**
     * 注册广播监听蓝牙开关
     */
    public void BluetoothRegister(){
        mBluetoothDeviceLintent = new BluetoothDeviceLintent();
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);//蓝牙连接上
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);//蓝牙断开连接
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//蓝牙开启关闭
        mContext.registerReceiver(mBluetoothDeviceLintent,intentFilter);
    }

    /**
     * 反注册广播监听蓝牙开关
     */
    private void  UnregisterBluetooth(){
        if(mBluetoothDeviceLintent!=null){
            mContext.unregisterReceiver(mBluetoothDeviceLintent);
        }

    }

    /**
     * 连接设备
     * @param device
     */
    public void ConnDevice(BluetoothDevice device){
        if(device == null){
            return;
        }
        BluetoothPrintControl mBluetoothPrintControl = BluetoothPrintControl.getInstance();
        mBluetoothPrintControl.stop();

        mBluetoothPrintControl.start();
        mBluetoothPrintControl.connect(device);
    }

    /**
     * 停止连接设备
     */
    public void StopDevice(){
        BluetoothPrintControl mBluetoothPrintControl = BluetoothPrintControl.getInstance();
        mBluetoothPrintControl.stop();
    }

    /**
     * 是否已连接
     * true: 没有连接
     * @return
     */
    public boolean CheckDeviceConnStatu(){
        BluetoothPrintControl mBluetoothPrintControl = BluetoothPrintControl.getInstance();
        boolean flag=mBluetoothPrintControl.IsNoConnection();
        return flag;
    }

//    /**
//     * 检测设备状态
//     * @param handler
//     * @param what
//     */
//    public void CheckDeviceStatus(Handler handler,int what){
//        BluetoothPrintControl mBluetoothPrintControl = BluetoothPrintControl.getInstance();
//        mBluetoothPrintControl.HandlerStatusCheck(handler,what);
//    }

    /**
     * 添加连接监听
     * @param listener
     */
    public void AddConnListener(BlePrintConnListener listener){
        if(listener == null){
            return;
        }
        if(mConnListenerList == null){
            mConnListenerList = new HashMap<Integer,BlePrintConnListener>();
        }
        mConnListenerList.put(listener.hashCode(),listener);
    }

    /**
     * 移除连接监听
     * @param listener
     */
    public void RemoveConnListener(BlePrintConnListener listener){
        if(listener == null || mConnListenerList == null || mConnListenerList.size()<=0){
            return;
        }
        int hashCode=listener.hashCode();
        if(mConnListenerList.containsKey(hashCode)){
            mConnListenerList.remove(hashCode);
        }
    }

    private class BluetoothDeviceLintent extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){//蓝牙连接上
            }else if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){//蓝牙断开连接
                StopDevice();
            }else if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){//蓝牙开启关闭
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF://蓝牙关闭
                        StopDevice();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF://蓝牙正在关闭
                        StopDevice();
                        break;
                    case BluetoothAdapter.STATE_ON://蓝牙开启
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON://蓝牙正在开启
                        break;
                }
            }
        }
    }

    public void SendString(String data){
        BluetoothPrintControl mBluetoothPrintControl = BluetoothPrintControl.getInstance();
        mBluetoothPrintControl.WriteString(data);
    }

    public void SendString(String data,String format){
        BluetoothPrintControl mBluetoothPrintControl = BluetoothPrintControl.getInstance();
        mBluetoothPrintControl.WriteString(data,format);
    }

    public void SendByte(byte[] out){
        BluetoothPrintControl mBluetoothPrintControl = BluetoothPrintControl.getInstance();
        mBluetoothPrintControl.WriteByte(out);
    }
}
