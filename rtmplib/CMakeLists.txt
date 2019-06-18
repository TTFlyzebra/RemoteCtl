cmake_minimum_required(VERSION 3.4.1)

add_definitions("-DNO_CRYPTO")
include_directories(src/main/cpp/rtmp/librtmp)

#native-lib
file(GLOB PROJECT_SOURCES "src/main/cpp/rtmp/librtmp/*.c")

add_library(
        rtmplib
        SHARED
        src/main/cpp/rtmp/libresrtmp.c
        ${PROJECT_SOURCES}
        )

find_library( # Sets the name of the path variable.
        log-lib
        log)

target_link_libraries( # Specifies the target library.
        rtmplib
        ${log-lib})
