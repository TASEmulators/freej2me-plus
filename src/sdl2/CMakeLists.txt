cmake_minimum_required(VERSION 3.1)

project(sdl_interface CXX)

find_package(SDL2 REQUIRED CONFIG REQUIRED COMPONENTS SDL2)
find_package(SDL2 REQUIRED CONFIG COMPONENTS SDL2main)

add_executable(sdl_interface anbu.cpp)

install(
    TARGETS sdl_interface
    RUNTIME DESTINATION bin
)
