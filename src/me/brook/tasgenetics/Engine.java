package me.brook.tasgenetics;

import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import me.brook.tasgenetics.Display.RenderingMode;
import me.brook.tasgenetics.tools.GifSequenceWriter;
import me.brook.tasgenetics.tools.Vector2;
import rta.gambatte.LoadFlags;

public class Engine implements Runnable {

	public static final int DEFAULT_FPS = 500;

	private boolean running;
	private Thread heartbeat;

	private World world;
	private Display display;

	private int fps = DEFAULT_FPS;
	private RenderingMode renderingMode = RenderingMode.GRAPH;
	private double zoomValue = 3;
	private boolean toggleFocus = false;
	private Vector2 cursor;

	public Engine() throws Exception {
		LoadFlags.FPS = 60 * 400000000;
		heartbeat = new Thread(this, "heartbeat-1");
		world = new World(this);
		// world = new World(this,
		// "E:\\Programming\\Natural Selection\\storage\\2021_35_17__03_35_48__generation85\\0.nnet",
		// false);
		display = new Display(this);
		cursor = new Vector2(display.getFrame().getWidth() / 2, display.getFrame().getHeight() / 2);
	}

	protected void updateZoomOffsets(double toZoom) {
		display.center = display.center.multiply(getZoom(zoomValue + toZoom) / getZoom());

		this.zoomValue += toZoom;
	}

	public void start() {
		heartbeat.start();
		running = true;
	}

	public void requestStop() {
		running = false;
	}

	private List<Digit> fps_history;

	private long fps_sum;

	public int getAverageFps() {
		return (int) (1e9f / (fps_sum / Math.max(1, fps_history.size())));
	}

	public class Digit {
		public int value;

		public Digit(int value) {
			this.value = value;
		}
	}

	long delay;

	@Override
	public void run() {
		delay = (long) (1e9 / Math.max(fps, 1));
		fps_history = new ArrayList<>();

		long last = 0;

		while(running) {
			long now = System.nanoTime();
			if(last + delay <= now) {
				try {
					world.update();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				display.update(world, renderingMode);

				int delta = (int) (now - last);

				if(fps_history.size() > 100) {
					fps_sum -= fps_history.remove(0).value;
				}

				fps_sum += delta;
				fps_history.add(new Digit(delta));

				last = now;
			}
		}

		stop();

	}

	public Display getDisplay() {
		return display;
	}

	public static void main(String[] args) throws Exception {
		Engine engine;
		try {
			engine = new Engine();
			engine.start();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public int getFps() {
		return fps;
	}

	public void setFps(int fps) {
		this.fps = fps;
	}

	public World getWorld() {
		return world;
	}

	public float getZoom() {
		return getZoom(this.zoomValue);
	}

	public float getZoom(double zoomValue) {
		return (float) Math.pow(2, zoomValue);
	}

	public void setZoomValue(float zoom) {
		this.zoomValue = zoom;
	}

	public Vector2 getCursor() {
		return cursor;
	}

	private Point mouse;

	public Point getMouseLocation() {
		return mouse;
	}

	public void stop() {
		System.out.println("Ran for " + world.getElapsedTime() / 1000.0 + " seconds");
		saveGif();
		world.save();

		System.exit(1);
	}
	
	public boolean isRunning() {
		return running;
	}

	public void saveGif() {
		try {
			List<BufferedImage> images = new ArrayList<>(display.getLocationList());

			File parent = new File("gifs");
			if(!parent.exists()) {
				parent.mkdirs();
			}

			String name = "save-" + parent.listFiles().length;
			File file = new File(parent, String.format("%s.gif", name));

			ImageOutputStream output = new FileImageOutputStream(file);

			GifSequenceWriter writer = new GifSequenceWriter(output, BufferedImage.TYPE_INT_RGB, 1000 / 8, true);

			BufferedImage scaled = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
			for(BufferedImage image : images) {
				scaled.getGraphics().drawImage(image.getScaledInstance(512, 512, Image.SCALE_FAST), 0, 0, null);
				
				writer.writeToSequence(scaled);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

}
