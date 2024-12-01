#include "VGCD.h"         // Verilator generated header
#include "verilated.h"    // Verilator core header
#include "verilated_vcd_c.h" // For waveform generation

#include <iostream>
#include <cstdlib>

#define MAX_CYCLES 1000

void tick(VGCD* top, VerilatedVcdC* tfp, int& main_time) {
    top->clock = 0;
    top->eval();
    if (tfp) tfp->dump(main_time++);
    top->clock = 1;
    top->eval();
    if (tfp) tfp->dump(main_time++);
}

int main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);

    VGCD* top = new VGCD;
    VerilatedVcdC* tfp = nullptr;

    // Enable waveform dump
    Verilated::traceEverOn(true);
    tfp = new VerilatedVcdC;
    top->trace(tfp, 99);
    tfp->open("./build/wave.vcd");

    int main_time = 0; // Time simulation counter
    top->reset = 1;
    top->input_valid = 0;

    // Apply reset
    for (int i = 0; i < 5; i++) {
        tick(top, tfp, main_time);
    }
    top->reset = 0;

    // Provide inputs
    int input_x = 56; // Example input x
    int input_y = 98; // Example input y

    top->input_bits_x = input_x;
    top->input_bits_y = input_y;
    top->input_valid = 1;

    while (main_time < MAX_CYCLES) {
        tick(top, tfp, main_time);

        // Assert input_ready, then stop providing inputs
        if (top->input_ready && top->input_valid) {
            top->input_valid = 0;
        }

        // Check output validity and print the result
        if (top->output_valid) {
            std::cout << "GCD(" << input_x << ", " << input_y << ") = " << top->output_bits << std::endl;
            break;
        }
    }

    if (main_time >= MAX_CYCLES) {
        std::cerr << "Simulation reached max cycles without completing!" << std::endl;
    }

    top->final();
    if (tfp) {
        tfp->close();
        delete tfp;
    }
    delete top;
    return 0;
}