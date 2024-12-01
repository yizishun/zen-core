import "DPI-C" function void ref_generate(input int x, input int y, output int out);
class scoreboard extends uvm_scoreboard;
  `uvm_component_utils(scoreboard)
  function new(string name="scoreboard", uvm_component parent=null);
    super.new(name, parent);
  endfunction

  uvm_analysis_imp #(item, scoreboard) ap_imp;

  virtual function void build_phase(uvm_phase phase);
    super.build_phase(phase);
    ap_imp = new("ap_imp", this);
  endfunction

  virtual task write(item itm);
    int ref_result;
    ref_generate(itm.x, itm.y, ref_result);
    if(ref_result == itm.out) begin
      `uvm_info(get_type_name(), $sformatf("PASSED, x = %d, y = %d, out = %d, ref_out = %d", itm.x, itm.y, itm.out, ref_result), UVM_LOW)
    end
    else begin
      `uvm_error(get_type_name(), $sformatf("FAILED, x = %d, y = %d, out = %d, ref_out = %d", itm.x, itm.y, itm.out, ref_result))
    end
  endtask
endclass
