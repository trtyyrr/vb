# 1. 设置 CMake 的最小版本要求
cmake_minimum_required(VERSION 3.22.1)

# 2. 定义项目名称
project("nexus_audit")

# 3. 创建算力库 (包含你的内联汇编代码)
add_library(
    nexus_audit # 库的名称，Java 层加载时使用 System.loadLibrary("nexus_audit")
    SHARED      # 编译为共享库 (.so)
    native-lib.cpp # 源文件路径
)

# 4. 查找 Android 原生日志库 (用于调试)
find_library(
    log-lib
    log
)

# 5. 链接库文件
target_link_libraries(
    nexus_audit # 目标库
    ${log-lib}  # 链接日志库
)

# 6. 极致性能编译参数（关键步骤）
# 即使 build.gradle 没写对，这里也能强制开启硬件加速
target_compile_options(
    nexus_audit
    PRIVATE
    -O3                             # 开启最高级代码优化
    -march=armv8.5-a+sve+sve2       # 针对骁龙 8 Gen 3 (Cortex-X4) 指令集优化
    -ffast-math                     # 牺牲微小精度换取极致浮点算力
    -funroll-loops                  # 展开循环，减少分支跳转开销
)