module TB;
  // 时钟和复位信号
  reg clock;
  reg reset;

  // DUT接口信号
  wire icache_out_valid;
  wire [31:0] icache_out_bits_inst;
  wire [31:0] icache_out_bits_pc;
  reg icache_out_ready;
  reg ifu_out_ready;
  reg out_ready;
  
  // 控制信号
  reg isFlush;
  reg [31:0] correctedPC;
  reg fencei_valid;
  reg fencei_bits_is_fencei;

  // DUT实例化
  Zen dut (
    .clock(clock),
    .reset(reset),
    .out_valid(out_valid),
    .out_bits_inst(out_bits_inst),
    .out_bits_pc(out_bits_pc),
    .out_ready(out_ready),
    .isFlush(isFlush),
    .correctedPC(correctedPC),
    .fencei_valid(fencei_valid),
    .fencei_bits_is_fencei(fencei_bits_is_fencei)
  );

  assign dut.ifu.out_ready = ifu_out_ready;
  assign dut.icache.out_resp_valid = icache_resp_valid;
  assign dut.icache.out_resp_bits_rdata = icache_resp_data;

  // 时钟生成
  initial begin
    clock = 0;
    forever #5 clock = ~clock;  // 10ns 时钟周期
  end

  // 复位生成
  initial begin
    reset = 1;
    #20;
    reset = 0;
  end

  // 测试过程
  initial begin
    // 初始化信号
    out_ready = 0;
    isFlush = 0;
    correctedPC = 32'h0000_0000;
    fencei_valid = 0;
    fencei_bits_is_fencei = 0;
    
    // 初始化跨模块信号
    ifu_out_ready = 0;
    icache_resp_valid = 0;
    icache_resp_data = 0;

    // 等待复位结束
    @(negedge reset);

    // 测试握手信号和指令获取
    repeat (50) begin
      @(posedge clock);
      // 随机生成out_ready信号
      out_ready = 1;

      // 模拟ICache响应
      if (icache_req_valid && ($random % 4 == 0)) begin
        icache_resp_valid = 1;
        icache_resp_data = $random;  // 随机指令数据
      end else begin
        icache_resp_valid = 0;
      end

      // 随机生成分支跳转
      if ($random % 20 == 0) begin  // 5%的概率产生分支跳转
        isFlush = 1;
        correctedPC = $random & 32'hFFFF_FFF0;  // 对齐的地址
        @(posedge clock);
        isFlush = 0;
      end

      // 随机生成fence.i指令
      if ($random % 50 == 0) begin  // 2%的概率产生fence.i指令
        fencei_valid = 1;
        fencei_bits_is_fencei = 1;
        @(posedge clock);
        fencei_valid = 0;
        fencei_bits_is_fencei = 0;
      end

      // 检查握手信号
      if (out_valid && out_ready) begin
        @(posedge clock);
        out_ready = 0;
        repeat ($random % 5) @(posedge clock);
        out_ready = 1;
      end
    end

    // 等待一些时钟周期让管道清空
    repeat(10) @(posedge clock);
    
    $finish;
  end

  // 波形输出
  initial begin
    $dumpfile("zen.vcd");
    $dumpvars(0, TB);
  end

  // 强制内部信号连接
  always @(*) begin
    force dut.ifu.out_ready = ifu_out_ready;
    force dut.icache.in_resp_valid = icache_resp_valid;
    force dut.icache.in_resp_bits_rdata = icache_resp_data;
  end

endmodule 