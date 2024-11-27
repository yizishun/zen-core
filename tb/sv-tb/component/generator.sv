// The generator class is used to generate a random
// number of transactions with random addresses and data
// that can be driven to the design
`ifndef GENERATOR_SV
`define GENERATOR_SV
class generator;
    mailbox #(item)drv_mbx;
    event drv_done;
    int num = 5;

    task run();
        for(int i=0; i<num; i++) begin
            item itm = new;
            if (itm.randomize() == 0) begin
                $display("Randomization failed!");
            end

            $display("%t [Generator]: Generated item %0d", $realtime, i);
            drv_mbx.put(itm);
            @(drv_done);
        end
        $display ("%t [Generator] Done generation of %0d items", $realtime, num);
        $finish;
    endtask
endclass
`endif