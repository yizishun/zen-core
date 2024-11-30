`timescale 1ns / 1ps
`include "component/test.sv"
module TB(
);
  //interface
  gcd_if _if();
  clk_if _cif ();
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
  initial begin
    uvm_config_db#(virtual gcd_if)::set(null, "uvm_test_top", "gcd_if", _if);
    uvm_config_db#(virtual clk_if)::set(null, "*", "clk_if", _cif);
    run_test("test");
  end
  //debug print
  initial begin
      forever begin
          #1 _if.print("INTERFACE");
      end
  end
  
endmodule //TB

