package com.akeysoft.elf.ui;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

import com.akeysoft.elf.core.Cpu;
import com.akeysoft.elf.core.InputDevice;
import com.akeysoft.elf.core.OutputDevice;
import com.akeysoft.elf.core.OutputListener;
import com.akeysoft.elf.core.Ram;

public class Elf2 extends JFrame implements ActionListener, OutputListener {

	private static final long serialVersionUID = 6278492777863190203L;

	private JMenu jMenuFile, jMenuHelp;
	private JMenuItem jMenuItemExit, jMenuItemOpen, jMenuItemSave, jMenuItemAbout;
	
	private JButton[] numberButtons;
	private JToggleButton runButton;
	private JToggleButton loadButton;
	private JToggleButton mpButton;
	private JButton iButton;
	
	private JLabel hexHi;
	private JLabel hexLo;
	private JLabel q;
	
	private ImageIcon[] numberIcons;
	private ImageIcon qon;
	private ImageIcon qoff;

	private Ram ram;

	private Cpu cpu;
	
	private boolean isLoad, isMp, isRun;
	
	private int inputOffset;
	private byte inputValue;
	
	private File currentFile;
	
	public Elf2() {
		this.isLoad = false;
		this.isMp = false;
		this.isRun = false;
		
		this.inputOffset = 0;
		this.inputValue = 0;
		
		// Core
		this.initCore();
		
		// Frame
		this.setTitle("ELF II Emulator");
		this.setLayout(null);
		
		// Icons
		numberIcons = new ImageIcon[16];
		for (int i = 0; i < numberIcons.length; i ++)
			numberIcons[i] = createImageIcon(i + ".png");
		
		qon = createImageIcon("qon.png");
		qoff = createImageIcon("qoff.png");
		
		int hexLeft = 28;
		int hexTop = 8;
		
		hexHi = new JLabel();
		hexHi.setBounds(hexLeft, hexTop, 90, 122);
		this.add(hexHi);
		
		hexLo = new JLabel();
		hexLo.setBounds(hexLeft + 90, hexTop, 90, 122);
		this.add(hexLo);
		
		this.setHex(0x00);
		
		q = new JLabel();
		q.setBounds(hexLeft + 180, hexTop, 60, 122);
		this.add(q);
		
		this.setQ(false);
		
		// File menu
		jMenuFile = new JMenu("File");
		jMenuFile.setMnemonic(KeyEvent.VK_F);
		
		jMenuItemOpen = new JMenuItem("Open");
		jMenuItemOpen.setMnemonic(KeyEvent.VK_O);
		jMenuItemOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				ActionEvent.CTRL_MASK));
		
		jMenuItemOpen.addActionListener(this);
		
		jMenuFile.add(jMenuItemOpen);
		
		jMenuItemSave = new JMenuItem("Save");
		jMenuItemSave.setMnemonic(KeyEvent.VK_S);
		jMenuItemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				ActionEvent.CTRL_MASK));
		
		jMenuItemSave.addActionListener(this);
		
		jMenuFile.add(jMenuItemSave);
		
		jMenuItemExit = new JMenuItem("Exit");
		jMenuItemExit.setMnemonic(KeyEvent.VK_X);
		jMenuItemExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
				ActionEvent.CTRL_MASK));
		
		jMenuItemExit.addActionListener(this);
		
		jMenuFile.add(jMenuItemExit);
		
		// Help menu
		jMenuHelp = new JMenu("Help");
		jMenuHelp.setMnemonic(KeyEvent.VK_H);
		
		jMenuItemAbout = new JMenuItem("About");
		jMenuItemAbout.setMnemonic(KeyEvent.VK_A);
		jMenuItemAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1,
				ActionEvent.CTRL_MASK));
		
		jMenuItemAbout.addActionListener(this);
		
		jMenuHelp.add(jMenuItemAbout);
		
		// Build menu bar
		JMenuBar mb = new JMenuBar();
		mb.add(jMenuFile);
		mb.add(jMenuHelp);
		
		this.setJMenuBar(mb);
		
		// 0-F buttons
		this.numberButtons = new JButton[16];
		
		for (int i = 0; i < numberButtons.length; i ++) {
			int row = 3 - i / 4;
			int col = i % 4;
			
			String label = Integer.toHexString(i).toUpperCase();
			
			this.numberButtons[i] = this.createButton(label);
			
			this.setButtonBounds(this.numberButtons[i], row, col);
		}
		
		// Run, Load, M/P buttons
		this.runButton = this.createToggleButton("Run");
		this.setButtonBounds(this.runButton, 0, 4);
		
		this.loadButton = this.createToggleButton("Load");
		this.setButtonBounds(this.loadButton, 1, 4);
		
		this.mpButton = this.createToggleButton("M/P");
		this.setButtonBounds(this.mpButton, 2, 4);
		
		// I button
		this.iButton = this.createButton("I");
		this.setButtonBounds(this.iButton, 3, 4);
		
		// Other properties
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setSize(300, 400);
		this.setResizable(false);
		this.setLocationByPlatform(true);
		
		// Events
		this.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				if (!cpu.isStop())
					cpu.stop();
			}
		});
	}
	
	private void setHex(int value) {
		int hi = (value >> 4) & 0x0F;
		int lo = value & 0x0F;
		
		hexHi.setIcon(this.numberIcons[hi]);
		hexLo.setIcon(this.numberIcons[lo]);
	}
	
	private void setQLed(boolean isOn) {
		if (isOn)
			q.setIcon(this.qon);
		else
			q.setIcon(this.qoff);
	}
	
	private ImageIcon createImageIcon(String path) {
		URL imgUrl = getClass().getResource(path);
		if (imgUrl != null) {
			return new ImageIcon(imgUrl);
		} else {
			return null;
		}
	}
	private JToggleButton createToggleButton(String label) {
		JToggleButton btn = new JToggleButton(label);
		btn.setMargin(new Insets(0, 0, 0, 0));
		btn.addActionListener(this);
		
		this.add(btn);
		
		return btn;
	}

	private JButton createButton(String label) {
		JButton btn = new JButton(label);
		btn.setMargin(new Insets(0, 0, 0, 0));
		btn.addActionListener(this);
		
		this.add(btn);
		
		return btn;
	}
	
	private void setButtonBounds(AbstractButton btn, int row, int col) {
		int buttonsTop = 136;
		int buttonsLeft = 16;
		int buttonWidth = 48;
		int buttonsPadding = 4;
		
		int left = buttonsLeft + col * (buttonWidth + buttonsPadding);
		int top = buttonsTop + row * (buttonWidth + buttonsPadding);
		if (col == 4) {
			left = buttonsLeft + 4 * (buttonWidth + buttonsPadding) + buttonsPadding;
		}
		
		btn.setBounds(left, top, buttonWidth, buttonWidth);
	}
	
	private void initCore() {
		InputDevice inputDevice = new InputDevice();
		OutputDevice outputDevice = new OutputDevice();
		outputDevice.setOutputListener(this);
		
		ram = new Ram(65536);
		cpu = new Cpu(ram, inputDevice, outputDevice);
		
		cpu.reset();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Elf2 elf2 = new Elf2();
		elf2.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == jMenuItemExit) {
			this.dispose();
		} else if (e.getSource() == jMenuItemAbout) {
			JOptionPane.showMessageDialog(this,
					"ELF II Emulator\n\nVersion 0.01\n\nCopyright(C) Akeysoft\n\nhttp://www.akeysoft.com\n",
					"About",
					JOptionPane.INFORMATION_MESSAGE);
		} else if (e.getSource() == runButton) {
			isRun = runButton.isSelected();
			isLoad = false;
			
			updateButtons();
			
			if (isRun && cpu.isStop()) {
				cpu.reset();
				(new Thread(new CpuRunner(cpu, this))).start();
			} else if ((!isRun) && (!cpu.isStop())) {
				cpu.stop();
			}
		} else if (e.getSource() == mpButton) {
			isMp = mpButton.isSelected();
			
			updateButtons();
			
			ram.setMp(isMp);
		} else if (e.getSource() == loadButton) {
			isLoad = loadButton.isSelected();
			isRun = false;
			
			updateButtons();
			
			if (!cpu.isStop())
				cpu.stop();
			
			if (isLoad && mpButton.isSelected())
				cpu.reset();
			
			if (isLoad)
				this.inputOffset = 0;
		} else if (e.getSource() == iButton) {
			if (isLoad) {
				ram.setByte(this.inputOffset, this.inputValue);
				byte currentByte = ram.getByte(this.inputOffset);
				this.inputOffset ++;
				this.inputOffset &= 0x00FFFF;
				
				this.setHex(currentByte);
			}
		} else if (e.getSource() == jMenuItemOpen) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setMultiSelectionEnabled(false);
			chooser.setSelectedFile(this.currentFile);
			
			int returnVal = chooser.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				this.loadFileToRam(chooser.getSelectedFile());
			}
		} else if (e.getSource() == jMenuItemSave) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setMultiSelectionEnabled(false);
			chooser.setSelectedFile(this.currentFile);
			
			int returnVal = chooser.showSaveDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				this.saveFileFromRam(chooser.getSelectedFile());
			}
		} else {
			for (int i = 0; i < this.numberButtons.length; i ++) {
				JButton btn = this.numberButtons[i];
				
				if (e.getSource() == btn) {
					this.inputValue = (byte) (this.inputValue << 4);
					this.inputValue |= i;
				}
			}
		}
	}

	private void saveFileFromRam(File file) {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			
			try {
				// find the last non-zere position
				int lastNonZeroPos = 65535;
				while (lastNonZeroPos >= 0) {
					if (ram.getByte(lastNonZeroPos) != 0)
						break;
					
					lastNonZeroPos --;
				}
				
				if (lastNonZeroPos < 65535)
					lastNonZeroPos ++;
				
				for (int i = 0; i < lastNonZeroPos + 1; i ++) {
					fos.write(ram.getByte(i));
				}
			} finally {
				fos.close();
			}
			
			this.setTitle("ELF II Emulator [" + file.getName() + "]");
			this.currentFile = file;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					"Could not save file!",
					"Save error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void loadFileToRam(File file) {
		try {
			FileInputStream fis = new FileInputStream(file);
			try {
				byte[] buf = new byte[65536];
				fis.read(buf);
				ram.setBytes(0, buf);
			} finally {
				fis.close();
			}
			
			this.setTitle("ELF II Emulator [" + file.getName() + "]");
			this.currentFile = file;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					"Could not open file!",
					"Open error",
					JOptionPane.ERROR_MESSAGE);
		}
		
	}

	private void updateButtons() {
		this.runButton.setSelected(this.isRun);
		this.loadButton.setSelected(this.isLoad);
		this.mpButton.setSelected(this.isMp);
	}

	public void setPort(int index, byte value) {
		if (index == 3) {
			this.setHex(value);
		}
	}

	public void setQ(boolean isOn) {
		this.setQLed(isOn);
	}

	public void stopped() {
		this.runButton.setSelected(false);
	}

}
