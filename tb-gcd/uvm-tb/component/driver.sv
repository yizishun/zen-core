class driver extends uvm_driver #(item);
  `uvm_component_utils(driver)
  function new (string name = "driver", uvm_component parent = null);
    super.new(name, parent);
  endfunction

  virtual gcd_if vif;
  virtual clk_if vcif;

  virtual function void build_phase(uvm_phase phase);
    super.build_phase(phase);
    if(!uvm_config_db#(virtual gcd_if)::get(this, "", "gcd_if", vif))
      `uvm_fatal("DRV", "Can't get vif");
    if(!uvm_config_db#(virtual clk_if)::get(this, "", "clk_if", vcif))
      `uvm_fatal("DRV", "Can't get vif");
  endfunction

  virtual task run_phase(uvm_phase phase);
    super.run_phase(phase);
    forever begin
      item itm;
      `uvm_info("DRV", $sformatf("Wait for item from sequencer"), UVM_LOW)
      seq_item_port.get_next_item(itm);
      `uvm_info("DRV", $sformatf("Get item, start driving"), UVM_LOW)
      drive_item(itm);
      wait(vif.out_valid);
      @(posedge vcif.clock);
      seq_item_port.item_done();
      `uvm_info("DRV", $sformatf("drive down"), UVM_LOW)      
    end
  endtask

  //TODO:modify
  virtual task drive_item(item itm);
    // Try to get a new transaction every time and then assign
    // packet contents to the interface. But do this only if the
    // design is ready to accept new transactions 
    @(posedge vcif.clock);
    if(!vif.reset)begin
        if(vif.in_ready)begin
          vif.in_valid <= 1;
          vif.x <= itm.x;
          vif.y <= itm.y;
        end
        else begin
            vif.in_valid <= 0;
            wait(vif.in_ready);
            vif.in_valid <= 1;
            vif.x <= itm.x;
            vif.y <= itm.y;
        end
    end
    endtask
endclass
