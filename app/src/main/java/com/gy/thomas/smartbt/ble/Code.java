package com.gy.thomas.smartbt.ble;

/**
 * Created by dingjikerbo on 2016/10/10.
 */
public class Code {

    public static final int REQUEST_SUCCESS = 0;
    public static final int REQUEST_FAILED = -1;
    public static final int REQUEST_CANCELED = -2;
    public static final int ILLEGAL_ARGUMENT = -3;
    public static final int BLE_NOT_SUPPORTED = -4;
    public static final int BLUETOOTH_DISABLED = -5;
    public static final int SERVICE_UNREADY = -6;
    public static final int REQUEST_TIMEDOUT = -7;
    public static final int REQUEST_OVERFLOW = -8;
    public static final int REQUEST_DENIED = -9;
    public static final int REQUEST_EXCEPTION = -10;
    public static final int REQUEST_UNKNOWN = -11;

    public static String toString(int code) {
        switch (code) {
            case REQUEST_SUCCESS:
                return "请求成功";
            case REQUEST_FAILED:
                return "请求失败";
            case ILLEGAL_ARGUMENT:
                return "非法的参数";
            case BLE_NOT_SUPPORTED:
                return "不支持的蓝牙设备";
            case BLUETOOTH_DISABLED:
                return "蓝牙不可用，请先开启手机蓝牙";
            case SERVICE_UNREADY:
                return "服务没有准备好";
            case REQUEST_TIMEDOUT:
                return "请求超时";
            case REQUEST_DENIED:
                return "请求拒绝";
            default:
                return "未知的异常: " + code;
        }
    }
}
