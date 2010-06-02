package com.akeysoft.elf.core;

import junit.framework.TestCase;

public class CpuTestCase extends TestCase {

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

	public void testIDL() throws UnknownOpcodeException {
		ram.setByte(0, (byte) 0x00);
		
		assertFalse(cpu.isIdle());
		
		cpu.run();
		
		assertTrue(cpu.isIdle());
		assertEquals(2, cpu.getCycles());
	}
	
	public void testSEQ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {0x7a, 0x00});
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		
		ram.setBytes(0, new byte[] {0x7b, 0x00});
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(4, cpu.getCycles());
	}
	
	public void testREQ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {0x7b, 0x7a, 0x00});
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(6, cpu.getCycles());
	}
	
	public void testBn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7b,		// SEQ
				0x37, 0x04,	// B4 4
				0x7a,		// REQ
				0x00		// IDL
		});
		
		cpu.setEf(3, false);
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(8, cpu.getCycles());
		
		cpu.setEf(3, true);
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(6, cpu.getCycles());
	}
	
	public void testBNn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7b,		// SEQ
				0x3f, 0x04,	// BN4 4
				0x7a,		// REQ
				0x00		// IDL
		});
		
		cpu.setEf(3, false);
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(6, cpu.getCycles());
		
		cpu.setEf(3, true);
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(8, cpu.getCycles());
	}
	
	public void testBR() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7a,		// REQ
				0x30, 0x04, // BR 4
				0x00,		// IDL
				0x7b,		// SEQ
				0x00		// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(8, cpu.getCycles());
	}
	
	public void testLBR() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7a,						// REQ
				(byte) 0xc0, 0x00, 0x05, 	// LBR 5
				0x00,						// IDL
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(9, cpu.getCycles());
	}
	
	public void testOUTn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x64,	// OUT 4
				0x7b,	// SEQ <-- will not be executed.
				0x00	// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(0x7b, outputDevice.getPort(3));
		assertEquals(4, cpu.getCycles());
	}
	
	public void testINPn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x6c,	// INP 4
				0x7a,	// REQ <-- will be replaced by the input byte.
				0x00	// IDL
		});
		
		inputDevice.setPort(3, (byte) 0x7b);
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(0x7b, ram.getByte(0x01));
		assertEquals(0x7b, cpu.getD());
		assertEquals(6, cpu.getCycles());
	}
	
	public void testSKP() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7a,	// REQ
				0x38,	// SKP
				0x7b,	// SEQ
				0x00	// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(6, cpu.getCycles());
	}
	
	public void testLSKP() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7a,			// REQ
				(byte) 0xc8,	// LSKP
				0x7b,			// SEQ
				0x7b,			// SEQ
				0x00			// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(7, cpu.getCycles());
	}
	
	public void testNOP() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7a,			// REQ
				(byte) 0xc4,	// NOP
				0x7b,			// SEQ
				0x00			// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(9, cpu.getCycles());
	}
	
	public void testLDI() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x55,	// LDI 55
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x55, cpu.getD());
		assertEquals(4, cpu.getCycles());
	}
	
	public void testBZ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x00,	// LDI 00
				0x32, 0x05,		// BZ 06
				0x7b,			// SEQ
				0x00			// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(6, cpu.getCycles());
		
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x01,	// LDI 01
				0x32, 0x05,		// BZ 06
				0x7b,			// SEQ
				0x00			// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(8, cpu.getCycles());
	}
	
	public void testBNZ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x00,	// LDI 00
				0x3a, 0x05,			// BNZ 05
				0x7b,				// SEQ
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(8, cpu.getCycles());
		
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x01,	// LDI 01
				0x3a, 0x05,			// BNZ 05
				0x7b,				// SEQ
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(6, cpu.getCycles());
	}
	
	public void testLBZ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x00,			// LDI 00
				(byte) 0xc2, 0x00,	0x06,	// LBZ 0006
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(7, cpu.getCycles());
		
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x01,			// LDI 01
				(byte) 0xc2, 0x00,	0x06,	// LBZ 0006
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(9, cpu.getCycles());
	}
	
	public void testLBNZ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x00,			// LDI 00
				(byte) 0xca, 0x00,	0x06,	// LBNZ 0006
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(9, cpu.getCycles());
		
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x01,			// LDI 01
				(byte) 0xca, 0x00,	0x06,	// LBNZ 0006
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(7, cpu.getCycles());
	}
	
	public void testLSZ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x00,	// LDI 00
				(byte) 0xce,		// LSZ
				0x7b,				// SEQ
				0x7b,				// SEQ
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(7, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x01,	// LDI 01
				(byte) 0xce,		// LSZ
				0x7b,				// SEQ
				0x7b,				// SEQ
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(11, cpu.getCycles());
	}

	public void testLSNZ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x00,	// LDI 00
				(byte) 0xc6,		// LSNZ
				0x7b,				// SEQ
				0x7b,				// SEQ
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(11, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x01,	// LDI 01
				(byte) 0xc6,		// LSNZ
				0x7b,				// SEQ
				0x7b,				// SEQ
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(7, cpu.getCycles());
	}
	
	public void testLBQ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7a,						// REQ
				(byte) 0xc1, 0x00, 0x05,	// LBQ 0005
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(9, cpu.getCycles());
		
		ram.setBytes(0, new byte[] {
				0x7b,						// SEQ
				(byte) 0xc1, 0x00, 0x05,	// LBQ 0005
				0x7a,						// REQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(7, cpu.getCycles());
	}
	
	public void testLBNQ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7a,						// REQ
				(byte) 0xc9, 0x00, 0x05,	// LBNQ 0005
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(7, cpu.getCycles());
		
		ram.setBytes(0, new byte[] {
				0x7b,						// SEQ
				(byte) 0xc9, 0x00, 0x05,	// LBNQ 0005
				0x7a,						// REQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(9, cpu.getCycles());
	}

	public void testBQ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7a,		// REQ
				0x31, 0x04,	// BQ 04
				0x7b,		// SEQ
				0x00		// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(8, cpu.getCycles());
		
		ram.setBytes(0, new byte[] {
				0x7b,				// SEQ
				(byte) 0x31, 0x04,	// BQ 04
				0x7a,				// REQ
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(6, cpu.getCycles());
	}
	
	public void testBNQ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7a,		// REQ
				0x39, 0x04,	// BNQ 04
				0x7b,		// SEQ
				0x00		// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(6, cpu.getCycles());
		
		ram.setBytes(0, new byte[] {
				0x7b,				// SEQ
				(byte) 0x39, 0x04,	// BNQ 04
				0x7a,				// REQ
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(8, cpu.getCycles());
	}
	
	public void testLSQ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7a,			// REQ
				(byte) 0xcd,	// LSQ
				0x7b,			// SEQ
				0x7b,			// SEQ
				0x00			// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(11, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				0x7b,			// SEQ
				(byte) 0xcd,	// LSQ
				0x7a,			// REQ
				0x7a,			// REQ
				0x00			// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(7, cpu.getCycles());
	}
	
	public void testLSNQ() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7a,			// REQ
				(byte) 0xc5,	// LSNQ
				0x7b,			// SEQ
				0x7b,			// SEQ
				0x00			// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(7, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				0x7b,			// SEQ
				(byte) 0xc5,	// LSNQ
				0x7a,			// REQ
				0x7a,			// REQ
				0x00			// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(11, cpu.getCycles());
	}
	
	public void testINCn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x11,			// INC 1
				0x12,			// INC 2
				0x12,			// INC 2
				0x1f,			// INC f
				0x1f,			// INC f
				0x1f,			// INC f
				0x00			// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(1, cpu.getR(1));
		assertEquals(2, cpu.getR(2));
		assertEquals(3, cpu.getR(0xf));
		assertEquals(14, cpu.getCycles());
	}
	
	public void testDECn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x21,			// DEC 1
				0x22,			// DEC 2
				0x22,			// DEC 2
				0x2f,			// DEC f
				0x2f,			// DEC f
				0x2f,			// DEC f
				0x00			// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0xFFFF, cpu.getR(1));
		assertEquals(0xFFFE, cpu.getR(2));
		assertEquals(0xFFFD, cpu.getR(0xf));
		assertEquals(14, cpu.getCycles());
	}
	
	public void testGLOn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xaa,	// LDI AA
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, 0x55,			// LDI 55
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, 0x00,			// LDI 00
				(byte) 0x81,				// GLO 1
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x55, cpu.getD());
		assertEquals(14, cpu.getCycles());
	}
	
	public void testGHIn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xaa,	// LDI AA
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, 0x55,			// LDI 55
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, 0x00,			// LDI 00
				(byte) 0x91,				// GHI 1
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0xaa, cpu.getD());
		assertEquals(14, cpu.getCycles());
	}
	
	public void testPLOn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xaa,	// LDI AA
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xa1,				// PLO 1
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0xaa5a, cpu.getR(1));
		assertEquals(10, cpu.getCycles());
	}
	
	public void testPHIn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xaa,	// LDI AA
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xa1,				// PLO 1
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0xaa5a, cpu.getR(1));
		assertEquals(10, cpu.getCycles());
	}
	
	public void testXRI() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x5a,	// LDI 5A
				(byte) 0xfb, 0x54,	// XRI 54
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x0e, cpu.getD());
		assertEquals(6, cpu.getCycles());
	}
	
	public void testORI() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x5a,	// LDI 5A
				(byte) 0xf9, 0x54,	// ORI 54
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x5e, cpu.getD());
		assertEquals(6, cpu.getCycles());
	}
	
	public void testANI() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x5a,	// LDI 5A
				(byte) 0xfa, 0x54,	// ANI 54
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x50, cpu.getD());
		assertEquals(6, cpu.getCycles());
	}
	
	public void testSTRn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x38,				// SKP
				0x00,				// <-- place holder
				0x11,				// INC 1 <-- R1 <- 1
				(byte) 0xf8, 0x5a,	// LDI 5A
				(byte) 0x51,		// STR 1
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x5a, ram.getByte(1));
		assertEquals(10, cpu.getCycles());
	}
	
	public void testSEXn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xe2,		// SEX 2
				0x00				// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x02, cpu.getX());
		assertEquals(4, cpu.getCycles());
	}
	
	public void testADD() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x38,						// SKP
				(byte) 0xe2,				// <- data
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0x01,	// LDI 01
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xf4,				// ADD
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x3c, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(14, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				0x38,						// SKP
				(byte) 0x02,				// <- data
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0x01,	// LDI 01
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xf4,				// ADD
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x5c, cpu.getD());
		assertEquals(0x00, cpu.getDF());
		assertEquals(14, cpu.getCycles());
	}
	
	public void testADI() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xfc, (byte) 0xe2,	// ADI E2
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x3c, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(6, cpu.getCycles());
	}
	
	public void testRET() throws UnknownOpcodeException {
		/*
		 * Init:
		 *   ram 0002: 0xa2
		 *   X: 01
		 *   R1: 01
		 *   R2: 0b
		 *   
		 * After RET:
		 *   X: 0a
		 *   P: 02
		 *   R1: 02
		 *   IE: 01
		 */
		ram.setBytes(0, new byte[] {
				0x38,						// SKP
				(byte) 0xa2,				// <- data
				(byte) 0xf8, (byte) 0x01,	// LDI 01
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, (byte) 0x0b,	// LDI 0b
				(byte) 0xa2,				// PLO 2
				(byte) 0xe1,				// SEX 1
				0x70,						// RET
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x0a, cpu.getX());
		assertEquals(2, cpu.getR(1));
		assertEquals(2, cpu.getP());
		assertEquals(0x01, cpu.getIE());
		
		assertFalse(outputDevice.isQOn());
		
		assertEquals(16, cpu.getCycles());
	}
	
	public void testBNF() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x38,						// SKP
				(byte) 0xe2,				// <- data
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0x01,	// LDI 01
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xf4,				// ADD
				0x3b, 0x0c,					// BNF 0c
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();

		assertTrue(outputDevice.isQOn());
		assertEquals(18, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				0x38,						// SKP
				(byte) 0x02,				// <- data
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0x01,	// LDI 01
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xf4,				// ADD
				0x3b, 0x0c,					// BNF 0c
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();

		assertFalse(outputDevice.isQOn());
		assertEquals(16, cpu.getCycles());
	}
	
	public void testLBNF() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x38,						// SKP
				(byte) 0xe2,				// <- data
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0x01,	// LDI 01
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xf4,				// ADD
				(byte) 0xcb, 0x00, 0x0d,	// LBNF 0D
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();

		assertTrue(outputDevice.isQOn());
		assertEquals(19, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				0x38,						// SKP
				(byte) 0x02,				// <- data
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0x01,	// LDI 01
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xf4,				// ADD
				(byte) 0xcb, 0x00, 0x0d,	// LBNF 0D
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();

		assertFalse(outputDevice.isQOn());
		assertEquals(17, cpu.getCycles());
	}
	
	public void testIRX() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xe1,	// SEX 1
				0x60,			// IRX
				0x60,			// IRX
				0x60,			// IRX
				0x00			// IDL
		});
		
		cpu.reset();
		cpu.run();

		assertEquals(3, cpu.getR(1));
		assertEquals(10, cpu.getCycles());
	}
	
	public void testADCI() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xfc, (byte) 0xe2,	// ADI E2
				0x7c, 0x02,					// ADCI 02
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x3f, cpu.getD());
		assertEquals(0x00, cpu.getDF());
		assertEquals(8, cpu.getCycles());
	}
	
	public void testSHL() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xa5,	// LDI a5
				(byte) 0xfe,				// SHL
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x4a, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(6, cpu.getCycles());
	}
	
	public void testSHLC() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xf0,	// LDI f0
				(byte) 0xfe,				// SHL
				(byte) 0xf8, (byte) 0xa5,	// LDI a5
				(byte) 0x7e,				// SHLC
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x4b, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(10, cpu.getCycles());
	}
	
	public void testSTXD() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, (byte) 0x0a,	// LDI 0a
				(byte) 0xa1,				// PLO 1
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0xa5,	// LDI a5
				0x73,						// STXD
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x09, cpu.getR(1));
		assertEquals(0xa5, ram.getUnsignedByte(0x0a));
		assertEquals(16, cpu.getCycles());
	}
	
	public void testLDAn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, (byte) 0x07,	// LDI 07
				(byte) 0xa1,				// PLO 1
				0x41,						// LDA 1
				0x00,						// IDL
				(byte) 0xa5					// <- data to load into D
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x08, cpu.getR(1));
		assertEquals(0xa5, cpu.getD());
		assertEquals(12, cpu.getCycles());
	}
	
	public void testLDNn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, (byte) 0x07,	// LDI 07
				(byte) 0xa1,				// PLO 1
				0x01,						// LDN 1
				0x00,						// IDL
				(byte) 0xa5					// <- data to load into D
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x07, cpu.getR(1));
		assertEquals(0xa5, cpu.getD());
		assertEquals(12, cpu.getCycles());
	}
	
	public void testLDX() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, (byte) 0x08,	// LDI 08
				(byte) 0xa1,				// PLO 1
				(byte) 0xe1,				// SEX 1
				(byte) 0xf0,				// LDX
				0x00,						// IDL
				(byte) 0xa5					// <- data to load into D
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0xa5, cpu.getD());
		assertEquals(14, cpu.getCycles());
	}
	
	public void testLDXA() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, (byte) 0x08,	// LDI 08
				(byte) 0xa1,				// PLO 1
				(byte) 0xe1,				// SEX 1
				0x72,						// LDXA
				0x00,						// IDL
				(byte) 0xa5					// <- data to load into D
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0xa5, cpu.getD());
		assertEquals(0x09, cpu.getR(1));
		assertEquals(14, cpu.getCycles());
	}
	
	public void testOR() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, (byte) 0x0a,	// LDI 0a
				(byte) 0xa1,				// PLO 1
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xf1,				// OR
				0x00,						// IDL
				(byte) 0xa5					// <- data
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0xff, cpu.getD());
		assertEquals(16, cpu.getCycles());
	}
	
	public void testAND() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, (byte) 0x0a,	// LDI 0a
				(byte) 0xa1,				// PLO 1
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xf2,				// AND
				0x00,						// IDL
				(byte) 0xa5					// <- data
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x00, cpu.getD());
		assertEquals(16, cpu.getCycles());
	}
	
	public void testXOR() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, (byte) 0x0a,	// LDI 0a
				(byte) 0xa1,				// PLO 1
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xf3,				// XOR
				0x00,						// IDL
				(byte) 0x54					// <- data
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x0e, cpu.getD());
		assertEquals(16, cpu.getCycles());
	}
	
	public void testBDF() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7b,						// SEQ
				(byte) 0xf8, (byte) 0xa5,	// LDI a5
				(byte) 0xfe,				// SHL
				(byte) 0x33, 0x07,			// BDF 07
				0x7a,						// REQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(10, cpu.getCycles());
		
		ram.setBytes(0, new byte[] {
				0x7b,						// SEQ
				(byte) 0xf8, (byte) 0x75,	// LDI 75
				(byte) 0xfe,				// SHL
				(byte) 0x33, 0x07,			// BDF 07
				0x7a,						// REQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(12, cpu.getCycles());
	}
	
	public void testLBDF() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x7b,						// SEQ
				(byte) 0xf8, (byte) 0xa5,	// LDI a5
				(byte) 0xfe,				// SHL
				(byte) 0xc3, 0x00,	0x08,	// LBDF 0008
				0x7a,						// REQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(11, cpu.getCycles());
		
		ram.setBytes(0, new byte[] {
				0x7b,						// SEQ
				(byte) 0xf8, (byte) 0x75,	// LDI 75
				(byte) 0xfe,				// SHL
				(byte) 0xc3, 0x00, 0x08,	// LBDF 0008
				0x7a,						// REQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(13, cpu.getCycles());
	}
	
	public void testLSDF() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xf0,	// LDI f0
				(byte) 0xfe,				// SHL
				(byte) 0xcf,				// LSDF
				0x7b,						// SEQ
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(9, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0x0f,	// LDI 0f
				(byte) 0xfe,				// SHL
				(byte) 0xcf,				// LSDF
				0x7b,						// SEQ
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(13, cpu.getCycles());
	}

	public void testLSNF() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0x0f,	// LDI 0f
				(byte) 0xfe,				// SHL
				(byte) 0xc7,				// LSNF
				0x7b,						// SEQ
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(9, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xf0,	// LDI f0
				(byte) 0xfe,				// SHL
				(byte) 0xc7,				// LSNF
				0x7b,						// SEQ
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(13, cpu.getCycles());
	}
	
	public void testADC() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, 0x0d,			// LDI 0d
				(byte) 0xa1,				// PLO 1
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0x80,	// LDI 80
				(byte) 0xfe,				// SHL
				(byte) 0xf8, 0x5a,			// LDI 5a
				0x74,						// ADC
				0x00,						// IDL
				(byte) 0xa6					// <- data
		});
		
		cpu.reset();
		cpu.run();

		assertEquals(0x01, cpu.getD());
		assertEquals(1, cpu.getDF());
		assertEquals(20, cpu.getCycles());
	}
	
	public void testSD() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x38,						// SKP
				(byte) 0xe2,				// <- data
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0x01,	// LDI 01
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xf5,				// SD
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x88, cpu.getD());
		assertEquals(0x00, cpu.getDF());
		assertEquals(14, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				0x38,						// SKP
				(byte) 0x5a,				// <- data
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0x01,	// LDI 01
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, (byte) 0xe2,	// LDI E2
				(byte) 0xf5,				// SD
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x78, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(14, cpu.getCycles());
	}
	
	public void testSDI() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xfd, (byte) 0xe2,	// SDI E2
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x88, cpu.getD());
		assertEquals(0x00, cpu.getDF());
		assertEquals(6, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xe2,	// LDI e2
				(byte) 0xfd, 0x5a,			// SDI 5a
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x78, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(6, cpu.getCycles());
	}
	
	public void testSDBI() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x0f,			// LDI 0F
				(byte) 0xfe,				// SHL
				(byte) 0xf8, 0x5a,			// LDI 5A
				0x7d, (byte) 0xe2,			// SDBI E2
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x87, cpu.getD());
		assertEquals(0x00, cpu.getDF());
		assertEquals(10, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xf0,	// LDI F0
				(byte) 0xfe,				// SHL
				(byte) 0xf8, (byte) 0xe2,	// LDI e2
				0x7d, 0x5a,					// SDBI 5a
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x78, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(10, cpu.getCycles());
	}
	
	public void testSDB() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, 0x0d,			// LDI 0d
				(byte) 0xa1,				// PLO 1
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, 0x0f,			// LDI 0F
				(byte) 0xfe,				// SHL
				(byte) 0xf8, 0x5a,			// LDI 5A
				0x75,						// SDB
				0x00,						// IDL
				(byte) 0xe2					// <- data
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x87, cpu.getD());
		assertEquals(0x00, cpu.getDF());
		assertEquals(20, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, 0x0d,			// LDI 0d
				(byte) 0xa1,				// PLO 1
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0xf0,	// LDI F0
				(byte) 0xfe,				// SHL
				(byte) 0xf8, (byte) 0xe2,	// LDI e2
				0x75,						// SDB
				0x00,						// IDL
				0x5a						// <- data
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x78, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(20, cpu.getCycles());
	}
	
	public void testSM() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				0x38,						// SKP
				(byte) 0xe2,				// <- data
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0x01,	// LDI 01
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xf7,				// SM
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x78, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(14, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				0x38,						// SKP
				(byte) 0x5a,				// <- data
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0x01,	// LDI 01
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, (byte) 0xe2,	// LDI E2
				(byte) 0xf7,				// SM
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x88, cpu.getD());
		assertEquals(0x00, cpu.getDF());
		assertEquals(14, cpu.getCycles());
	}
	
	public void testSMI() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, 0x5a,			// LDI 5A
				(byte) 0xff, (byte) 0xe2,	// SMI E2
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x78, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(6, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xe2,	// LDI e2
				(byte) 0xff, 0x5a,			// SMI 5a
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x88, cpu.getD());
		assertEquals(0x00, cpu.getDF());
		assertEquals(6, cpu.getCycles());
	}

	public void testSMB() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, 0x0d,			// LDI 0d
				(byte) 0xa1,				// PLO 1
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0x0f,	// LDI 0f
				(byte) 0xfe,				// SHL
				(byte) 0xf8, 0x5a,			// LDI 5A
				0x77,						// SMB
				0x00,						// IDL
				(byte) 0xe2					// <- data
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x77, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(20, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, 0x0d,			// LDI 0d
				(byte) 0xa1,				// PLO 1
				(byte) 0xe1,				// SEX 1
				(byte) 0xf8, (byte) 0xf0,	// LDI f0
				(byte) 0xfe,				// SHL
				(byte) 0xf8, (byte) 0xe2,	// LDI e2
				0x77,						// SMB
				0x00,						// IDL
				(byte) 0x5a					// <- data
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x88, cpu.getD());
		assertEquals(0x00, cpu.getDF());
		assertEquals(20, cpu.getCycles());
	}
	
	public void testSMBI() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0x0f,	// LDI 0f
				(byte) 0xfe,				// SHL
				(byte) 0xf8, 0x5a,			// LDI 5A
				0x7f, (byte) 0xe2,			// SMBI e2
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x77, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(10, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xf0,	// LDI f0
				(byte) 0xfe,				// SHL
				(byte) 0xf8, (byte) 0xe2,	// LDI e2
				0x7f, 0x5a,					// SMBI 5a
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x88, cpu.getD());
		assertEquals(0x00, cpu.getDF());
		assertEquals(10, cpu.getCycles());
	}
	
	public void testSHR() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xa5,	// LDI a5
				(byte) 0xf6,				// SHR
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x52, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(6, cpu.getCycles());
	}
	
	public void testSHRC() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xf8, (byte) 0xf0,	// LDI f0
				(byte) 0xfe,				// SHL
				(byte) 0xf8, (byte) 0xa5,	// LDI a5
				(byte) 0x76,				// SHC
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0xd2, cpu.getD());
		assertEquals(0x01, cpu.getDF());
		assertEquals(10, cpu.getCycles());
	}
	
	public void testSEPn() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xf8, 0x08,			// LDI 08
				(byte) 0xa1,				// PLO 1
				(byte) 0xd1,				// SEP 1
				(byte) 0xc4,				// NOP
				(byte) 0xc4,				// NOP
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(12, cpu.getCycles());
	}

	public void testDIS() throws UnknownOpcodeException {
		/*
		 * Init:
		 *   ram 0002: 0xa2
		 *   X: 01
		 *   R1: 01
		 *   R2: 0b
		 *   
		 * After DIS:
		 *   X: 0a
		 *   P: 02
		 *   R1: 02
		 *   IE: 00
		 */
		ram.setBytes(0, new byte[] {
				0x38,						// SKP
				(byte) 0xa2,				// <- data
				(byte) 0xf8, (byte) 0x01,	// LDI 01
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, (byte) 0x0b,	// LDI 0b
				(byte) 0xa2,				// PLO 2
				(byte) 0xe1,				// SEX 1
				0x71,						// DIS
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x0a, cpu.getX());
		assertEquals(2, cpu.getR(1));
		assertEquals(2, cpu.getP());
		assertEquals(0, cpu.getIE());
		
		assertFalse(outputDevice.isQOn());
		
		assertEquals(16, cpu.getCycles());
	}
	
	public void testLSIE() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0xcc,				// LSIE
				0x7b,						// SEQ
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertFalse(outputDevice.isQOn());
		assertEquals(5, cpu.getCycles());

		ram.setBytes(0, new byte[] {
				0x38,						// SKP
				(byte) 0xa0,				// <- data
				(byte) 0xf8, (byte) 0x01,	// LDI 01
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, (byte) 0x0b,	// LDI 0b
				(byte) 0xa2,				// PLO 2
				(byte) 0xe1,				// SEX 1
				0x71,						// DIS
				(byte) 0xcc,				// LSIE
				0x7b,						// SEQ
				0x7b,						// SEQ
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertTrue(outputDevice.isQOn());
		assertEquals(23, cpu.getCycles());
	}

	public void testMARK() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb2,				// PHI 2
				(byte) 0xf8, 0x08,			// LDI 08
				(byte) 0xa2,				// PLO 2
				(byte) 0xe1,				// SEX 1
				0x79,						// MARK
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x10, cpu.getT());
		assertEquals(0x10, ram.getUnsignedByte(0x08));
		assertEquals(0x00, cpu.getX());
		assertEquals(14, cpu.getCycles());
	}

	public void testSAV() throws UnknownOpcodeException {
		ram.setBytes(0, new byte[] {
				(byte) 0x90,				// GHI 0
				(byte) 0xb1,				// PHI 1
				(byte) 0xb2,				// PHI 2
				(byte) 0xa1,				// PLO 1
				(byte) 0xf8, 0x08,			// LDI 08
				(byte) 0xa2,				// PLO 2
				(byte) 0xe1,				// SEX 1
				0x79,						// MARK
				(byte) 0xe1,				// SEX 1
				0x78,						// SAV
				0x00						// IDL
		});
		
		cpu.reset();
		cpu.run();
		
		assertEquals(0x10, cpu.getT());
		assertEquals(0x10, ram.getUnsignedByte(0x08));
		assertEquals(0x10, ram.getUnsignedByte(0x00));
		assertEquals(22, cpu.getCycles());
	}

}
