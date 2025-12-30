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

    static { System.loadLibrary("nexus_audit"); }
    
    private native void startAudit(int threads);
    private native long getPts();

    private long lastPts = 0;
    private boolean isRunning = false;
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Button btn = findViewById(R.id.btn_launch);
        final TextView cpuTxt = findViewById(R.id.cpu_pts_display);
        final TextView gpuTxt = findViewById(R.id.gpu_fps_display);
        final LinearLayout container = findViewById(R.id.game_canvas_container);

        gameView = new GameView(this, gpuTxt);
        container.addView(gameView);

        btn.setOnClickListener(v -> {
            if (!isRunning) {
                startAudit(32); // 启动 32 线程 CPU 审计
                isRunning = true;
                btn.setText("FULL SYSTEM AUDIT RUNNING...");
                btn.setEnabled(false);
                startCpuTimer(cpuTxt);
                gameView.startThread();
            }
        });
    }

    private void startCpuTimer(TextView view) {
        final Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                long current = getPts();
                long diff = current - lastPts;
                lastPts = current;
                view.setText("CPU Pts/s: " + diff);
                h.postDelayed(this, 1000);
            }
        });
    }

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
                Canvas canvas = holder.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK);
                    // 压榨 GPU：每帧画 500 个随机圆
                    for (int i = 0; i < 500; i++) {
                        paint.setColor(Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                        canvas.drawCircle(random.nextInt(canvas.getWidth()), random.nextInt(canvas.getHeight()), 30, paint);
                    }
                    holder.unlockCanvasAndPost(canvas);
                    frames++;
                }
                long now = System.currentTimeMillis();
                if (now - lastTime > 1000) {
                    final int fps = frames;
                    fpsView.post(() -> fpsView.setText("GPU FPS: " + fps));
                    frames = 0;
                    lastTime = now;
                }
            }
        }
    }
}
