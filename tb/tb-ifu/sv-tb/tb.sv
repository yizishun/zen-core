module TB;
  // 时钟和复位信号
  reg clock;
  reg reset;

  // DUT接口信号
  wire out_valid;
  wire [31:0] out_bits_inst;
  wire [31:0] out_bits_pc;
  reg out_ready;
  reg imem_req_ready;
  wire imem_req_valid;
  wire [31:0] imem_req_bits_addr;
  wire [2:0] imem_req_bits_size;
  wire [2:0] imem_req_bits_cmd;
  wire [3:0] imem_req_bits_wmask;
  wire [31:0] imem_req_bits_wdata;
  wire imem_resp_ready;
  reg imem_resp_valid;
  reg [2:0] imem_resp_bits_cmd;
  reg [31:0] imem_resp_bits_rdata;
  reg isFlush;
  reg [31:0] correctedPC;

  // DUT实例化
  IFU dut (
    .clock(clock),
    .reset(reset),
    .out_valid(out_valid),
    .out_bits_inst(out_bits_inst),
    .out_bits_pc(out_bits_pc),
    .out_ready(out_ready),
    .imem_req_ready(imem_req_ready),
    .imem_req_valid(imem_req_valid),
    .imem_req_bits_addr(imem_req_bits_addr),
    .imem_req_bits_size(imem_req_bits_size),
    .imem_req_bits_cmd(imem_req_bits_cmd),
    .imem_req_bits_wmask(imem_req_bits_wmask),
    .imem_req_bits_wdata(imem_req_bits_wdata),
    .imem_resp_ready(imem_resp_ready),
    .imem_resp_valid(imem_resp_valid),
    .imem_resp_bits_cmd(imem_resp_bits_cmd),
    .imem_resp_bits_rdata(imem_resp_bits_rdata),
    .isFlush(isFlush),
    .correctedPC(correctedPC)
  );

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
    imem_req_ready = 0;
    imem_resp_valid = 0;
    imem_resp_bits_cmd = 0;
    imem_resp_bits_rdata = 0;
    isFlush = 0;
    correctedPC = 32'h0000_0000;

    // 等待复位结束
    @(negedge reset);

    // 测试握手信号
    repeat (50) begin
      @(posedge clock);
      imem_req_ready = ($random % 2) == 0;
      out_ready = ($random % 3) == 0;

      // 模拟IMEM响应
      if ($random % 4 == 0) begin
        imem_resp_valid = 1;
        imem_resp_bits_rdata = $random;
      end else begin
        imem_resp_valid = 0;
      end

      // 检查握手信号
      if (imem_req_valid && imem_req_ready) begin
        @(posedge clock);
        imem_req_ready = 0;
        repeat ($random % 5) @(posedge clock);
        imem_req_ready = 1;
      end

      if (out_valid && out_ready) begin
        @(posedge clock);
        out_ready = 0;
        repeat ($random % 5) @(posedge clock);
        out_ready = 1;
      end
    end

    $finish;
  end
endmodule
