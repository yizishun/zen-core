`ifndef ENV_SV
`define ENV_SV
`include "component/generator.sv"
`include "component/driver.sv"
`include "component/monitor.sv"
`include "component/scoreboard.sv"
`include "transaction/item.sv"
class env;
    driver d0;
    monitor m0;
    generator g0;
    scoreboard sb0;

    mailbox #(item)scb_mbx;
    mailbox #(item)drv_mbx;
    event drv_done;

    virtual gcd_if vif;
    virtual clk_if vcif;
    function new();
        d0 = new;
        m0 = new;
        g0 = new;
        sb0 = new;
        scb_mbx = new();
        drv_mbx = new();

        d0.drv_mbx = drv_mbx;
        g0.drv_mbx = drv_mbx;
        m0.scb_mbx = scb_mbx;
        sb0.scb_mbx = scb_mbx;

        d0.drv_done = drv_done;
        g0.drv_done = drv_done;
    endfunction

    task run();
        d0.vif = vif;
        d0.vcif = vcif;
        m0.vif = vif;
        m0.vcif = vcif;

        fork
            g0.run();
            d0.run();
            m0.run();
            sb0.run();
        join
    endtask
endclass
`endif