package com.gl.BluetoothPrint;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Created by waiting on 2017/12/21.
 */

public class BluetoothPrintControl {

    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static BluetoothPrintControl mBluetoothPrintControl;

    private int mState;
    private BluetoothAdapter mAdapter;
    private Handler mHandler;
    private ConnectThread mConnectThread;//作为客户端主动连接线程
    private AcceptThread mAcceptThread;//作为服务端accept等待连接线程
    private ConnectedThread mConnectedThread;//数据读写线程

    public static BluetoothPrintControl getInstance() {
        if (mBluetoothPrintControl == null) {
            mBluetoothPrintControl = new BluetoothPrintControl();
        }
        return mBluetoothPrintControl;
    }

    public BluetoothPrintControl() {
        this.mState = 0;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    public synchronized int getState() {
        return this.mState;
    }

    public boolean IsNoConnection() {
        return this.mState != 3;
    }


    private void sendMessageToMainThread(int flag) {
        if (this.mHandler != null) {
            Message message = this.mHandler.obtainMessage();
            Bundle data = new Bundle();
            data.putInt("flag", flag);
            message.setData(data);
            this.mHandler.sendMessage(message);
        }
    }

    private void sendMessageToMainThread(int flag, int state) {
        if (this.mHandler != null) {
            Message message = this.mHandler.obtainMessage();
            Bundle data = new Bundle();
            data.putInt("flag", flag);
            data.putInt("state", state);
            message.setData(data);
            this.mHandler.sendMessage(message);
        }
    }

    private synchronized void setState(int state) {
        System.out.println("setState()= " + this.mState + " -> " + state);
        this.mState = state;
        switch (this.mState) {
            case 0:
                sendMessageToMainThread(32, 16);
                break;
            case 3:
                sendMessageToMainThread(32, 17);
        }
    }

    public synchronized void start() {
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        if (this.mAcceptThread == null) {
            this.mAcceptThread = new AcceptThread();
            this.mAcceptThread.start();
        }
        setState(1);
    }

    public synchronized void stop() {
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }
        if (this.mAcceptThread != null) {
            this.mAcceptThread.cancel();
            this.mAcceptThread = null;
        }
        setState(0);
    }


    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        if (this.mAcceptThread != null) {
            this.mAcceptThread.cancel();
            this.mAcceptThread = null;
        }

        this.mConnectedThread = new ConnectedThread(socket);
        this.mConnectedThread.start();

        sendMessageToMainThread(34);
        setState(3);
    }

    public synchronized void connect(BluetoothDevice device) {
        if ((this.mState == 2) &&
                (this.mConnectThread != null)) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        this.mConnectThread = new ConnectThread(device);
        this.mConnectThread.start();
        setState(2);
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                if(mAdapter != null && mAdapter.isEnabled()){
                    tmp = BluetoothPrintControl.this.mAdapter.
                            listenUsingRfcommWithServiceRecord("BluetoothChatService", BluetoothPrintControl.this.MY_UUID);
                }
            } catch (Exception e) {
            }
            this.mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket = null;
            while (BluetoothPrintControl.this.mState != 3 && mAdapter != null && mAdapter.isEnabled()) {
                try {
                    socket = this.mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                if (socket != null)
                    synchronized (BluetoothPrintControl.this) {
                        switch (BluetoothPrintControl.this.mState) {
                            case 1:
                            case 2:
                                Log.e("AcceptThread","AcceptThread------connected");
                                BluetoothPrintControl.this.connected(socket, socket.getRemoteDevice());
                                break;
                            case 0:
                            case 3:
                                try {
                                    socket.close();
                                } catch (IOException localIOException1) {
                                }
                        }
                    }
            }
        }

        public void cancel() {
            try {
                if(mAdapter != null && mAdapter.isEnabled() && mmServerSocket!=null){
                    this.mmServerSocket.close();
                }
            } catch (IOException localIOException) {
            }
        }
    }




    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private  InputStream mmInStream ;
        private  OutputStream mmOutStream ;

        public ConnectedThread(BluetoothSocket socket) {
            this.mmSocket = socket;
            //HsBluetoothPrintDriver.this.pBluetoothSocket = socket;
            try {
                this.mmInStream = socket.getInputStream();
                this.mmOutStream = socket.getOutputStream();
            } catch (IOException localIOException) {

            }

        }

        public void write(byte[] buffer) {
            try {
                if(mAdapter != null && mAdapter.isEnabled() && mmOutStream !=null){
                    this.mmOutStream.write(buffer);
                }
            } catch (IOException localIOException) {
            }
        }

        public void write(byte[] buffer, int dataLen) {
            try {
                if(mAdapter != null && mAdapter.isEnabled() && mmOutStream !=null) {
                    for (int i = 0; i < dataLen; i++)
                        this.mmOutStream.write(buffer[i]);
                }
            } catch (IOException localIOException) {
            }
        }

        public void cancel() {
            try {
                if(mAdapter != null && mAdapter.isEnabled() && mmSocket!=null) {
                    this.mmSocket.close();
                }
            } catch (IOException localIOException) {
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            this.mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(BluetoothPrintControl.this.MY_UUID);
            } catch (Exception localException) {
            }

            this.mmSocket = tmp;
        }

        public void run() {
            setName("ConnectThread");

            BluetoothPrintControl.this.mAdapter.cancelDiscovery();
            try {
                this.mmSocket.connect();
            } catch (IOException e) {
                BluetoothPrintControl.this.sendMessageToMainThread(33);
                try {
                    this.mmSocket.close();
                } catch (IOException localIOException1) {
                }
                BluetoothPrintControl.this.start();
                return;
            }

            synchronized (BluetoothPrintControl.this) {
                BluetoothPrintControl.this.mConnectThread = null;
            }

            BluetoothPrintControl.this.connected(this.mmSocket, this.mmDevice);
            //  StartReadStatu(mmSocket);
        }

        public void cancel() {
            try {
                if(mAdapter != null && mAdapter.isEnabled()&& mmSocket!=null) {
                    this.mmSocket.close();
                }
            } catch (IOException localIOException) {
            }
        }
    }

    public void WriteString(String dataString) {
        byte[] data = null;
        if (this.mState != 3) return;
        ConnectedThread r = this.mConnectedThread;
        try {
            data = dataString.getBytes("GBK");
            r.write(data);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    public void WriteString(String dataString,String format) {
        byte[] data = null;
        if (this.mState != 3) return;
        ConnectedThread r = this.mConnectedThread;
        try {
            data = dataString.getBytes(format);
            r.write(data);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    public void WriteByte(byte[] out) {
        if (this.mState != 3) return;
        ConnectedThread r = this.mConnectedThread;
        r.write(out);
    }

}
