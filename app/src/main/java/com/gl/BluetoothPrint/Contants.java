package com.gl.BluetoothPrint;

/**
 * Created by waiting on 2017/12/21.
 */

public class Contants {

    //主动连接结果
    public static final int FLAG_STATE_CHANGE = 32;
    public static final int FLAG_FAIL_CONNECT = 33;
    public static final int FLAG_SUCCESS_CONNECT = 34;

    //服务端等待连接结果
    private static final int STATE_NONE = 0;
    private static final int STATE_LISTEN = 1;
    private static final int STATE_CONNECTING = 2;
    private static final int STATE_CONNECTED = 3;

}
