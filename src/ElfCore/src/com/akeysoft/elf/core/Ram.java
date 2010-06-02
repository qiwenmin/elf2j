package com.akeysoft.elf.core;

public class Ram {

	private byte[] memory;
	
	private boolean mp;
	
	public Ram(int size) {
		mp = false;
		
		this.memory = new byte[size];
	}
	
	public void setMp(boolean mp) {
		this.mp = mp;
	}
	
	public boolean isMp() {
		return this.mp;
	}
	
	public void setByte(int address, byte value) {
		if (this.mp) return;
		
		this.memory[address] = value;
	}
	
	public void setBytes(int beginAddress, byte[] values) {
		if (this.mp) return;
		
		int len = memory.length - beginAddress > values.length ? values.length : memory.length - beginAddress;
		
		for (int i = 0; i < len; i ++) {
			this.memory[beginAddress + i] = values[i];
		}
	}

	public byte getByte(int address) {
		return this.memory[address];
	}
	
	public int getUnsignedByte(int address) {
		return ((int) this.memory[address]) & 0x00FF;
	}
}
