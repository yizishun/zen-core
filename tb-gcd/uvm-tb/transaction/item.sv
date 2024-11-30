`ifndef ITEM_SV
`define ITEM_SV
`define WIDTH 32
//TODO:modify
class item extends uvm_sequence_item;
  rand bit [`WIDTH-1:0]  	x;
  rand bit [`WIDTH-1:0] 	y;
  bit [`WIDTH-1:0] 		  out;

  // Use utility macros to implement standard functions
  // like print, copy, clone, etc
  `uvm_object_utils_begin(item)
  	`uvm_field_int (x, UVM_DEFAULT)
  	`uvm_field_int (y, UVM_DEFAULT)
  	`uvm_field_int (out, UVM_DEFAULT)
  `uvm_object_utils_end

  virtual function string convert2str();
    return $sformatf("x=0x%0d y=0x%0d out=0x%0d", x, y, out);
  endfunction

  function new(string name = "reg_item");
    super.new(name);
  endfunction
endclass
`endif