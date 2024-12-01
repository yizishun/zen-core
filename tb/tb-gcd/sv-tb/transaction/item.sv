// This is the base transaction object that will be used
// in the environment to initiate new transactions and
// capture transactions at DUT interface
`ifndef ITEM_SV
`define ITEM_SV
class item;
    rand bit [31:0] x;
    rand bit [31:0] y;
    bit [31:0] out;

    // This function allows us to print contents of the data packet
    // so that it is easier to track in a logfile
    function void print(string tag = "");
        $display("%t [%s] x: %d, y: %d, out: %d", $realtime, tag, x, y, out);
    endfunction
endclass
`endif