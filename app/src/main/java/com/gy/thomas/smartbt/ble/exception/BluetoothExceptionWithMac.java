package com.gy.thomas.smartbt.ble.exception;

public class BluetoothExceptionWithMac extends BluetoothException {

    String mac;

    public String getMac() {
        return mac;
    }

    public BluetoothExceptionWithMac(String msg, String mac) {
        super(msg);
        this.mac = mac;
    }

}
