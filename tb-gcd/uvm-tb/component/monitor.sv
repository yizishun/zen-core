class monitor extends uvm_monitor;
  `uvm_component_utils(monitor)
  function new (string name = "monitor", uvm_component parent = null);
    super.new(name, parent);
  endfunction

  uvm_analysis_port #(item) mon_ap;
  virtual gcd_if vif;
  virtual clk_if vcif;
  semaphore sema4;

  virtual function void build_phase(uvm_phase phase);
    super.build_phase(phase);
    if(!uvm_config_db#(virtual gcd_if)::get(this, "", "gcd_if", vif))begin
      `uvm_fatal("MON", "Can't get vif")
    end
    if(!uvm_config_db#(virtual clk_if)::get(this, "", "clk_if", vcif))begin
      `uvm_fatal("MON", "Can't get vif")
    end
    sema4 = new(1);
    mon_ap = new("mon_ap", this);
  endfunction

  virtual task run_phase(uvm_phase phase);
    super.run_phase(phase);
    sample_port("MON");
  endtask
  //TODO:modify
  task sample_port(string tag = "");
        item itm = new;
        forever begin
            @(posedge vcif.clock);
            if(vif.out_valid)begin
              itm.out = vif.out;
              `uvm_info(get_type_name(), $sformatf("Monitor found packet %s", itm.convert2str()), UVM_LOW)
              mon_ap.write(itm);
            end
            if(vif.in_ready && vif.in_valid)begin
                $display("%t [%s]first stage", $realtime, tag);
                itm.x = vif.x;
                itm.y = vif.y;
            end
        end
    endtask
endclass
