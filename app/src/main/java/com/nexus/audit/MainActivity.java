package com.nexus.audit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends Activity {

    // --- 核心 1：保留之前的 C++ 连接 ---
    static { System.loadLibrary("nexus_audit"); }
    // 这里的函数名保持不变，为了兼容你现有的 C++ 代码
    private native void startAudit(int threads);
    private native long getPts();

    // --- 核心 2：定义 UI 变量 ---
    private long lastPts = 0;
    private boolean isRunning = false;
    private GameView gameView; // 新增的小游戏画布

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 关联 XML
        
        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 绑定 XML 里的控件
        final Button btn = findViewById(R.id.btn_launch);
        final TextView cpuTxt = findViewById(R.id.cpu_pts_display);
        final TextView gpuTxt = findViewById(R.id.gpu_fps_display);
        final LinearLayout container = findViewById(R.id.game_canvas_container);

        // 初始化 GPU 压力测试小游戏，并塞入界面
        gameView = new GameView(this, gpuTxt);
        container.addView(gameView);

        // 点击按钮：同时启动 CPU 汇编 + GPU 渲染
        btn.setOnClickListener(v -> {
            if (!isRunning) {
                // 1. 启动 C++ 汇编引擎 (CPU 满载)
                startAudit(32); 
                
                // 2. 锁定按钮状态
                isRunning = true;
                btn.setText("FULL LOAD RUNNING...");
                btn.setEnabled(false);
                
                // 3. 开启 CPU 数值刷新
                startCpuTimer(cpuTxt);
                
                // 4. 开启 GPU 游戏渲染线程
                gameView.startThread();
            }
        });
    }

    // 定时刷新 CPU 分数
    private void startCpuTimer(TextView view) {
        final Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                long current = getPts();
                long diff = current - lastPts;
                lastPts = current;
                view.setText("CPU Pts/s: " + diff);
                h.postDelayed(this, 1000); // 每秒刷新
            }
        });
    }

    // --- 核心 3：内置一个 SurfaceView 小游戏来压榨 GPU ---
    class GameView extends SurfaceView implements Runnable {
        private SurfaceHolder holder;
        private boolean running = false;
        private Paint paint = new Paint();
        private Random random = new Random();
        private TextView fpsView;

        public GameView(Context context, TextView fView) {
            super(context);
            holder = getHolder();
            fpsView = fView;
        }

        public void startThread() {
            running = true;
            new Thread(this).start();
        }

        @Override
        public void run() {
            long lastTime = System.currentTimeMillis();
            int frames = 0;
            while (running) {
                if (!holder.getSurface().isValid()) continue;
                
                // 锁定画布开始绘图
                Canvas canvas = holder.lockCanvas();
                
                // 模拟 GPU 负载：每帧绘制 500 个随机颜色的圆
                canvas.drawColor(Color.BLACK); // 清屏
                for (int i = 0; i < 500; i++) {
                    paint.setColor(Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                    canvas.drawCircle(random.nextInt(canvas.getWidth()), random.nextInt(canvas.getHeight()), 30, paint);
                }
                
                holder.unlockCanvasAndPost(canvas); // 提交给 GPU 显示
                
                // 计算 FPS
                frames++;
                long now = System.currentTimeMillis();
                if (now - lastTime > 1000) {
                    final int fps = frames;
                    // 回到主线程更新 UI
                    fpsView.post(() -> fpsView.setText("GPU FPS: " + fps));
                    frames = 0;
                    lastTime = now;
                }
            }
        }
    }
}
