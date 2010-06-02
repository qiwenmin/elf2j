package com.akeysoft.elf.core;

public class InputDevice {

	private byte[] ports;
	public InputDevice() {
		this.ports = new byte[7];
	}
	
	public void setPort(int index, byte value) {
		this.ports[index] = value;
	}
	
	public byte getPort(int index) {
		return this.ports[index];
	}	
}
