module TB(
    input logic clk,
    input logic rst,
    input logic [7:0] data_in,
    output logic [7:0] data_out
);

    assign data_out = data_in;
    initial begin
        `ifdef VCS
            $fsdbDumpfile("./build/dump.fsdb");
            $fsdbDumpvars("+all");
            $fsdbDumpSVA;
            $fsdbDumpon;
       `endif
       `ifdef VERILATOR
            $dumpfile("./build/dump.vcd");
            $dumpvars(0);
       `endif
    end
    
endmodule //TB

