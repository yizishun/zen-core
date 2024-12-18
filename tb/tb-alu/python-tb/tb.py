import cocotb
from cocotb.clock import Clock
from cocotb.triggers import RisingEdge
from cocotb.regression import TestFactory

@cocotb.test()
async def test_alu_addition(dut):
    """
    Test ALU addition operation
    """
    clock = Clock(dut.clock, 10, units="ns")  # Create a 10ns clock
    cocotb.start_soon(clock.start())  # Start the clock

    # Reset the ALU
    dut.reset.value = 1
    await RisingEdge(dut.clock)
    dut.reset.value = 0
    await RisingEdge(dut.clock)

    # Test data for addition
    test_vectors = [
        {"src_0": 5, "src_1": 10, "func": 8, "expected_result": 15},
        {"src_0": 0, "src_1": 0, "func": 8, "expected_result": 0},
        {"src_0": 0xFFFFFFFF, "src_1": 1, "func": 8, "expected_result": 0},
        {"src_0": 123, "src_1": 321, "func": 8, "expected_result": 444},
    ]

    for vec in test_vectors:
        dut.src_0.value = vec["src_0"]
        dut.src_1.value = vec["src_1"]
        dut.func.value = vec["func"]  # Assuming func=0 for addition

        await RisingEdge(dut.clock)  # Wait for clock edge

        result = dut.result.value.integer  # Read the result from DUT
        assert result == vec["expected_result"], f"Addition failed for src_0={vec['src_0']}, src_1={vec['src_1']}"

@cocotb.test()
async def test_alu_subtraction(dut):
    """
    Test ALU subtraction operation
    """
    clock = Clock(dut.clock, 10, units="ns")  # Create a 10ns clock
    cocotb.start_soon(clock.start())  # Start the clock

    # Reset the ALU
    dut.reset.value = 1
    await RisingEdge(dut.clock)
    dut.reset.value = 0
    await RisingEdge(dut.clock)

    # Test data for subtraction
    test_vectors = [
        {"src_0": 15, "src_1": 10, "func": 9, "expected_result": 5},
        {"src_0": 0, "src_1": 0, "func": 9, "expected_result": 0},
        {"src_0": 1, "src_1": 2, "func": 9, "expected_result": 0xFFFFFFFF},  # Test unsigned underflow
        {"src_0": 321, "src_1": 123, "func": 9, "expected_result": 198},
    ]

    for vec in test_vectors:
        dut.src_0.value = vec["src_0"]
        dut.src_1.value = vec["src_1"]
        dut.func.value = vec["func"]  # Assuming func=1 for subtraction

        await RisingEdge(dut.clock)  # Wait for clock edge

        result = dut.result.value.integer  # Read the result from DUT
        assert result == vec["expected_result"], f"Subtraction failed for src_0={vec['src_0']}, src_1={vec['src_1']}, result={result}"

@cocotb.test()
async def test_alu_cmpu(dut):
    """
    Test ALU cmpu operation
    """
    clock = Clock(dut.clock, 10, units="ns")  # Create a 10ns clock
    cocotb.start_soon(clock.start())  # Start the clock

    # Reset the ALU
    dut.reset.value = 1
    await RisingEdge(dut.clock)
    dut.reset.value = 0
    await RisingEdge(dut.clock)

    # Test data for cmpu
    test_vectors = [
        {"src_0": 15, "src_1": 10, "func": 3, "expected_result": 0},
        {"src_0": 0, "src_1": 0, "func": 3, "expected_result": 0},
        {"src_0": 1, "src_1": 2, "func": 3, "expected_result": 1},
        {"src_0": 321, "src_1": 123, "func": 3, "expected_result": 0},
    ]

    for vec in test_vectors:
        dut.src_0.value = vec["src_0"]
        dut.src_1.value = vec["src_1"]
        dut.func.value = vec["func"]  # Assuming func=3 for cmpu

        await RisingEdge(dut.clock)  # Wait for clock edge

        result = dut.result.value.integer  # Read the result from DUT
        assert result == vec["expected_result"], f"cmpu failed for src_0={vec['src_0']}, src_1={vec['src_1']}, result={result}"