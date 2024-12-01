`ifndef VERILATOR
class gcd_sequencer extends uvm_sequencer #(item);
`else
class gcd_sequencer extends uvm_sequencer #(item, item);
`endif

   `uvm_component_utils(gcd_sequencer)
//   `uvm_sequencer_utils(gcd_sequencer)

   function new(string name, uvm_component parent);
      super.new(name, parent);
   endfunction: new
endclass: gcd_sequencer
