#include <vltstd/svdpi.h>
#include <iostream>
#include <fstream>
#include <nlohmann/json.hpp>
#include <common.h>
#include <driver.h>

using json = nlohmann::json;

extern "C" svLogic dump_wave(const char* file);

json config;
Driver driver;

void get_config(){
    std::ifstream f(CONFIG_FILE);
    config = json::parse(f);
}

extern "C" void zen_init(){
    //for verilog
    dump_wave("./build/wave.vcd");
    //for cpp
    get_config();
    driver = Driver(config);
}

extern "C" void zen_watchdog(char* dst){
    *dst = (char)driver.watchdog();
}