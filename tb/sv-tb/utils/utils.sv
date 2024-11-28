`timescale 1ns / 1ns
`ifndef UTILS_SV
`define UTILS_SV
task dumpWave(input string file);
    `ifdef VCS
        $fsdbDumpfile(file);
        $fsdbDumpvars("+all");
        $fsdbDumpSVA;
        $fsdbDumpon;
    `endif
    `ifdef VERILATOR
        $dumpfile(file);
        $dumpvars(0, TB);
    `endif
endtask

task generateClock(ref logic clock);
    begin
        clock = 1;
        forever begin
            #1 clock = ~clock;
        end
    end
endtask

task generateReset(ref logic reset);
    begin
        reset = 1;
        #4 reset = 0;
    end
endtask

task fin();
    $display("[%t] Testbench started", $realtime);
    #1000 $display("[%t] Testbench finished", $realtime);$finish;
endtask

task init_tb(ref logic reset, ref logic clock);
    fork
        dumpWave("./build/wave.vcd");
        generateClock(clock);
        generateReset(reset);
        fin();
    join
endtask
`endif