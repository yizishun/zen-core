// The driver is responsible for driving transactions to the DUT
// All it does is to get a transaction from the mailbox if it is
// available and drive it out into the DUT interface.
`ifndef DRIVER_SV
`define DRIVER_SV
class driver;
    virtual gcd_if vif;
    virtual clk_if vcif;
    event drv_done;
    mailbox #(item)drv_mbx;

    task run();
        @(posedge vcif.clock)

        // Try to get a new transaction every time and then assign
        // packet contents to the interface. But do this only if the
        // design is ready to accept new transactions 
        forever begin
            item itm;
            $display("%t [Driver]: waiting for itm", $realtime);
            @(posedge vcif.clock);
            if(!vif.reset)begin
                if(vif.in_ready)begin
                    drv_mbx.get(itm);
                    itm.print("Driver");
                    vif.in_valid = 1;
                    vif.x = itm.x;
                    vif.y = itm.y;
                    $display("%t [Driver]: driving item, valid = %d, x = %d, y = %d", $realtime, vif.in_valid, vif.x, vif.y);
                    ->drv_done;
                end
                else begin
                    vif.in_valid = 0;
                end
            end
        end
    endtask
endclass
`endif