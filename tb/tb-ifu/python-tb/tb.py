import cocotb
from cocotb.clock import Clock
from cocotb.triggers import RisingEdge, FallingEdge, Timer
from cocotb.regression import TestFactory
import random

async def reset_dut(dut):
    """Reset the DUT."""
    dut.reset.value = 1
    await Timer(10, units="ps")  # Hold reset for 20 ns
    dut.reset.value = 0
    await RisingEdge(dut.clock)  # Wait for a clock edge after reset

async def handshake_fsm(dut):
    state = "IDLE"

    while True:
        if state == "IDLE":
            if dut.imem_req_valid.value and dut.imem_req_ready.value:
                state = "WAIT_RESP"
                dut.imem_resp_valid.value = 1
        elif state == "WAIT_RESP":
            if dut.imem_resp_valid.value and dut.imem_resp_ready.value:
                state = "IDLE"
                dut.imem_resp_valid.value = 0

        await RisingEdge(dut.clock)

@cocotb.test()
async def test_ifu_handshake(dut):
    """
    Test IFU handshake signals
    """
    clock = Clock(dut.clock, 10, units="ps")  # Create a 10ns clock
    cocotb.start_soon(clock.start())  # Start the clock

    # Reset the IFU
    await reset_dut(dut)

    # Initialize signals
    dut.isFlush.value = 0
    dut.correctedPC.value = 0x0000_0000
    dut.out_ready.value = 1
    dut.imem_resp_valid.value = 0
    
    cocotb.fork(handshake_fsm(dut))

    # Test handshake
    for cycle in range(50):

        await RisingEdge(dut.clock)
        dut.imem_req_ready.value = 1

        # out 
        if dut.out_valid.value and dut.out_ready.value:
            await RisingEdge(dut.clock)
            dut.out_ready.value = 0
            assert dut.out_valid.value == 0, "Output valid should be low after handshake"
            await Timer(random.randint(1, 5), units="ps")  # Random delay
            await RisingEdge(dut.clock)
            dut.out_ready.value = 1
