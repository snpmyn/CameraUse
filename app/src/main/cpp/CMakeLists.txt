cmake_minimum_required(VERSION 3.22.1)

project("usb_reset_lib")

# 编译 C++ 动态库
add_library(
        usb_reset_lib
        SHARED
        usb_reset.cpp
)

# 寻找系统日志库
find_library(
        log-lib
        log
)

# 链接日志库
target_link_libraries(
        usb_reset_lib
        ${log-lib}
)