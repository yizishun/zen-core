`timescale 1ns / 1ps
`include "test.sv"
module TB(
);
    logic reset;
    clk_if _cif ();
    gcd_if _if();
    // dump wave
    string file = "./build/wave";
    initial begin
    `ifdef VCS
        $fsdbDumpfile(file);
        $fsdbDumpvars("+all");
        $fsdbDumpSVA;
        $fsdbDumpon;
    `endif
    `ifdef VERILATOR
        $dumpfile(file);
        $dumpvars(0, TB);
        $dumpvars(0, _if.x);
    `endif
    end
    // reset
    initial begin
        _if.reset = 1;
        #4 _if.reset = 0;
    end
    

    //DUT
    GCD gcd(
        .reset(_if.reset),
        .clock(_cif.clock),
        .input_valid(_if.in_valid),
        .input_ready(_if.in_ready),
        .input_bits_x(_if.x),
        .input_bits_y(_if.y),
        .output_valid(_if.out_valid),
        .output_bits(_if.out)
    );
    assign _if.busy = gcd.probeWire_busy_probe;
    //testcase
    test t0;
    initial begin
        t0 = new;
        t0.e0.vif = _if;
        t0.e0.vcif = _cif;
        t0.run();
    end
    //debug print
    initial begin
        #0.1;
        forever begin
            #1 _if.print("INTERFACE"); _cif.print("CLOCK");
        end
    end
    
endmodule //TB

