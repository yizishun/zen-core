#include "VJK.h"        // Verilated module header
#include "verilated.h"  // Verilator header
#include "verilated_vcd_c.h"  // For VCD tracing

#include <iostream>
#include <fstream>

vluint64_t main_time = 0;  // Current simulation time

double sc_time_stamp() {
    return main_time;  // Current simulation time
}

int main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);

    // Initialize the top Verilated module
    VJK* top = new VJK;

    // Enable VCD trace if desired
    Verilated::traceEverOn(true);
    VerilatedVcdC* trace = new VerilatedVcdC;
    top->trace(trace, 99);  // Trace up to 99 levels
    trace->open("build/wave.vcd");

    // Reset sequence
    top->reset = 1;
    top->clock = 0;
    main_time += 5;
    top->eval();
    trace->dump(main_time);

    top->reset = 0;
    main_time += 5;
    top->eval();
    trace->dump(main_time);

    // Apply test vectors
    for (int i = 0; i < 10; i++) {
        // Generate clock signal
        top->clock = 0;
        top->eval();
        main_time += 5;
        trace->dump(main_time);

        top->clock = 1;
        top->eval();
        main_time += 5;
        trace->dump(main_time);

        // Apply input signals
        top->input_j = (i % 2 == 0) ? 1 : 0;
        top->input_k = (i % 3 == 0) ? 1 : 0;

        // Evaluate module
        top->eval();
        trace->dump(main_time);

        // Output current state
        std::cout << "Time: " << main_time 
                  << ", J: " << top->input_j 
                  << ", K: " << top->input_k 
                  << ", Q: " << top->output_q 
                  << ", Q1: " << top->output_q1 << std::endl;
    }

    // Finalize simulation
    trace->close();
    delete top;
    delete trace;

    return 0;
}
