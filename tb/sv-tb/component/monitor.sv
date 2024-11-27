// The monitor has a virtual interface handle with which
// it can monitor the events happening on the interface.
// It sees new transactions and then captures information
// into a packet and sends it to the scoreboard
// using another mailbox.
`ifndef MONITOR_SV
`define MONITOR_SV
class monitor;
    virtual gcd_if vif;
    virtual clk_if vcif;
    mailbox #(item)scb_mbx;

    task run();
        $display("%t [Monitor]: started", $realtime);
        sample_port("Monitor");
    endtask
    task sample_port(string tag = "");
        int i = 0;
        item itm;
        forever begin
            @(posedge vcif.clock);
            if(vif.out_valid && !vif.busy)begin
                $display("%t [%s]second stage", $realtime, tag);
                itm.out = vif.out;
                itm.print(tag);
                scb_mbx.put(itm);
            end
            if(vif.in_ready && vif.in_valid)begin
                $display("%t [%s]first stage", $realtime, tag);
                itm = new;
                itm.x = vif.x;
                itm.y = vif.y;
            end
            if(vif.busy)begin
                i ++;
                $display("%t [Monitor]: Busy: %d clock", $realtime, i);
            end
            if(!vif.busy)
                i = 0;
        end
    endtask
endclass
`endif