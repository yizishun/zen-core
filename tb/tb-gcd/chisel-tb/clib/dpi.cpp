#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <time.h>
#include <stdbool.h>
#include <string.h>
#include <math.h>
#include "svdpi.h"
extern "C" svLogic dump_wave(const char* file);
#define DATA_WIDTH 16
extern "C" void gcd_init(){
  dump_wave("./build/wave.vcd");
}
uint16_t gcd(uint16_t x, uint16_t y) {
    while (y != 0) {
        uint16_t temp = y;
        y = x % y;
        x = temp;
    }
    return x;
}
void write_to_pointer(uint8_t* dst, const uint8_t* data, size_t len) {
    memcpy(dst, data, len);
}

extern "C" void gcd_input(svBitVecVal* out_0) {
    // 初始化随机数种子
    srand(time(NULL));

    // 生成随机 x 和 y
    uint16_t x = rand() % ((uint16_t)1 << DATA_WIDTH);
    uint16_t y = rand() % ((uint16_t)1 << DATA_WIDTH);
    uint16_t result = gcd(x, y);

    // 打印生成的数据
    printf("gcd_input: x=%llu, y=%llu, result=%llu\n", x, y, result);

    // 填充数据到输出指针，按以下顺序：
    // [ result | y | x | valid ]
    uint8_t data[3 * sizeof(uint16_t) + 1] = {0}; // valid (1 byte) + 3 * 64-bit numbers

    // 将 result, y, x 依次写入
    memcpy(&data[0], &result, sizeof(uint16_t));
    memcpy(&data[sizeof(uint16_t)], &y, sizeof(uint16_t));
    memcpy(&data[2 * sizeof(uint16_t)], &x, sizeof(uint16_t));

    // 随机生成 valid 位 (95% 的概率为 1)
    data[3 * sizeof(uint16_t)] = (rand() % 100 < 95) ? 1 : 0;

    // 写入到指针
    write_to_pointer((uint8_t*)out_0, data, sizeof(data));
}
