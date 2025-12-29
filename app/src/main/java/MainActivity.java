package com.nexus.audit;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    
    // ---------------------------------------------------------
    // 1. 【核心枢纽】加载 C++ 汇编引擎
    // ---------------------------------------------------------
    static { 
        // 这里的名字必须和 CMakeLists.txt 中的 project 名字一致
        System.loadLibrary("nexus_audit"); 
    }

    // 声明 C++ 侧的函数接口（这就是你之前的另外一个文件里的核心内容）
    private native void startAudit(int threads);
    private native long getPts();

    // ---------------------------------------------------------
    // 2. 【状态控制】定义运行变量
    // ---------------------------------------------------------
    private long lastPts = 0;
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 【关键】关联布局文件，没有这一行就看不到按钮
        setContentView(R.layout.activity_main); 
        
        // 保持屏幕常亮，防止审计时平板熄屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 绑定界面上的组件
        final Button btn = findViewById(R.id.btn_launch);
        final TextView txt = findViewById(R.id.pts_display);

        // ---------------------------------------------------------
        // 3. 【启动逻辑】挂钩按钮与汇编引擎
        // ---------------------------------------------------------
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRunning) {
                    // 调用 C++ 接口，启动 32 线程暴力审计
                    startAudit(32); 
                    
                    isRunning = true;
                    btn.setText("AUDITING..."); // 改变按钮文字
                    btn.setEnabled(false);      // 禁用按钮防止重复点击
                    
                    // 启动每秒更新一次数值的定时器
                    startUIUpdateTimer(txt);
                }
            }
        });
    }

    // 定时刷新屏幕上的审计点数
    private void startUIUpdateTimer(final TextView view) {
        final Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                long current = getPts();
                long diff = current - lastPts; // 计算每秒增量
                lastPts = current;
                
                // 将结果显示在屏幕上
                view.setText("Audit Pts/s: " + diff + "\nTotal Pts: " + current);
                
                // 每 1000 毫秒（1秒）运行一次
                h.postDelayed(this, 1000);
            }
        });
    }
}
