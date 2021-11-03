package me.brook.tasgenetics;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import rta.gambatte.Gb;
import rta.gambatte.LoadFlags;

public class EmulateTask implements Callable<Boolean>, Runnable {

	private ConcurrentLinkedQueue<Agent> entities;

	private long initTime = System.nanoTime();
	private boolean finished = false;
	
	private Gb gb;

	public EmulateTask(ConcurrentLinkedQueue<Agent> entities) {
		this.entities = entities;
		loadEmulator();

		// int start = (int) ((100 / (float) segments) * this.threadIndex);
		// int end = (int) ((100 / (float) segments) * (this.threadIndex+1));
		//
		// System.out.println(start + " -> " + end);

	}

	private void loadEmulator() {

		try {

			if(!new File("roms").exists()) {
				new File("roms").mkdir();
				System.err.println("I need ROMs to simulate!");
				System.exit(0);
			}

			// Init gambatte with 1 session
			// TODO: Parallelism?
			this.gb = new Gb();
			gb.loadBios("roms/gbc_bios.bin");
			gb.loadRom("roms/poke" + "red" + ".gb", LoadFlags.DEFAULT_LOAD_FLAGS);

//			gb.createDisplay(4);
		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}
	
	@Override
	public void run() {
		call();
	}

	@Override
	public Boolean call() {
		Agent e = entities.poll();
		while(e != null) {
			e.emulate(gb);
			
			e = entities.poll();
			System.out.print(".");
		}
		
		finished = true;
		
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (initTime ^ (initTime >>> 32));
		result = prime * result + ((entities == null) ? 0 : entities.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		EmulateTask other = (EmulateTask) obj;
		if(initTime != other.initTime)
			return false;
		if(entities == null) {
			if(other.entities != null)
				return false;
		}
		else if(!entities.equals(other.entities))
			return false;
		return true;
	}

	public boolean isDone() {
		return finished;
	}
	
	public Gb getGb() {
		return gb;
	}
	
	public void setEntities(ConcurrentLinkedQueue<Agent> entities) {
		this.entities = entities;
	}

}
