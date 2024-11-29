`ifndef SCOREBOARD_SV
`define SCOREBOARD_SV
import "DPI-C" function void ref_generate(input int x, input int y, output int out);
class scoreboard;
    mailbox #(item) scb_mbx;

    task run();
        $display("%t [Scoreboard]: started", $realtime);
        forever begin
            item itm;
            int ref_result;
            scb_mbx.get(itm);
            ref_generate(itm.x, itm.y, ref_result);
            if(ref_result == itm.out) begin
                $display("%t [Scoreboard] PASSED, x = %d, y = %d, out = %d, ref_out = %d", $realtime, itm.x, itm.y, itm.out, ref_result);
            end
            else begin
                $display("%t [Scoreboard] FAILED, x = %d, y = %d, out = %d, ref_out = %d", $realtime, itm.x, itm.y, itm.out, ref_result);
            end
        end
    endtask
endclass
`endif