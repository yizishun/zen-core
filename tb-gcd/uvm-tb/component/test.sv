`ifndef TEST_SV
`define TEST_SV
`include "include.sv"
`include "component/env.sv"

class test extends uvm_test;
  `uvm_component_utils(test)
  function new(string name = "test", uvm_component parent=null);
    super.new(name, parent);
  endfunction

  env e0;
  virtual gcd_if vif;
  virtual clk_if vcif;

  virtual function void build_phase(uvm_phase phase);
    super.build_phase(phase);
    e0 = env::type_id::create("e0", this);
    if(!uvm_config_db#(virtual gcd_if)::get(this, "", "gcd_if", vif))
      `uvm_fatal("TEST", "Can't get vif")
    if(!uvm_config_db#(virtual clk_if)::get(this, "", "clk_if", vcif))
      `uvm_fatal("TEST", "Can't get vif")
    
    uvm_config_db#(virtual gcd_if)::set(this, "e0.a0.*", "gcd_if", vif);
  endfunction

  virtual task run_phase(uvm_phase phase);
    gen_item_seq seq = gen_item_seq::type_id::create("seq");
    phase.raise_objection(this);
    apply_reset();

    seq.randomize() with {num inside {[5:10]}; };
    seq.start(e0.a0.s0);
    #1000;
    phase.drop_objection(this);
  endtask

  virtual task apply_reset();
    vif.reset <= 1;
    repeat(5) @ (posedge vcif.clock);
    vif.reset <= 0;
    repeat(10) @ (posedge vcif.clock);
  endtask
endclass
`endif