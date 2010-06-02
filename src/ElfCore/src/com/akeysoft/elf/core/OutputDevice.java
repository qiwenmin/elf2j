package com.akeysoft.elf.core;

public class OutputDevice {

	private boolean qOn;
	private byte[] ports;
	private OutputListener listener;
	
	public OutputDevice() {
		this.qOn = false;
		this.ports = new byte[7];
		this.listener = null;
	}
	
	public void setOutputListener(OutputListener listener) {
		this.listener = listener;
	}
	
	public void setQ(boolean isOn) {
		if (this.listener != null && this.qOn != isOn) {
			this.listener.setQ(isOn);
		}
		
		this.qOn = isOn;
	}
	
	public boolean isQOn() {
		return this.qOn;
	}
	
	public void setPort(int index, byte value) {
		if (this.listener != null && this.ports[index] != value) {
			this.listener.setPort(index, value);
		}
		
		this.ports[index] = value;
	}
	
	public byte getPort(int index) {
		return this.ports[index];
	}
}
