package me.brook.tasgenetics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import me.brook.neat.species.Species;
import me.brook.tasgenetics.tools.GifSequenceWriter;
import me.brook.tasgenetics.tools.Vector2;

public class Display {

	private DecimalFormat df = new DecimalFormat("#.##E0");

	private static final int GRID_SQUARES = 6;

	private Engine engine;

	private JFrame frame;
	private BufferedImage buffer;

	public Vector2 center;
	public Vector2 cursor;

	private double currentZoom = 1;

	private GifSequenceWriter gifWriter;

	private List<BufferedImage> locationList;

	public Display(Engine engine) {
		this.engine = engine;

		locationList = new ArrayList<>();

		boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString()
				.indexOf("-agentlib:jdwp") > 0;

		frame = new JFrame(isDebug ? "Stats Debug" : "Stats");
		frame.setSize(768, 768);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		buffer = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);

		center = new Vector2(0, 0);

		frame.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				Insets i = frame.getInsets();
				buffer = new BufferedImage(frame.getWidth() - i.left - i.right, frame.getHeight() - i.top - i.bottom,
						BufferedImage.TYPE_INT_RGB);
			}
		});

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				engine.stop();
			}
		});
	}

	public static enum RenderingMode {
		GRAPH;
	}

	public void update(World world, RenderingMode mode) {
		currentZoom = engine.getZoom();

		drawLocationChart(world);

		Insets insets = frame.getInsets();
		frame.getGraphics().drawImage(buffer, 0 + insets.left, 0 + insets.top,
				frame.getWidth(),
				frame.getHeight(), null);
	}

	private void drawLocationChart(World world) {
		Graphics2D g2 = (Graphics2D) buffer.getGraphics();

		g2.setColor(Color.GRAY);
		g2.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());

		if(world.getFinalLocations().isEmpty())
			return;

		Rectangle r = new Rectangle(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
		int highX = -1, highY = -1;
		for(Vector2 loc : world.getFinalLocations()) {

			if(r.x > loc.x)
				r.x = (int) loc.x;

			if(r.y > loc.y)
				r.y = (int) loc.y;

			if(highX < loc.x) {
				highX = (int) (loc.x);
			}

			if(highY < loc.y) {
				highY = (int) (loc.y);
			}

		}
		r.width = highX - r.x + 1;
		r.height = highY - r.y + 1;

		if(r.width > 0 && r.height > 0) {

			int size = Math.max(r.width, r.height);
			BufferedImage map = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
			Graphics2D g3 = (Graphics2D) map.getGraphics();

			g3.setColor(Color.WHITE);
			g3.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());

			List<Vector2> locations = getLocations(world);

			int index = 0;
			int locSize = locations.size();

			for(Vector2 loc : locations) {
				int rgb;
				rgb = interpolateColor(Color.RED, Color.BLUE, 1 - (((float) index) / locations.size())).getRGB();

				if(locSize - index < world.getNoveltyCreated()) {
					rgb = Color.MAGENTA.getRGB();
				}

				renderLocation(g3, loc, r, map, rgb, index == 0);
				index++;
			}

			renderLocation(g3, locations.get(0), r, map, Color.GREEN.getRGB(), true);

			locationList.add(map);

			// flipped in y dimension
			g2.drawImage(map, buffer.getWidth() / 4, buffer.getHeight() / 4, buffer.getWidth() / 2,
					buffer.getHeight() / 2,
					null);
		}
	}

	private List<Vector2> getLocations(World world) {
		List<Vector2> list = new ArrayList<>();

		for(double[] novelty : world.getNoveltyTracker().getPreviousStates().values()) {
			list.add(new Vector2(novelty[0], novelty[1]));
		}

		return list;
	}

	private void renderLocation(Graphics2D g3, Vector2 loc, Rectangle r, BufferedImage map, int rgb, boolean highlight) {

		int x = (int) (loc.x - r.x);
		int y = (int) (loc.y - r.y);

		map.setRGB(x, y, rgb);

	}

	private boolean firstPauseRenderFast, firstPauseRenderNetwork, firstPauseRenderGraph;

	private BufferedImage heatMap;

	public Vector2 transformScreenToWorld(Vector2 screen) {
		double x = (((screen.x - frame.getWidth() / 2) * ((double) buffer.getWidth() / frame.getWidth()) - center.x
				- frame.getInsets().left)
				/ currentZoom);

		double y = (((screen.y - frame.getHeight() / 2) * ((double) buffer.getHeight() / frame.getHeight()) - center.y
				- frame.getInsets().top)
				/ currentZoom);

		return new Vector2(x, y);
	}

	public Vector2 transformWorldToScreen(Vector2 world) {
		double x = ((world.x / currentZoom) / ((double) buffer.getWidth() / frame.getWidth() - (center.x
				- frame.getInsets().left))) + frame.getWidth() / 2;

		double y = ((world.y / currentZoom) / ((double) buffer.getHeight() / frame.getHeight() - (center.y
				- frame.getInsets().top))) + frame.getHeight() / 2;

		return new Vector2(x, y);
	}

	private Color interpolateColor(Color COLOR1, Color COLOR2, float fraction) {
		final float INT_TO_FLOAT_CONST = 1f / 255f;
		fraction = Math.min(fraction, 1f);
		fraction = Math.max(fraction, 0f);

		final float RED1 = COLOR1.getRed() * INT_TO_FLOAT_CONST;
		final float GREEN1 = COLOR1.getGreen() * INT_TO_FLOAT_CONST;
		final float BLUE1 = COLOR1.getBlue() * INT_TO_FLOAT_CONST;
		final float ALPHA1 = COLOR1.getAlpha() * INT_TO_FLOAT_CONST;

		final float RED2 = COLOR2.getRed() * INT_TO_FLOAT_CONST;
		final float GREEN2 = COLOR2.getGreen() * INT_TO_FLOAT_CONST;
		final float BLUE2 = COLOR2.getBlue() * INT_TO_FLOAT_CONST;
		final float ALPHA2 = COLOR2.getAlpha() * INT_TO_FLOAT_CONST;

		final float DELTA_RED = RED2 - RED1;
		final float DELTA_GREEN = GREEN2 - GREEN1;
		final float DELTA_BLUE = BLUE2 - BLUE1;
		final float DELTA_ALPHA = ALPHA2 - ALPHA1;

		float red = RED1 + (DELTA_RED * fraction);
		float green = GREEN1 + (DELTA_GREEN * fraction);
		float blue = BLUE1 + (DELTA_BLUE * fraction);
		float alpha = ALPHA1 + (DELTA_ALPHA * fraction);

		red = Math.min(red, 1f);
		red = Math.max(red, 0f);
		green = Math.min(green, 1f);
		green = Math.max(green, 0f);
		blue = Math.min(blue, 1f);
		blue = Math.max(blue, 0f);
		alpha = Math.min(alpha, 1f);
		alpha = Math.max(alpha, 0f);

		return new Color(red, green, blue, alpha);
	}

	/**
	 * 
	 * @param fitnessOverTime
	 * @return Returns a graph of the values that may be scaled to match the rendering position
	 */
	public Polygraph createGraph(List<Double> fitnessOverTime) {
		Polygraph p = new Polygraph();

		if(fitnessOverTime.isEmpty()) {
			return p;
		}

		p.addPoint(0, 0);

		double highest = fitnessOverTime.get(0).intValue();
		for(Double d : fitnessOverTime) {
			if(highest < d) {
				highest = d;
			}
		}

		int x = 1;
		for(Double d : fitnessOverTime) {
			p.addPoint(x++, (int) ((d / highest) * frame.getHeight()));

		}

		// go back to y= 0 and connect to 0,0
		p.addPoint(x - 1, 0);
		p.highestFitness = highest;

		return p;
	}

	public JFrame getFrame() {
		return frame;
	}

	public int getImageWidth() {
		return buffer.getWidth();
	}

	public int getImageHeight() {
		return buffer.getHeight();
	}

	public void refreshScreen() {
		firstPauseRenderFast = true;
		firstPauseRenderNetwork = true;
		firstPauseRenderGraph = true;
	}

	public static class Polygraph extends Polygon {

		private static final long serialVersionUID = 7165010014839145739L;

		public double highestFitness;

		public int getEntryWidth() {
			return this.npoints - 2;
		}

	}

	public List<BufferedImage> getLocationList() {
		return locationList;
	}

}
