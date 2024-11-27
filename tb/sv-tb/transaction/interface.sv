`ifndef INTERFACE_SV
`define INTERFACE_SV
interface gcd_if ();
    logic reset;
    logic [31:0] x, y;
    logic in_valid;
    logic in_ready;

    logic [31:0] out;
    logic out_valid;

    //probe
    logic busy;

    function void print(string tag = "");
        $display("%t [%s] reset=%b in_valid=%b in_ready=%b x=%d y=%d out=%d out_valid=%b busy=%b", 
                 $realtime, tag, reset, in_valid, in_ready, x, y, out, out_valid, busy);
    endfunction
endinterface

interface clk_if();
    logic clock;
    initial clock = 0;

    always #1 clock <= ~clock;

    function void print(string tag = "");
        $display("%t [%s] clock=%b", $realtime, tag, clock);
    endfunction
endinterface
`endif