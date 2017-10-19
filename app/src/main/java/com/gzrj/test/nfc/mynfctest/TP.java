package com.gzrj.test.nfc.mynfctest;

import android.webkit.JavascriptInterface;

/**
 * 通过此类进行java与js的交互
 * Created by RP_S on 2017/10/17.
 */

public class TP {
    private String data;

    private NfcVUtil nfcVUtil;

    public TP() {

    }

    @JavascriptInterface
    public void setData(String data) {
        this.data = data;
    }

    @JavascriptInterface
    public String getData() {
        return this.data;
    }

    @JavascriptInterface
    public NfcVUtil getNfcVUtil() {
        return nfcVUtil;
    }

    public void setNfcVUtil(NfcVUtil nfcVUtil) {
        this.nfcVUtil = nfcVUtil;
    }
}
