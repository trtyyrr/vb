package com.nexus.audit;

import android.app.Activity;
import android.os.*;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;

public class MainActivity extends Activity {
    static { System.loadLibrary("nexus_audit"); }

    // 定义 JNI 接口
    private native void startAudit(int threads);
    private native long getPts();

    private long lastPts = 0;
    private boolean running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 强制常亮，防止冰箱内锁屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final TextView ptsView = findViewById(R.id.pts_display);
        final Button launchBtn = findViewById(R.id.btn_launch);

        launchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!running) {
                    // 开启 32 线程：汇编指令将瞬间填满骁龙 8 Gen 3 的所有流水线
                    startAudit(32); 
                    running = true;
                    launchBtn.setText("AUDITING...");
                    launchBtn.setEnabled(false);
                    startMonitoring(ptsView);
                }
            }
        });
    }

    private void startMonitoring(final TextView view) {
        final Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                long current = getPts();
                long diff = current - lastPts;
                lastPts = current;

                // 实时刷新算力结算
                view.setText(String.format("Pts/s: %d\nTotal: %d", diff, current));
                
                h.postDelayed(this, 1000);
            }
        });
    }
}
