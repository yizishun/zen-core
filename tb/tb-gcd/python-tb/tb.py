import cocotb
from cocotb.triggers import Timer, RisingEdge, ReadOnly
from cocotb.clock import Clock

async def reset_dut(dut):
    """Reset the DUT."""
    dut.reset.value = 1
    await Timer(5, units="ns")  # Hold reset for 20 ns
    dut.reset.value = 0
    await RisingEdge(dut.clock)  # Wait for a clock edge after reset

@cocotb.test()
async def simple_test(dut):
  cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())
  await reset_dut(dut)
  
  dut.input_bits_x = 10
  dut.input_bits_y = 5
  dut.input_valid = 1
  while(dut.input_ready == 0):
    await ReadOnly()
  await RisingEdge(dut.clock)
  dut.input_valid = 0
  
  while(dut.output_valid == 0):
    await ReadOnly()
  assert(dut.output_bits == 5, f"Test case 1 failed: GCD(10, 5) != {dut.output_bits}")
  dut._log.info("Test case 1 passed: GCD(10, 5) = 5")