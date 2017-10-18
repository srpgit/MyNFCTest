package com.gzrj.test.nfc.mynfctest;

import android.webkit.JavascriptInterface;

/**
 * java传递数据给js页面
 * Created by RP_S on 2017/10/17.
 */

public class TP {
    public String data = "initial";

    public TP(String data) {
        this.data = data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @JavascriptInterface
    public String getData() {
        return this.data;
    }
}
