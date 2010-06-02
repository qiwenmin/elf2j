package com.akeysoft.elf.ui;

import javax.swing.JOptionPane;

import com.akeysoft.elf.core.Cpu;
import com.akeysoft.elf.core.UnknownOpcodeException;

public class CpuRunner implements Runnable {

	private Cpu cpu;
	private Elf2 elf2;
	
	public CpuRunner(Cpu cpu, Elf2 elf2) {
		this.cpu = cpu;
		this.elf2 = elf2;
	}
	
	public void run() {
		try {
			this.cpu.run();
		} catch (UnknownOpcodeException e) {
			JOptionPane.showMessageDialog(elf2,
					"Unknown opcode: " + e.getMessage(),
					"Exception",
					JOptionPane.ERROR_MESSAGE);
			cpu.stop();
		}
		
		if (cpu.isStop())
			elf2.stopped();
	}
}
