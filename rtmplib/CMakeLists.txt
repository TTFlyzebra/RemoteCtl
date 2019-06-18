cmake_minimum_required(VERSION 3.4.1)

add_definitions("-DNO_CRYPTO")
include_directories(src/main/cpp/rtmp/librtmp)
include_directories(src/main/cpp/restreaming)

#native-lib
file(GLOB PROJECT_SOURCES1
        "src/main/cpp/rtmp/librtmp/*.c"
        "src/main/cpp/rtmp/libresrtmp.c"
        )
file(GLOB PROJECT_SOURCES2 "src/main/cpp/restreaming/*.c")


find_library( # Sets the name of the path variable.
        log-lib
        log)

add_library(
        rtmplib
        SHARED
        ${PROJECT_SOURCES1}
        )

target_link_libraries( # Specifies the target library.
        rtmplib
        ${log-lib})


add_library(
        flytools
        SHARED
        ${PROJECT_SOURCES2}
)

target_link_libraries( # Specifies the target library.
        flytools
        android
        ${log-lib})
