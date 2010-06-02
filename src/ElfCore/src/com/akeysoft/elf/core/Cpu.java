package com.akeysoft.elf.core;

import java.util.Date;

// ref: http://www.cosmacelf.com/shortcourse.htm
// ref: http://www.ittybittycomputers.com/IttyBitty/ShortCor.htm
// ref: http://homepage.mac.com/ruske/tinyelf/tinyelfhelp/tinyelfhelp.html
public class Cpu {

	public Cpu(Ram ram, InputDevice inputDevice, OutputDevice outputDevice) {
		this.stepMode = false;
		this.pauseAfterCycles = 0L;
		this.lastPauseCycles = 0L;
		this.stopAfterCycles = 0L;
		
		this.r = new int[16];
		this.ef = new boolean[4];
		
		this.ram = ram;
		this.inputDevice = inputDevice;
		this.outputDevice = outputDevice;
		
		this.stop = true;

		this.reset();
	}

	// Clock: 1.79M
	// 1790 / 8
	private static long CYCLES_PER_MS = 224;
	
	private boolean stepMode;
	private long cycles;
	private long pauseAfterCycles;
	private long lastPauseCycles;
	private long stopAfterCycles;
	private long lastPauseAtMs;
	
	private int d; // 8-bit
	private int[] r; // 16-bit each
	private int p, x; // 4-bit each
	private int t; // 8-bit
	private int df; // 1-bit
	private int ie; // 1-bit
	
	private boolean[] ef;
	
	private Ram ram;
	private OutputDevice outputDevice;
	private InputDevice inputDevice;
	
	boolean idle;
	
	boolean stop;
	
	public void stop() {
		this.stop = true;
	}
	
	public boolean isStop() {
		return this.stop;
	}
	
	public void setStepMode(boolean stepMode) {
		this.stepMode = stepMode;
	}
	
	public boolean isStepMode() {
		return this.stepMode;
	}
	
	public long getCycles() {
		return this.cycles;
	}
	
	private void setPauseAfterCycles(long cycles) {
		this.lastPauseCycles = this.cycles; 
		this.pauseAfterCycles = cycles;
	}
	
	public void setStopAfterCycles(long cycles) {
		this.stopAfterCycles = cycles;
	}
	
	public long getPauseAfterCycles() {
		return this.pauseAfterCycles;
	}
	
	public void reset() {
		this.cycles = 0;
		
		this.outputDevice.setQ(false);
		this.ie = 1;
		this.x = 0;
		this.p = 0;
		this.r[0] = 0;
		
		this.idle = false;
	}
	
	// index: 0-3, maps EF1 - EF4.
	public void setEf(int index, boolean value) {
		this.ef[index] = value;
	}
	
	public int getD() {
		return this.d;
	}
	
	public int getDF() {
		return this.df;
	}
	
	public int getX() {
		return this.x;
	}
	
	public int getP() {
		return this.p;
	}
	
	public int getR(int index) {
		return this.r[index] & 0xFFFF;
	}
	
	public int getIE() {
		return this.ie;
	}
	
	public int getT() {
		return this.t;
	}
	
	public boolean isIdle() {
		return this.idle;
	}
	
	private String toHex(int i, int width) {
		String result = Integer.toHexString(i);
		while (result.length() < width) {
			result = "0" + result;
		}
		
		return result;
	}
	
	public void run() throws UnknownOpcodeException {
		this.stop = false;
		
		this.lastPauseAtMs = (new Date()).getTime();
		this.setPauseAfterCycles(CYCLES_PER_MS);
		
		while ((!this.idle) && (!this.stop)) {
			step();
			
			if (this.stepMode)
				break;
			
			long cyclesSinceLastPause = this.cycles - this.lastPauseCycles;
			if (this.pauseAfterCycles > 0L && cyclesSinceLastPause > this.pauseAfterCycles) {
				long currentInMs = (new Date()).getTime();
				while (currentInMs - this.lastPauseAtMs == 0) {
					try {
						Thread.sleep(1L);
						currentInMs = (new Date()).getTime();
					} catch (InterruptedException e) {
					}
				}
				
				this.setPauseAfterCycles(CYCLES_PER_MS * (currentInMs - this.lastPauseAtMs));
				this.lastPauseAtMs = currentInMs;
			}
			
			if (this.stopAfterCycles > 0L && this.cycles > this.stopAfterCycles)
				break;
		}
	}

	private void step() throws UnknownOpcodeException {
		int pc = this.r[this.p] & 0xFFFF;
		byte opcode = this.ram.getByte(pc);
		this.r[this.p] ++;
		this.r[this.p] &= 0xFFFF; 
		
		switch (opcode) {
		case 0x00:
			this.execIDL(opcode);
			break;
		
		case 0x01:
		case 0x02:
		case 0x03:
		case 0x04:
		case 0x05:
		case 0x06:
		case 0x07:
		case 0x08:
		case 0x09:
		case 0x0a:
		case 0x0b:
		case 0x0c:
		case 0x0d:
		case 0x0e:
		case 0x0f:
			this.execLDNn(opcode);
			break;
			
		case 0x10:
		case 0x11:
		case 0x12:
		case 0x13:
		case 0x14:
		case 0x15:
		case 0x16:
		case 0x17:
		case 0x18:
		case 0x19:
		case 0x1a:
		case 0x1b:
		case 0x1c:
		case 0x1d:
		case 0x1e:
		case 0x1f:
			this.execINCn(opcode);
			break;
			
		case 0x20:
		case 0x21:
		case 0x22:
		case 0x23:
		case 0x24:
		case 0x25:
		case 0x26:
		case 0x27:
		case 0x28:
		case 0x29:
		case 0x2a:
		case 0x2b:
		case 0x2c:
		case 0x2d:
		case 0x2e:
		case 0x2f:
			this.execDECn(opcode);
			break;
			
		case 0x30:
			this.execBR(opcode);
			break;
			
		case 0x31:
			this.execBQ(opcode);
			break;
			
		case 0x32:
			this.execBZ(opcode);
			break;
			
		case 0x33:
			this.execBDF(opcode);
			break;
			
		case 0x34:
		case 0x35:
		case 0x36:
		case 0x37:
			this.execBn(opcode);
			break;
		
		case 0x38:
			this.execSKP(opcode);
			break;
			
		case 0x39:
			this.execBNQ(opcode);
			break;
			
		case 0x3a:
			this.execBNZ(opcode);
			break;
			
		case 0x3b:
			this.execBNF(opcode);
			break;
			
		case 0x3c:
		case 0x3d:
		case 0x3e:
		case 0x3f:
			this.execBNn(opcode);
			break;
			
		case (byte) 0x40:
		case (byte) 0x41:
		case (byte) 0x42:
		case (byte) 0x43:
		case (byte) 0x44:
		case (byte) 0x45:
		case (byte) 0x46:
		case (byte) 0x47:
		case (byte) 0x48:
		case (byte) 0x49:
		case (byte) 0x4a:
		case (byte) 0x4b:
		case (byte) 0x4c:
		case (byte) 0x4d:
		case (byte) 0x4e:
		case (byte) 0x4f:
			this.execLDAn(opcode);
			break;
			
		case 0x50:
		case 0x51:
		case 0x52:
		case 0x53:
		case 0x54:
		case 0x55:
		case 0x56:
		case 0x57:
		case 0x58:
		case 0x59:
		case 0x5a:
		case 0x5b:
		case 0x5c:
		case 0x5d:
		case 0x5e:
		case 0x5f:
			this.execSTRn(opcode);
			break;
			
		case 0x60:
			this.execIRX(opcode);
			break;
			
		case 0x61:
		case 0x62:
		case 0x63:
		case 0x64:
		case 0x65:
		case 0x66:
		case 0x67:
			this.execOUTn(opcode);
			break;
			
		case 0x69:
		case 0x6a:
		case 0x6b:
		case 0x6c:
		case 0x6d:
		case 0x6e:
		case 0x6f:
			this.execINPn(opcode);
			break;
			
		case 0x70:
			this.execRET(opcode);
			break;
			
		case 0x71:
			this.execDIS(opcode);
			break;
			
		case 0x72:
			this.execLDXA(opcode);
			break;
			
		case 0x73:
			this.execSTXD(opcode);
			break;
			
		case 0x74:
			this.execADC(opcode);
			break;
			
		case 0x75:
			this.execSDB(opcode);
			break;
			
		case 0x76:
			this.execSHRC(opcode);
			break;
			
		case 0x77:
			this.execSMB(opcode);
			break;
			
		case 0x78:
			this.execSAV(opcode);
			break;
			
		case 0x79:
			this.execMARK(opcode);
			break;
			
		case 0x7a:
			this.execREQ(opcode);
			break;
		
		case 0x7b:
			this.execSEQ(opcode);
			break;
			
		case 0x7c:
			this.execADCI(opcode);
			break;
			
		case 0x7d:
			this.execSDBI(opcode);
			break;
			
		case 0x7e:
			this.execSHLC(opcode);
			break;
			
		case 0x7f:
			this.execSMBI(opcode);
			break;
			
		case (byte) 0x80:
		case (byte) 0x81:
		case (byte) 0x82:
		case (byte) 0x83:
		case (byte) 0x84:
		case (byte) 0x85:
		case (byte) 0x86:
		case (byte) 0x87:
		case (byte) 0x88:
		case (byte) 0x89:
		case (byte) 0x8a:
		case (byte) 0x8b:
		case (byte) 0x8c:
		case (byte) 0x8d:
		case (byte) 0x8e:
		case (byte) 0x8f:
			this.execGLOn(opcode);
			break;
			
		case (byte) 0x90:
		case (byte) 0x91:
		case (byte) 0x92:
		case (byte) 0x93:
		case (byte) 0x94:
		case (byte) 0x95:
		case (byte) 0x96:
		case (byte) 0x97:
		case (byte) 0x98:
		case (byte) 0x99:
		case (byte) 0x9a:
		case (byte) 0x9b:
		case (byte) 0x9c:
		case (byte) 0x9d:
		case (byte) 0x9e:
		case (byte) 0x9f:
			this.execGHIn(opcode);
			break;
			
		case (byte) 0xa0:
		case (byte) 0xa1:
		case (byte) 0xa2:
		case (byte) 0xa3:
		case (byte) 0xa4:
		case (byte) 0xa5:
		case (byte) 0xa6:
		case (byte) 0xa7:
		case (byte) 0xa8:
		case (byte) 0xa9:
		case (byte) 0xaa:
		case (byte) 0xab:
		case (byte) 0xac:
		case (byte) 0xad:
		case (byte) 0xae:
		case (byte) 0xaf:
			this.execPLOn(opcode);
			break;
			
		case (byte) 0xb0:
		case (byte) 0xb1:
		case (byte) 0xb2:
		case (byte) 0xb3:
		case (byte) 0xb4:
		case (byte) 0xb5:
		case (byte) 0xb6:
		case (byte) 0xb7:
		case (byte) 0xb8:
		case (byte) 0xb9:
		case (byte) 0xba:
		case (byte) 0xbb:
		case (byte) 0xbc:
		case (byte) 0xbd:
		case (byte) 0xbe:
		case (byte) 0xbf:
			this.execPHIn(opcode);
			break;
			
		case (byte) 0xc0:
			this.execLBR(opcode);
			break;
		
		case (byte) 0xc1:
			this.execLBQ(opcode);
			break;
			
		case (byte) 0xc2:
			this.execLBZ(opcode);
			break;
			
		case (byte) 0xc3:
			this.execLBDF(opcode);
			break;
			
		case (byte) 0xc4:
			this.execNOP(opcode);
			break;
			
		case (byte) 0xc5:
			this.execLSNQ(opcode);
			break;
			
		case (byte) 0xc6:
			this.execLSNZ(opcode);
			break;
			
		case (byte) 0xc7:
			this.execLSNF(opcode);
			break;
			
		case (byte) 0xc8:
			this.execLSKP(opcode);
			break;
			
		case (byte) 0xc9:
			this.execLBNQ(opcode);
			break;
			
		case (byte) 0xca:
			this.execLBNZ(opcode);
			break;
			
		case (byte) 0xcb:
			this.execLBNF(opcode);
			break;
			
		case (byte) 0xcc:
			this.execLSIE(opcode);
			break;
			
		case (byte) 0xcd:
			this.execLSQ(opcode);
			break;
			
		case (byte) 0xce:
			this.execLSZ(opcode);
			break;
			
		case (byte) 0xcf:
			this.execLSDF(opcode);
			break;
			
		case (byte) 0xd0:
		case (byte) 0xd1:
		case (byte) 0xd2:
		case (byte) 0xd3:
		case (byte) 0xd4:
		case (byte) 0xd5:
		case (byte) 0xd6:
		case (byte) 0xd7:
		case (byte) 0xd8:
		case (byte) 0xd9:
		case (byte) 0xda:
		case (byte) 0xdb:
		case (byte) 0xdc:
		case (byte) 0xdd:
		case (byte) 0xde:
		case (byte) 0xdf:
			this.execSEPn(opcode);
			break;
			
		case (byte) 0xe0:
		case (byte) 0xe1:
		case (byte) 0xe2:
		case (byte) 0xe3:
		case (byte) 0xe4:
		case (byte) 0xe5:
		case (byte) 0xe6:
		case (byte) 0xe7:
		case (byte) 0xe8:
		case (byte) 0xe9:
		case (byte) 0xea:
		case (byte) 0xeb:
		case (byte) 0xec:
		case (byte) 0xed:
		case (byte) 0xee:
		case (byte) 0xef:
			this.execSEXn(opcode);
			break;
			
		case (byte) 0xf0:
			this.execLDX(opcode);
			break;
			
		case (byte) 0xf1:
			this.execOR(opcode);
			break;
			
		case (byte) 0xf2:
			this.execAND(opcode);
			break;
			
		case (byte) 0xf3:
			this.execXOR(opcode);
			break;
			
		case (byte) 0xf4:
			this.execADD(opcode);
			break;
			
		case (byte) 0xf5:
			this.execSD(opcode);
			break;
			
		case (byte) 0xf6:
			this.execSHR(opcode);
			break;
			
		case (byte) 0xf7:
			this.execSM(opcode);
			break;
			
		case (byte) 0xf8:
			this.execLDI(opcode);
			break;
			
		case (byte) 0xf9:
			this.execORI(opcode);
			break;
			
		case (byte) 0xfa:
			this.execANI(opcode);
			break;
			
		case (byte) 0xfb:
			this.execXRI(opcode);
			break;
			
		case (byte) 0xfc:
			this.execADI(opcode);
			break;
			
		case (byte) 0xfd:
			this.execSDI(opcode);
			break;
			
		case (byte) 0xfe:
			this.execSHL(opcode);
			break;
			
		case (byte) 0xff:
			this.execSMI(opcode);
			break;
			
		default:
			throw new UnknownOpcodeException(toHex(opcode & 0xff, 2));
		}
	}
	
	private void execSAV(byte opcode) {
		/*
		SAV     SAVe T                                             78
		-------------------------------------------------------------
		Copy the contents of the T register into the memory location
		pointed to by the address register pointed to by X.
		*/
		this.ram.setByte(this.r[this.x] & 0x00FFFF, (byte) this.t);
		
		this.cycles += 2;
	}
	
	private void execMARK(byte opcode) {
		/*
		MARK    Save X and P in T                                  79
		-------------------------------------------------------------
		Copy X and P into the T register, store T in the memory byte
		pointed to by R2, then decrement R2; finally copy P into X.
		*/
		this.t = ((this.x << 4) & 0xF0) | (this.p & 0x0F);
		this.ram.setByte(this.r[2] & 0x00FFFF, (byte) this.t);
		this.x = this.p;
		
		this.cycles += 2;
	}

	private void execLSIE(byte opcode) {
		/*
		LSIE    Long Skip if Interrupts are Enabled                CC
		-------------------------------------------------------------
		If the internal IE flag enabling interrupts is True=1, skip
		the next two instruction bytes; if False=0, take the next
		instruction in sequence.
		*/
		if (this.ie != 0) {
			this.r[this.p] += 2;
			this.r[this.p] &= 0x00FFFF; 
		}
		
		this.cycles += 3;
	}

	private void execDIS(byte opcode) {
		/*
		DIS     Return and DISable interrupts                      71
		-------------------------------------------------------------
		Read the byte from the memory location pointed to by the
		register pointed to by X; then increment that register. Copy
		the right 4 bits of the byte read into P, and the left four
		bits into X. Then disable interrupts (set IE to 0).
		*/
		int data = this.ram.getUnsignedByte(this.r[this.x] & 0x00FFFF);
		this.r[this.x] ++;
		this.r[this.x] &= 0x00FFFF;
		
		this.p = data & 0x0F;
		this.x = ((data & 0x00F0) >> 4) & 0x0F;
		
		this.ie = 0;
		
		this.cycles += 2;		
	}

	private void execSEPn(byte opcode) {
		/*
		SEP r   SEt P                                              Dr
		-------------------------------------------------------------
		Copy r (the low four bits of the instruction) into P, making
		the designated address register the new Program Counter.
		*/
		this.p = (opcode & 0x0F);
		
		this.cycles += 2;
	}

	private void execSHRC(byte opcode) {
		/*
		SHR     SHift D Right                                      F6
		SHRC    SHift D Right with Carry                           76
		-------------------------------------------------------------
		Move each bit in the accumulator D one bit position to the
		right. The rightmost bit of D is moved into DF. The leftmost
		bit of the accumulator is filled with zero (SHR) or with the
		previous contents of DF (SHRC).
		*/
		int oldDf = this.df;
		
		this.df = ((this.d & 0x01) == 0) ? 0: 1;
		this.d = this.d >> 1;
		
		if (oldDf != 0)
			this.d |= 0x80;
		
		this.d &= 0x00FF;
		
		this.cycles += 2;
	}

	private void execSHR(byte opcode) {
		/*
		SHR     SHift D Right                                      F6
		SHRC    SHift D Right with Carry                           76
		-------------------------------------------------------------
		Move each bit in the accumulator D one bit position to the
		right. The rightmost bit of D is moved into DF. The leftmost
		bit of the accumulator is filled with zero (SHR) or with the
		previous contents of DF (SHRC).
		*/
		this.df = ((this.d & 0x01) == 0) ? 0: 1;
		this.d = (this.d >> 1) & 0x00FF;
		
		this.cycles += 2;
	}

	private void execSMBI(byte opcode) {
		/*
		SM      Subtract Memory byte from D                        F7
		SMI b   Subtract Memory from D, Immediate               FF bb
		SMB     Subtract Memory with Borrow from D                 77
		SMBI b  Subtract Memory with Borrow from D, Immediate   7F bb
		-------------------------------------------------------------
		Complement the memory byte pointed to by the address register
		pointed to by X (or the second byte of the instruction) and
		add it plus 1 (or the DF register) to the accumulator. Put
		the sum in the accumulator, and the carry out of the sum into
		DF. This is equivalent to subtracting the immediate or memory
		byte from D, with or without consideration of a borrow
		incurred by a previous subtraction.
		*/
		int sum = this.d + (~(this.ram.getByte(this.r[this.p] & 0x00FFFF) & 0x00FF));
		
		if (this.df != 0)
			sum += 1;
		
		this.d = sum & 0x00FF;
		this.df = (sum & 0xFF00) != 0 ? 1 : 0;

		this.r[this.p] ++;
		this.r[this.p] &= 0x00FFFF;
		
		this.cycles += 2;
	}

	private void execSMB(byte opcode) {
		/*
		SM      Subtract Memory byte from D                        F7
		SMI b   Subtract Memory from D, Immediate               FF bb
		SMB     Subtract Memory with Borrow from D                 77
		SMBI b  Subtract Memory with Borrow from D, Immediate   7F bb
		-------------------------------------------------------------
		Complement the memory byte pointed to by the address register
		pointed to by X (or the second byte of the instruction) and
		add it plus 1 (or the DF register) to the accumulator. Put
		the sum in the accumulator, and the carry out of the sum into
		DF. This is equivalent to subtracting the immediate or memory
		byte from D, with or without consideration of a borrow
		incurred by a previous subtraction.
		*/
		int sum = this.d + (~(this.ram.getByte(this.r[this.x] & 0x00FFFF) & 0x00FF));
		
		if (this.df != 0)
			sum += 1;
		
		this.d = sum & 0x00FF;
		this.df = (sum & 0xFF00) != 0 ? 1 : 0;

		this.cycles += 2;
	}

	private void execSMI(byte opcode) {
		/*
		SM      Subtract Memory byte from D                        F7
		SMI b   Subtract Memory from D, Immediate               FF bb
		SMB     Subtract Memory with Borrow from D                 77
		SMBI b  Subtract Memory with Borrow from D, Immediate   7F bb
		-------------------------------------------------------------
		Complement the memory byte pointed to by the address register
		pointed to by X (or the second byte of the instruction) and
		add it plus 1 (or the DF register) to the accumulator. Put
		the sum in the accumulator, and the carry out of the sum into
		DF. This is equivalent to subtracting the immediate or memory
		byte from D, with or without consideration of a borrow
		incurred by a previous subtraction.
		*/
		int sum = this.d + (~(this.ram.getByte(this.r[this.p] & 0x00FFFF) & 0x00FF)) + 1;
		
		this.d = sum & 0x00FF;
		this.df = (sum & 0xFF00) != 0 ? 1 : 0;

		this.r[this.p] ++;
		this.r[this.p] &= 0x00FFFF;
		
		this.cycles += 2;
	}

	private void execSM(byte opcode) {
		/*
		SM      Subtract Memory byte from D                        F7
		SMI b   Subtract Memory from D, Immediate               FF bb
		SMB     Subtract Memory with Borrow from D                 77
		SMBI b  Subtract Memory with Borrow from D, Immediate   7F bb
		-------------------------------------------------------------
		Complement the memory byte pointed to by the address register
		pointed to by X (or the second byte of the instruction) and
		add it plus 1 (or the DF register) to the accumulator. Put
		the sum in the accumulator, and the carry out of the sum into
		DF. This is equivalent to subtracting the immediate or memory
		byte from D, with or without consideration of a borrow
		incurred by a previous subtraction.
		*/
		int sum = this.d + (~(this.ram.getByte(this.r[this.x] & 0x00FFFF) & 0x00FF)) + 1;
		
		this.d = sum & 0x00FF;
		this.df = (sum & 0xFF00) != 0 ? 1 : 0;

		this.cycles += 2;
	}

	private void execSDB(byte opcode) {
		/*
		SDBI b  Subtract D w.Borrow from memory Immediate byte  7D bb
		SDB     Subtract D with Borrow from memory                 75
		-------------------------------------------------------------
		Complement the accumulator and add it plus the value of the
		DF register to the second byte of the instruction (or the
		memory byte pointed to by the address register pointed to by
		X). Put the sum into the accumulator and the carry out of the
		sum back into DF. This instruction is used to extend a borrow
		from a previous SD or SDB (or SDI or SDBI).
		*/
		int sum = (~this.d) + (this.ram.getByte(this.r[this.x] & 0x00FFFF) & 0x00FF);
		if (this.df != 0)
			sum += 1;
		
		this.d = sum & 0x00FF;
		this.df = (sum & 0xFF00) != 0 ? 1 : 0;
		
		this.cycles += 2;
	}

	private void execSDBI(byte opcode) {
		/*
		SDBI b  Subtract D w.Borrow from memory Immediate byte  7D bb
		SDB     Subtract D with Borrow from memory                 75
		-------------------------------------------------------------
		Complement the accumulator and add it plus the value of the
		DF register to the second byte of the instruction (or the
		memory byte pointed to by the address register pointed to by
		X). Put the sum into the accumulator and the carry out of the
		sum back into DF. This instruction is used to extend a borrow
		from a previous SD or SDB (or SDI or SDBI).
		*/
		int sum = (~this.d) + (this.ram.getByte(this.r[this.p] & 0x00FFFF) & 0x00FF);
		if (this.df != 0)
			sum += 1;
		
		this.d = sum & 0x00FF;
		this.df = (sum & 0xFF00) != 0 ? 1 : 0;
		
		this.r[this.p] ++;
		this.r[this.p] &= 0x00FFFF;
		
		this.cycles += 2;
	}

	private void execSDI(byte opcode) {
		/*
		SDI b   Subtract D from Immediate byte                  FD bb
		-------------------------------------------------------------
		Complement the accumulator and add it plus one to the second
		byte of the instruction. The sum is put back into the
		accumulator, and the carry out is placed into DF.
		*/
		int sum = (~this.d) + (this.ram.getByte(this.r[this.p] & 0x00FFFF) & 0x00FF) + 1;
		this.d = sum & 0x00FF;
		this.df = (sum & 0xFF00) != 0 ? 1 : 0;
		
		this.r[this.p] ++;
		this.r[this.p] &= 0x00FFFF;
		
		this.cycles += 2;
	}

	private void execSD(byte opcode) {
		/*
		SD      Subtract D from memory                             F5
		-------------------------------------------------------------
		Add the memory byte pointed to by the register pointed to by
		X, to the complement of the accumulator plus 1. The sum is
		put back into the accumulator, and the carry out of the sum
		is placed in DF. This is equivalent to a Two's Complement
		subtraction of the accumulator from the memory byte.
		*/
		int sum = (~this.d) + (this.ram.getByte(this.r[this.x] & 0x00FFFF) & 0x00FF) + 1;
		this.d = sum & 0x00FF;
		this.df = (sum & 0xFF00) != 0 ? 1 : 0;
		
		this.cycles += 2;
	}

	private void execADC(byte opcode) {
		/*
		ADC     ADd with Carry                                     74
		ADCI b  ADd with Carry Immediate                        7C bb
		-------------------------------------------------------------
		Add the memory byte pointed to by the address register
		pointed to by X (or the second byte of the instruction) plus
		the value of the DF register to the accumulator. Put the
		result in the accumulator, and put the carry out of the sum
		back into DF.
		*/
		int sum = this.d + (this.ram.getByte(this.r[this.x] & 0x00FFFF) & 0x00FF);
		if (this.df != 0)
			sum += 1;
		
		this.d = sum & 0x00FF;
		this.df = (sum & 0xFF00) != 0 ? 1 : 0;
		
		this.cycles += 2;
	}

	private void execLSNF(byte opcode) {
		/*
		LSNF    Long Skip if DF is 0                               C7
		-------------------------------------------------------------
		Skip the next two bytes if DF=0.
		*/
		if (this.df == 0) {
			this.r[this.p] += 2;
			this.r[this.p] &= 0x00FFFF; 
		}
		
		this.cycles += 3;
	}

	private void execLSDF(byte opcode) {
		/*
		LSDF    Long Skip if DF is 1                               CF
		-------------------------------------------------------------
		Skip the next two bytes if DF=1.*/
		if (this.df != 0) {
			this.r[this.p] += 2;
			this.r[this.p] &= 0x00FFFF; 
		}
		
		this.cycles += 3;
	}

	private void execLBDF(byte opcode) {
		/*
		BDF a   Branch if DF is 1                               33 aa
		LBDF aa Long Branch if DF is 1                        C3 aaaa
		-------------------------------------------------------------
		Branch to the location whose address is in the second byte
		(or second and third bytes) of the instruction if DF=1.
		*/
		if (this.df != 0) {
			this.r[this.p] =
				this.ram.getUnsignedByte(this.r[this.p]) * 0x0100 +
				this.ram.getUnsignedByte(this.r[this.p] + 1);
		} else {
			this.r[this.p] += 2;
			this.r[this.p] &= 0x00FFFF;
		}
		
		this.cycles += 3;
	}

	private void execBDF(byte opcode) {
		/*
		BDF a   Branch if DF is 1                               33 aa
		LBDF aa Long Branch if DF is 1                        C3 aaaa
		-------------------------------------------------------------
		Branch to the location whose address is in the second byte
		(or second and third bytes) of the instruction if DF=1.
		*/
		if (this.df != 0) {
			this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]); 
		} else {
			this.r[this.p] ++;
			this.r[this.p] &= 0x00FFFF;
		}
		
		this.cycles += 2;
	}

	private void execANI(byte opcode) {
		/*
		ANI b   ANd Immediate                                   FA bb
		-------------------------------------------------------------
		All the bits in D corresponding to zeros in the second byte
		of the instruction are set to zeros.
		*/
		this.d &= (this.ram.getByte(this.r[this.p]) & 0x00FF);
		this.d &= 0x00FF;
		
		this.r[this.p] ++;
		this.r[this.p] &= 0xFFFF;
		
		this.cycles += 2;
	}

	private void execORI(byte opcode) {
		/*
		ORI b   OR Immediate                                    F9 bb
		-------------------------------------------------------------
		All the bits in D corresponding to ones in the second byte
		of the instruction are set to ones.
		*/
		this.d |= (this.ram.getByte(this.r[this.p]) & 0x00FF);
		this.d &= 0x00FF;
		
		this.r[this.p] ++;
		this.r[this.p] &= 0xFFFF;
		
		this.cycles += 2;
	}

	private void execXOR(byte opcode) {
		/*
		XOR     eXclusive OR                                       F3
		-------------------------------------------------------------
		The datum in D and the datum in the memory byte pointed to
		by the address register pointed to by X are combined in a
		bit-by-bit exclusive OR, and the result is left in D.
		*/
		this.d ^= this.ram.getUnsignedByte(this.r[this.x]);
		this.d &= 0x00FF;
		
		this.cycles += 2;
	}

	private void execAND(byte opcode) {
		/*
		AND     Logical AND                                        F2
		-------------------------------------------------------------
		The datum in D and the datum in the memory byte pointed to
		by the address register pointed to by X are combined in a
		bit-by-bit logical AND, and the result is left in D.
		*/
		this.d &= this.ram.getUnsignedByte(this.r[this.x]);
		this.d &= 0x00FF;
		
		this.cycles += 2;
	}

	private void execOR(byte opcode) {
		/*
		OR      Logical OR                                         F1
		-------------------------------------------------------------
		The datum in D and the datum in the memory byte pointed to
		by the address register pointed to by X are combined in a
		bit-by-bit inclusive OR, and the result is left in D.
		*/
		this.d |= this.ram.getUnsignedByte(this.r[this.x]);
		this.d &= 0x00FF;
		
		this.cycles += 2;
	}

	private void execLDXA(byte opcode) {
		/*
		LDXA    Load D via R(X) and Advance                        72
		-------------------------------------------------------------
		Load the accumulator from the memory byte pointed to by the
		address register pointed to by X, then increment that
		register.
		*/
		this.d = this.ram.getUnsignedByte(this.r[this.x]);
		
		this.r[this.x] ++;
		this.r[this.x] &= 0x00FFFF;
		
		this.cycles += 2;
	}

	private void execLDX(byte opcode) {
		/*
		LDX     Load D via R(X)                                    F0
		-------------------------------------------------------------
		Load the accumulator from the memory byte pointed to by the
		address register pointed to by X.
		*/
		this.d = this.ram.getUnsignedByte(this.r[this.x]);
		
		this.cycles += 2;
	}

	private void execLDNn(byte opcode) {
		/*
		LDN r   Load D via N (r = 1 to F)                          0r
		-------------------------------------------------------------
		Copy the memory byte pointed to by the specified address
		register r into the Accumulator.
		*/
		int rIndex = opcode & 0x0F;
		this.d = this.ram.getUnsignedByte(this.r[rIndex] & 0x00FFFF);
		
		this.cycles += 2;
	}

	private void execLDAn(byte opcode) {
		/*
		LDA r   Load D and Advance                                 4r
		-------------------------------------------------------------
		Copy the contents of the memory byte pointed to by the
		specified address register r into the Accumulator, and
		increment the register.
		*/
		int rIndex = opcode & 0x0F;
		this.d = this.ram.getUnsignedByte(this.r[rIndex] & 0x00FFFF);
		this.r[rIndex] ++;
		this.r[rIndex] &= 0x00FFFF;
		
		this.cycles += 2;
	}

	private void execSTXD(byte opcode) {
		/*
		STXD    STore D via R(X) and Decrement R(X)                73
		-------------------------------------------------------------
		Store the accumulator into the memory location pointed to by
		the address register pointed to by X, then decrement that
		address register.
		*/
		this.ram.setByte(this.r[this.x], (byte) this.d);
		this.r[this.x] --;
		this.r[this.x] &= 0x00FFFF;
		
		this.cycles += 2;
	}

	private void execSHL(byte opcode) {
		/*
		SHL     SHift D Left                                       FE
		SHLC    SHift D Left with Carry                            7E
		-------------------------------------------------------------
		Move each bit in the accumulator D one bit position to the
		left. The leftmost bit of D is moved into the DF register.
		The rightmost bit of the accumulator is set to zero (SHL) or
		to the previous contents of DF (SHLC).
		*/
		int v = this.d << 1;
		this.df = ((v & 0x0100) == 0) ? 0 : 1;
		this.d = v & 0x00FF;
		
		this.cycles += 2;
	}

	private void execSHLC(byte opcode) {
		/*
		SHL     SHift D Left                                       FE
		SHLC    SHift D Left with Carry                            7E
		-------------------------------------------------------------
		Move each bit in the accumulator D one bit position to the
		left. The leftmost bit of D is moved into the DF register.
		The rightmost bit of the accumulator is set to zero (SHL) or
		to the previous contents of DF (SHLC).
		*/
		int v = this.d << 1;
		if (this.df != 0)
			v |= 0x01;
		
		this.df = ((v & 0x0100) == 0) ? 0 : 1;
		this.d = v & 0x00FF;
		
		this.cycles += 2;
	}

	private void execADCI(byte opcode) {
		/*
		ADC     ADd with Carry                                     74
		ADCI b  ADd with Carry Immediate                        7C bb
		-------------------------------------------------------------
		Add the memory byte pointed to by the address register
		pointed to by X (or the second byte of the instruction) plus
		the value of the DF register to the accumulator. Put the
		result in the accumulator, and put the carry out of the sum
		back into DF.
		*/
		int sum = this.d + (this.ram.getByte(this.r[this.p] & 0x00FFFF) & 0x00FF);
		if (this.df != 0)
			sum += 1;
		
		this.d = sum & 0x00FF;
		this.df = (sum & 0xFF00) != 0 ? 1 : 0;
		
		this.r[this.p] ++;
		this.r[this.p] &= 0x00FFFF;
		
		this.cycles += 2;
	}

	private void execIRX(byte opcode) {
		/*
		IRX     Increment R(X)                                     60
		-------------------------------------------------------------
		Increment the register pointed to by X.
		*/
		this.r[this.x] ++;
		this.r[this.x] &= 0x00FFFF;
		
		this.cycles += 2;
	}

	private void execBNF(byte opcode) {
		/*
		BNF a   Branch if DF is 0                               3B aa
		LBNF aa Long Branch if DF is 0                        CB aaaa
		-------------------------------------------------------------
		Branch to the location whose address is in the second byte
		(or second and third bytes) of the instruction if DF=0.
		*/
		if (this.df == 0) {
			this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]); 
		} else {
			this.r[this.p] ++;
			this.r[this.p] &= 0x00FFFF;
		}
		
		this.cycles += 2;
	}

	private void execLBNF(byte opcode) {
		/*
		BNF a   Branch if DF is 0                               3B aa
		LBNF aa Long Branch if DF is 0                        CB aaaa
		-------------------------------------------------------------
		Branch to the location whose address is in the second byte
		(or second and third bytes) of the instruction if DF=0.
		*/
		if (this.df == 0) {
			this.r[this.p] =
				this.ram.getUnsignedByte(this.r[this.p]) * 0x0100 +
				this.ram.getUnsignedByte(this.r[this.p] + 1);
		} else {
			this.r[this.p] += 2;
			this.r[this.p] &= 0x00FFFF;
		}
		
		this.cycles += 3;
	}

	private void execRET(byte opcode) {
		/*
		RET     RETurn                                             70
		-------------------------------------------------------------
		Read the byte from the memory location pointed to by the
		register pointed to by X; then increment that register. Copy
		the right 4 bits of the byte read into P, and the left four
		bits into X. Then enable interrupts (set IE to 1).
		*/
		int data = this.ram.getUnsignedByte(this.r[this.x] & 0x00FFFF);
		this.r[this.x] ++;
		this.r[this.x] &= 0x00FFFF;
		
		this.p = data & 0x0F;
		this.x = ((data & 0x00F0) >> 4) & 0x0F;
		
		this.cycles += 2;
	}

	private void execADI(byte opcode) {
		/*
		ADI b   ADd Immediate                                   FC bb
		-------------------------------------------------------------
		Add the value of the second byte of the instruction to the
		accumulator, and put the sum back into the accumulator. Put
		the carry out bit in the DF register.
		*/
		int sum = this.d + (this.ram.getByte(this.r[this.p] & 0x00FFFF) & 0x00FF);
		this.d = sum & 0x00FF;
		this.df = (sum & 0xFF00) != 0 ? 1 : 0;
		
		this.r[this.p] ++;
		this.r[this.p] &= 0x00FFFF;
		
		this.cycles += 2;
	}

	private void execADD(byte opcode) {
		/*
		ADD     Add                                                F4
		-------------------------------------------------------------
		Add the contents of the memory byte pointed to by the
		register pointed to by X to the contents of D, and put the
		sum in D. Put the carry out of the sum into DF.
		*/
		int sum = this.d + (this.ram.getByte(this.r[this.x] & 0x00FFFF) & 0x00FF);
		this.d = sum & 0x00FF;
		this.df = (sum & 0xFF00) != 0 ? 1 : 0;
		
		this.cycles += 2;
	}

	private void execSEXn(byte opcode) {
		/*
		SEX r   Set X                                              Er
		-------------------------------------------------------------
		Set register X to point to the specified register r.
		*/
		this.x = (opcode & 0x0F);
		
		this.cycles += 2;
	}

	private void execSTRn(byte opcode) {
		/*
		STR r   SToRe D into memory                                5r
		-------------------------------------------------------------
		Using the specified address register, store (copy the
		contents of) the accumulator into memory.
		*/
		int rIndex = opcode & 0x0F;
		int address = this.r[rIndex] & 0x00FFFF;
		this.ram.setByte(address, (byte) (this.d & 0x00FF));
		
		this.cycles += 2;
	}

	private void execXRI(byte opcode) {
		/*
		XRI b   eXclusive oR Immediate                          FB bb
		-------------------------------------------------------------
		All the bits in D corresponding to ones in the second byte
		of the instruction are complemented.
		*/
		this.d ^= (this.ram.getByte(this.r[this.p]) & 0x00FF);
		this.d &= 0x00FF;
		
		this.r[this.p] ++;
		this.r[this.p] &= 0xFFFF;
		
		this.cycles += 2;
	}

	private void execPHIn(byte opcode) {
		/*
		PHI r   Put D into High byte of register                   Br
		-------------------------------------------------------------
		Copy D into the most significant eight bits of the specified
		register.
		*/
		int rIndex = opcode & 0x0F;
		this.r[rIndex] &= 0x00FF;
		this.r[rIndex] |= ((this.d & 0x00FF) << 8);
		
		this.cycles += 2;
	}

	private void execPLOn(byte opcode) {
		/*
		PLO r   Put D into Low byte of register                    Ar
		-------------------------------------------------------------
		Copy D into the least significant eight bits of the specified
		register.
		*/
		int rIndex = opcode & 0x0F;
		this.r[rIndex] &= 0xFF00;
		this.r[rIndex] |= (this.d & 0x00FF);
		
		this.cycles += 2;
	}

	private void execGHIn(byte opcode) {
		/*
		GHI r   Get HIgh byte of register                          9r
		-------------------------------------------------------------
		Copy the most significant eight bits of the specified
		register into D.
		*/
		int rIndex = opcode & 0x0F;
		this.d = (this.r[rIndex] >> 8) & 0x00FF;
		
		this.cycles += 2;
	}

	private void execGLOn(byte opcode) {
		/*
		GLO r   Get LOw byte of register                           8r
		-------------------------------------------------------------
		Copy the least significant eight bits of the specified
		register into D.
		*/
		int rIndex = opcode & 0x0F;
		this.d = this.r[rIndex] & 0x00FF;
		
		this.cycles += 2;
	}

	private void execDECn(byte opcode) {
		/*
		DEC r   DECrement register                                 2r
		-------------------------------------------------------------
		Decrement (subtract one from) the specified register.
		*/
		int rIndex = opcode & 0x0F;
		this.r[rIndex] --;
		this.r[rIndex] &= 0xFFFF;
		
		this.cycles += 2;
	}

	private void execINCn(byte opcode) {
		/*
		INC r   Increment register                                 1r
		-------------------------------------------------------------
		Increment (add one to) the address register specified in the
		right digit of the instruction.
		*/
		int rIndex = opcode & 0x0F;
		this.r[rIndex] ++;
		this.r[rIndex] &= 0xFFFF;
		
		this.cycles += 2;
	}

	private void execLSNQ(byte opcode) {
		/*
		LSNQ    Long Skip if Q is off                              C5
		-------------------------------------------------------------
		If Q is off, skip two bytes; otherwise execute the next byte
		after this opcode.
		*/
		if (!this.outputDevice.isQOn()) {
			this.r[this.p] += 2;
			this.r[this.p] &= 0xFFFF; 
		}
		
		this.cycles += 3;
	}

	private void execLSQ(byte opcode) {
		/*
		LSQ     Long Skip if Q is on                               CD
		-------------------------------------------------------------
		If Q is on, skip two bytes; otherwise execute the next byte
		after this opcode.
		*/
		if (this.outputDevice.isQOn()) {
			this.r[this.p] += 2;
			this.r[this.p] &= 0xFFFF;
		}
		
		this.cycles += 3;
	}

	private void execBNQ(byte opcode) {
		/*
		BNQ a   Branch if Q is off                              39 aa
		-------------------------------------------------------------
		Branch to the location whose one-byte address follows this
		opcode if Q is off; otherwise ignore the address and take the
		next instruction in sequence.
		*/
		if (!this.outputDevice.isQOn()) {
			this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]);
		} else {
			this.r[this.p] ++;
			this.r[this.p] &= 0xFFFF;
		}
		
		this.cycles += 2;
	}

	private void execBQ(byte opcode) {
		/*
		BQ a    Branch If Q is on                               31 aa
		-------------------------------------------------------------
		Branch to the location whose one-byte address follows this
		opcode if Q is on; otherwise ignore the address and take the
		next instruction in sequence.
		*/
		if (this.outputDevice.isQOn()) {
			this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]);
		} else {
			this.r[this.p] ++;
			this.r[this.p] &= 0xFFFF;
		}
		
		this.cycles += 2;
	}

	private void execLBNQ(byte opcode) {
		/*
		LBNQ aa Long Branch if Q is off                       C9 aaaa
		-------------------------------------------------------------
		Branch to the location whose two byte address follows this
		opcode if Q is off; otherwise ignore the address and take the
		next instruction in sequence otherwise.
		*/
		if (!this.outputDevice.isQOn()) {
			this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]) * 0x100 + this.ram.getUnsignedByte(this.r[this.p] + 1);
		} else {
			this.r[this.p] += 2;
			this.r[this.p] &= 0xFFFF;
		}
		
		this.cycles += 3;
	}

	private void execLBQ(byte opcode) {
		/*
		LBQ aa  Long Branch if Q is on                        C1 aaaa
		-------------------------------------------------------------
		Branch to the location whose two byte address follows this
		opcode if Q is on; otherwise ignore the address and take the
		next instruction in sequence.
		*/
		if (this.outputDevice.isQOn()) {
			this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]) * 0x100 + this.ram.getUnsignedByte(this.r[this.p] + 1);
		} else {
			this.r[this.p] += 2;
			this.r[this.p] &= 0xFFFF;
		}
		
		this.cycles += 3;
	}

	private void execLSNZ(byte opcode) {
		/*
		LSNZ    Long Skip if Not Zero                              C6
		-------------------------------------------------------------
		Skip two bytes if D is not zero; otherwise execute the next
		byte in sequence.
		*/
		if (this.d != 0) {
			this.r[this.p] += 2;
			this.r[this.p] &= 0xFFFF;
		}
		
		this.cycles += 3;
	}

	private void execLSZ(byte opcode) {
		/*
		LSZ     Long Skip if Zero                                  CE
		-------------------------------------------------------------
		Skip two bytes if D is zero; otherwise execute the next byte
		in sequence after the LSZ opcode.
		*/
		if (this.d == 0) {
			this.r[this.p] += 2;
			this.r[this.p] &= 0xFFFF;
		}
		
		this.cycles += 3;
	}

	private void execLBNZ(byte opcode) {
		/*
		LBNZ aa Long Branch if Not Zero                       CA aaaa
		-------------------------------------------------------------
		If the accumulator D is not exactly zero, replace the
		contents of the Program Counter with the second and third
		bytes of this instruction; otherwise proceed to the next
		instruction in sequence.
		*/
		if (this.d != 0) {
			this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]) * 0x100 + this.ram.getUnsignedByte(this.r[this.p] + 1);
		} else {
			this.r[this.p] += 2;
			this.r[this.p] &= 0xFFFF;
		}
		
		this.cycles += 3;
	}

	private void execLBZ(byte opcode) {
		/*
		LBZ aa  Long Branch if Zero                           C2 aaaa
		-------------------------------------------------------------
		If the accumulator D is exactly zero, replace the contents of
		the Program Counter with the second and third bytes of this
		instruction; otherwise proceed to the next instruction in
		sequence.
		*/
		if (this.d == 0) {
			this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]) * 0x100 + this.ram.getUnsignedByte(this.r[this.p] + 1);
		} else {
			this.r[this.p] += 2;
			this.r[this.p] &= 0xFFFF;
		}
		
		this.cycles += 3;
	}

	private void execLDI(byte opcode) {
		/*
		LDI b   Load D Immediate                                F8 bb
		-------------------------------------------------------------
		Copy the second byte of the instruction into the D register.
		*/
		this.d = this.ram.getByte(this.r[this.p]) & 0xFF;
		
		this.r[this.p] ++;
		this.r[this.p] &= 0xFFFF;
		
		this.cycles += 2;
	}

	private void execBNZ(byte opcode) {
		/*
		BNZ a   Branch on Not Zero                              3A aa
		-------------------------------------------------------------
		Branch if D is not exactly 00; otherwise take the next
		instruction in sequence (after the branch address).
		*/
		if (this.d != 0) {
			this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]); 
		} else {
			this.r[this.p] ++;
			this.r[this.p] &= 0xFFFF;
		}
		
		this.cycles += 2;
	}

	private void execBZ(byte opcode) {
		/*
		BZ a    Branch on Zero                                  32 aa
		-------------------------------------------------------------
		Branch if D is zero; otherwise execute the next instruction
		in sequence (after the branch address).
		*/
		if (this.d == 0) {
			this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]); 
		} else {
			this.r[this.p] ++;
			this.r[this.p] &= 0xFFFF;
		}
		
		this.cycles += 2;
	}

	private void execNOP(byte opcode) {
		/*
		NOP     No Operation                                       C4
		-------------------------------------------------------------
		This instruction does nothing, except take three major cycles
		to execute.
		*/
		this.cycles += 3;
	}

	private void execLSKP(byte opcode) {
		/*
		LSKP    Long Skip                                          C8
		-------------------------------------------------------------
		Skip the two bytes following the opcode.
		*/
		this.r[this.p] += 2;
		this.r[this.p] &= 0xFFFF;
		
		this.cycles += 3;
	}

	private void execSKP(byte opcode) {
		/*
		SKP     Skip one byte                                      38
		-------------------------------------------------------------
		The one-byte instruction following the SKP opcode is skipped.
		*/

		this.r[this.p] ++;
		this.r[this.p] &= 0xFFFF;
		
		this.cycles += 2;
	}

	private void execINPn(byte opcode) {
		/*
		INP p   Input to memory and D (p = 9 to F)                 6p
		-------------------------------------------------------------
		Input from the port p, a byte to be stored into the memory
		location pointed to by the address register pointed to by X,
		and also place the byte into D.
		*/
		int portIndex = (((int) opcode) & 0xFF) - 0x69;
		byte portValue = this.inputDevice.getPort(portIndex);
		this.ram.setByte(this.r[this.x], portValue);
		this.d = ((int) portValue) & 0x00FF;
		
		this.cycles += 2;
	}
	
	private void execOUTn(byte opcode) {
		/*
		OUT p   Output from memory (p = 1 to 7)                    6p
		-------------------------------------------------------------
		Output on the port p, the memory byte pointed to by the
		address register pointed to by X, then increment the address
		register.
		*/
		int portIndex = (((int) opcode) & 0xFF) - 0x61;
		byte portValue = this.ram.getByte(this.r[this.x]);
		this.outputDevice.setPort(portIndex, portValue);
		
		this.r[this.x] ++;
		this.r[this.x] &= 0xFFFF;
		
		this.cycles += 2;
	}

	private void execLBR(byte opcode) {
		/*
		LBR aa  Long Branch unconditionally                   C0 aaaa
		-------------------------------------------------------------
		Take as the next instruction, the one whose address is the
		second and third bytes of the LBR instruction.
		*/
		this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]) * 0x100 + this.ram.getUnsignedByte(this.r[this.p] + 1);
		
		this.cycles += 3;
	}

	private void execBR(byte opcode) {
		/*
		BR a    Branch unconditionally                          30 aa
		-------------------------------------------------------------
		Take as the next instruction, the one whose address is the
		second byte of the BR instruction.
		*/
		this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]);
		
		this.cycles += 2;
	}

	private void execBNn(byte opcode) {
		/*
		BN1 a   Branch on Not External Flag 1                   3C aa
		BN2 a   Branch on Not External Flag 2                   3D aa
		BN3 a   Branch on Not External Flag 3                   3E aa
		BN4 a   Branch on Not External Flag 4                   3F aa
		-------------------------------------------------------------
		If the corresponding external flag line is False (i.e. the
		pin is high electrically), then take as the next instruction
		the one found at the address which is in the second byte of
		the instruction. If the flag is True, advance to the next
		instruction in sequence and ignore the address in the second
		byte of this instruction.
		*/
		int efIndex = opcode & 0x03;
		if (!this.ef[efIndex]) {
			this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]);
		} else {
			this.r[this.p] ++;
			this.r[this.p] &= 0xFFFF;
		}
		
		this.cycles += 2;
	}

	private void execBn(byte opcode) {
		/*
		B1 a    Branch on External Flag 1                       34 aa
		B2 a    Branch on External Flag 2                       35 aa
		B3 a    Branch on External Flag 3                       36 aa
		B4 a    Branch on External Flag 4                       37 aa
		-------------------------------------------------------------
		If the corresponding external flag line is True (i.e. the
		pin is electrically low), then take as the next instruction
		the one found at the address which is in the second byte of
		the instruction. If the flag is False, advance to the next
		instruction in sequence and ignore the address in the second
		byte of this instruction.
		*/
		int efIndex = opcode & 0x03;
		if (this.ef[efIndex]) {
			this.r[this.p] = this.ram.getUnsignedByte(this.r[this.p]);
		} else {
			this.r[this.p] ++;
			this.r[this.p] &= 0xFFFF;
		}
		
		this.cycles += 2;
	}

	private void execSEQ(byte opcode) {
		/*
		SEQ	Set Q						   7B
		-------------------------------------------------------------
		Set the Q flip-flop in the 1802 on, and set the Q output pin
		high.
		*/
		outputDevice.setQ(true);
		
		this.cycles += 2;
	}

	private void execREQ(byte opcode) {
		/*
		REQ	Reset Q						   7A
		-------------------------------------------------------------
		Turn the Q flip-flop in the 1802 off, and set the Q output
		pin low.
		*/
		outputDevice.setQ(false);
		
		this.cycles += 2;
	}

	private void execIDL(byte opcode) {
		/*
		IDL	Idle						   00
		-------------------------------------------------------------
		Stop the execution of instructions, and wait for an interrupt
		or DMA to resume.
		*/
		
		this.idle = true;
		
		this.cycles += 2;
	}
}
