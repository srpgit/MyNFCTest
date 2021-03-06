package com.gzrj.test.nfc.mynfctest;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // 是否开启debug
    private static boolean isDebug = true;

    // nfc适配器
    private NfcAdapter adapter;

    // pendingIntent
    private PendingIntent pendingIntent;

    private NfcVUtil nfcVUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initComponents();
    }

    private <T> T getView(int id, Class<T> clazz) {
        return (T) findViewById(id);
    }

    /**
     * 检查nfc功能是否正常
     *
     * @return 正常返回true
     */
    private boolean checkNfC() {

        debug("checking nfc...");

        if (adapter == null) {
            adapter = NfcAdapter.getDefaultAdapter(this.getApplicationContext());
        }

        // 检测是否支持NFC
        if (adapter == null) {
            this.tip("本设备不支持NFC");
            return false;
        }

        if (pendingIntent == null) {
            pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        }

        // 检测NFC是否开启
        if (!adapter.isEnabled()) {
            this.tip("请在设置中开启NFC功能");
            return false;
        }

        debug("nfc is ok...");

        return true;
    }

    /**
     * debug 信息
     *
     * @param text
     */
    public static void debug(String text) {
        if (isDebug) {
            Log.d(MainActivity.class.getName(), text);
        }
    }

    /**
     * 用户提示信息
     *
     * @param msg
     */
    private void tip(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 禁用前台nfc检测
     */
    private void stopForeGroundDispatch() {
        if (adapter != null && adapter.isEnabled()) {
            adapter.disableForegroundDispatch(this);
        }
    }

    /**
     * 初始化webview
     */
    private void initComponents() {

        getView(R.id.write_to_block_btn, Button.class).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nfcVUtil == null) {
                    tip("请先接触NFC卡片，且不要移动");
                    return;
                }
                CharSequence text = getView(R.id.write_to_block_value, EditText.class).getText();
                try {
                    nfcVUtil.writeString(String.valueOf(text));
                    tip("写入成功");
                } catch (IOException e) {
                    if (e instanceof ConnectFailedException) {
                        tip("请先接触NFC卡片，且不要移动");
                    } else {
                        e.printStackTrace();
                        tip("写入失败");
                    }
                }
            }
        });

        getView(R.id.read_block_btn, Button.class).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nfcVUtil == null) {
                    tip("没有读取到卡片信息");
                    return;
                }
                TextView textView = getView(R.id.read_block_value, TextView.class);
                textView.setText("");
                try {
                    textView.setText(nfcVUtil.readAll());
                    tip("读取成功");
                } catch (IOException e) {
                    e.printStackTrace();
                    tip("读取失败");
                }
            }
        });
    }


    /**
     * 当前程序不在前台运行时，触发nfc事件，则直接读取信息。
     * 无论如何，都要开启前台检测，等待{@link MainActivity#onNewIntent(Intent)}事件触发。
     * 若不开启，则每次nfc连接，都会跳转到选择nfc处理程序。
     * 前台优先级大于全局。
     */
    @Override
    protected void onResume() {
        super.onResume();
        debug("on resume...");
        debug("intent is" + this.getIntent().getAction());

        Intent intent = this.getIntent();

        if (intent != null && NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            this.readNfcInfo(intent);
        }

        if (this.checkNfC()) {
            adapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    /**
     * 暂停时要停止前台nfc扫描
     */
    @Override
    protected void onPause() {
        super.onPause();
        this.stopForeGroundDispatch();
    }

    /**
     * 前台运行中，检测到{@link NfcAdapter#ACTION_TAG_DISCOVERED}时，读取nfc信息
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String actioName = intent.getAction();
        debug("on new intent..." + actioName);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(actioName) || NfcAdapter.ACTION_TAG_DISCOVERED.equals(actioName)) {
            readNfcInfo(intent);
        }
    }

    /**
     * 读取nfc tag中的信息
     *
     * @param intent
     */
    private NfcVUtil readNfcInfo(Intent intent) {
        debug("开始读取tag信息，来自intent：" + intent.getAction());

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (tag == null) {
            debug("tag is null...");
            return null;
        }

        boolean support = false;
        String[] techList = tag.getTechList();
        if (techList != null && techList.length > 0) {
            for (String tech : techList) {
                if (tech.equals(NfcV.class.getName())) {
                    support = true;
                    break;
                }
            }
        }
        if (!support) {
            this.tip("不是NfcV类型的Tag，不支持...");
            return null;
        }

        NfcV nfcV = NfcV.get(tag);

        if (nfcV == null) {
            debug("nfcV is null...");
            return null;
        }

        try {
            nfcV.connect();

            nfcVUtil = new NfcVUtil(nfcV);

            this.showNfcInfo(nfcVUtil);

            return nfcVUtil;

        } catch (IOException e) {
            e.printStackTrace();
            debug("读取失败：" + e.getMessage());
        } finally {
            if (nfcV != null) {
                try {
                    nfcV.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 显示标签信息
     *
     * @param nfcVUtil
     */
    private void showNfcInfo(NfcVUtil nfcVUtil) {
        Map<String, Object> data = new HashMap<>();
        try {
            getView(R.id.uid_value, TextView.class).setText(nfcVUtil.getUID());
            getView(R.id.afi_value, TextView.class).setText(nfcVUtil.getAFI());
            getView(R.id.dsfid_value, TextView.class).setText(nfcVUtil.getDSFID());
            getView(R.id.block_num_value, TextView.class).setText(String.valueOf(nfcVUtil.getBlockNumber()));
            getView(R.id.block_size_value, TextView.class).setText(String.valueOf(nfcVUtil.getOneBlockSize()));
            getView(R.id.uid_value, TextView.class).setText(nfcVUtil.getUID());
            getView(R.id.read_block_value, TextView.class).setText(nfcVUtil.readAll());
            tip("读取数据成功");
        } catch (Exception e) {
            e.printStackTrace();
            tip("读取失败");
        }
    }
}
