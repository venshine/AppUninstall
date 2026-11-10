cmake_minimum_required(VERSION 3.22.1)

add_library(uninstall SHARED
    src/main/cpp/uninstall.cpp)

target_link_libraries(uninstall log)