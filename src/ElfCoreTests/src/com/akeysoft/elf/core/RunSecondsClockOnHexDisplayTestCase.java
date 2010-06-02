package com.akeysoft.elf.core;

import junit.framework.TestCase;

public class RunSecondsClockOnHexDisplayTestCase extends TestCase {

	private Cpu cpu;
	private Ram ram;
	private InputDevice inputDevice;
	private OutputDevice outputDevice;
	
	protected void setUp() throws Exception {
		inputDevice = new InputDevice();
		outputDevice = new OutputDevice();
		ram = new Ram(65536);
		
		cpu = new Cpu(ram, inputDevice, outputDevice);
	}

	public void testRun() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90, (byte) 0xa2, (byte) 0xb2, (byte) 0xb3, (byte) 0xf8, (byte) 0x33, (byte) 0xa3, (byte) 0x12,
				(byte) 0x82, (byte) 0xfb, (byte) 0x0a, (byte) 0x3a, (byte) 0x1c, (byte) 0xf8, (byte) 0x00, (byte) 0xa2,
				(byte) 0x92, (byte) 0xfc, (byte) 0x10, (byte) 0xb2, (byte) 0x92, (byte) 0xfb, (byte) 0x60, (byte) 0x3a,
				(byte) 0x1c, (byte) 0xf8, (byte) 0x00, (byte) 0xb2, (byte) 0x82, (byte) 0x53, (byte) 0xe3, (byte) 0x92,
				(byte) 0xf4, (byte) 0x53, (byte) 0x64, (byte) 0x23, (byte) 0xf8, (byte) 0xf6, (byte) 0xa1, (byte) 0xf8,
				(byte) 0x90, (byte) 0xb1, (byte) 0x21, (byte) 0x91, (byte) 0x3a, (byte) 0x2a, (byte) 0x81, (byte) 0x3a,
				(byte) 0x2a, (byte) 0x30, (byte) 0x07, (byte) 0x00
		});
		
		cpu.setStopAfterCycles(3000000L);
		cpu.reset();
		cpu.run();
	}
}
