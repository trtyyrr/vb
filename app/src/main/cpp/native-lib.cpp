#include <jni.h>
#include <thread>
#include <atomic>
#include <vector>

using namespace std;

// 使用 C++ 的原子变量，确保多线程下数据安全
atomic<long long> global_pts(0);
atomic<bool> is_running(false);

extern "C" JNIEXPORT void JNICALL
Java_com_nexus_audit_MainActivity_startAudit(JNIEnv* env, jobject /* this */, jint threads) {
    is_running = true;
    
    // C++ 线程池：开启你指定的 32 线程
    for (int i = 0; i < threads; i++) {
        thread([]() {
            while (is_running) {
                // 1000x 嵌套审计逻辑
                for (int j = 0; j < 1000; j++) {
                    
                    // --- 核心：这里就是你要求的汇编/机器码 ---
                    asm volatile (
                        "mov w0, #1000\n\t"               // 设置计数寄存器
                        "1:\n\t"
                        "fadd v0.4s, v0.4s, v1.4s\n\t"    // 机器码：向量加
                        "fmla v2.4s, v0.4s, v1.4s\n\t"    // 机器码：向量乘加
                        "fsqrt v3.4s, v2.4s\n\t"          // 机器码：硬件开方
                        "frsqrte v4.4s, v3.4s\n\t"        // 机器码：倒数近似
                        "subs w0, w0, #1\n\t"             // 减计数
                        "b.ne 1b\n\t"                     // 跳转循环
                        : : : "v0","v1","v2","v3","v4","w0","cc"
                    );
                }
                global_pts.fetch_add(1); // 结算 Pts
            }
        }).detach(); // 让线程在后台飞奔
    }
}

// 供 Java 层每秒调用的接口
extern "C" JNIEXPORT jlong JNICALL
Java_com_nexus_audit_MainActivity_getPts(JNIEnv* env, jobject /* this */) {
    return global_pts.load();
}