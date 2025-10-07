cmake_minimum_required(VERSION 3.22.1)
project(mt63fft)
add_library(mt63fft SHARED
    kissfft_compat.c
    native_fft_jni.c)
if (CMAKE_SYSTEM_PROCESSOR MATCHES "arm" OR CMAKE_SYSTEM_PROCESSOR MATCHES "aarch64")
    if (ANDROID_ABI MATCHES "armeabi-v7a")
        add_definitions(-mfpu=neon -D__ARM_NEON__)
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -ffast-math -funroll-loops -fomit-frame-pointer")
    elseif(ANDROID_ABI MATCHES "arm64-v8a")
        add_definitions(-D__ARM_NEON)
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -ffast-math -funroll-loops -fomit-frame-pointer")
    endif()
endif()
target_link_libraries(mt63fft log m)

