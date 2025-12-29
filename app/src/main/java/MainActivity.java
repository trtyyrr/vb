package com.nexus.audit;

import android.app.Activity;
import android.os.*;
import android.widget.*;
import android.view.WindowManager;

public class MainActivity extends Activity {
    static { System.loadLibrary("nexus_audit"); }
    private native void startAudit(int threads);
    private native long getPts();

    private long lastPts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        TextView tv = new TextView(this);
        tv.setTextSize(40f); // 巨型字体，方便隔着冰箱门看（如果你的冰箱有玻璃窗）
        setContentView(tv);

        startAudit(32); // 启动 32 线程压榨

        final Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                long current = getPts();
                tv.setText("Pts/s: " + (current - lastPts) + "\nTotal: " + current);
                lastPts = current;
                h.postDelayed(this, 1000);
            }
        });
    }
}