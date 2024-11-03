cmake_minimum_required(VERSION 3.22.1)

project("invertnumber")

add_library(
        invertnumber
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        invertnumber
        ${log-lib})