package me.brook.tasgenetics;

import java.io.File;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import me.brook.neat.GeneticCarrier;
import me.brook.neat.InnovationHistory;
import me.brook.neat.network.NeatNetwork;
import me.brook.neat.species.Species;
import me.brook.tasgenetics.tools.Vector2;
import rta.RedBlueAddr;
import rta.gambatte.Gb;
import rta.tid.RedBlueTIDManip;
import rta.tid.RedBlueTIDManip.Checkable;
import rta.tid.RedBlueTIDManip.Strat;
import rta.tid.RedBlueTIDManip.Testable;

public class Agent implements GeneticCarrier, Testable {

	private final UUID uuid = UUID.randomUUID();

	private Vector2 location, startLocation;
	private World world;
	private Random random;

	private InnovationHistory innovationHistory;
	private GeneticAlgorithm<Integer> genetics;

	private double[] noveltyMetrics;
	private double fitness;
	private String status;
	private boolean isAlive = true;
	private boolean hasWon;
	private double distanceToGoal = 100;
	private boolean hasTicked = false;
	private int timeAlive = -1;
	private boolean shouldMutate = true;

	private Strat strat;

	public Agent(InnovationHistory innovationHistory, World world) {
		this.innovationHistory = innovationHistory;
		this.world = world;
		random = new Random();

//		 Integer[] genes = loadFromString(
//		 "2, 2, 2, 0, 5, 2, 4, 0, 2, 2, 4, 0, 3, 2, 4, 2, 2, 2, 2, 2, 5, 5, 2, 5, 5, 5, 5, 5, 5, 2, 2, 5, 2, 2, 2, 2, 5, 2, 5, 5, 1, 5, 5, 4"); // first ladder
		Integer[] genes = randomGenes(1);

		genetics = new GeneticAlgorithm<Integer>(new me.brook.tasgenetics.GeneticAlgorithm.Mutate() {

			private static final long serialVersionUID = 242327537817549480L;

			@Override
			public Object mutate(Object t) {
				return t;
			}
		}, genes);
	}

	private Integer[] randomGenes(int length) {

		Integer[] genes = new Integer[length];
		int i = 0;

		for(; i < length; i++) {
			genes[i] = random.nextInt(6);
		}

		return genes;
	}

	private Integer[] loadFromString(String string) {
		String[] split = string.replaceAll(" ", "").split(",");

		int length = split.length;
		timeAlive = length;

		Integer[] genes = new Integer[length];
		int i = 0;

		for(i = 0; i < split.length; i++) {
			genes[i] = Integer.valueOf(split[i]);
		}

		for(; i < length; i++) {
			genes[i] = random.nextInt(6);
		}

		return genes;
	}

	private Integer getInputFromChar(char c) {

		return switch(c) {
			case 'A' : {
				yield 0;
			}
			case 'B' : {
				yield 1;
			}
			case 'U' : {
				yield 2;
			}
			case 'D' : {
				yield 3;
			}
			case 'L' : {
				yield 4;
			}
			case 'R' : {
				yield 5;
			}
			default:
				yield 6;
		};
	}

	public void update() {

	}

	public void calculateOutputs() {

	}

	@Override
	public boolean isAlive() {
		return isAlive;
	}

	public void setTimeAlive(int timeAlive) {
		this.timeAlive = timeAlive;
	}

	public int getTimeAlive() {
		return timeAlive;
	}

	@Override
	public NeatNetwork getBrain() {
		return null;
	}

	@Override
	public double getFitness() {
		return fitness;
	}

	@Override
	public void setFitness(double fitness) {
		this.fitness = fitness;
	}

	@Override
	public long getTimeOfDeath() {
		return 0;
	}

	@Override
	public void setTimeOfDeath(long time) {

	}

	@Override
	public GeneticCarrier createClone() {
		Agent agent = new Agent(innovationHistory, world);

		try {
			agent.timeAlive = this.timeAlive;
			agent.genetics = this.genetics.copy();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return agent;
	}

	public void setGenetics(GeneticAlgorithm<Integer> genetics) {
		this.genetics = genetics;
	}

	@Override
	public GeneticCarrier breed(GeneticCarrier insertions) {
		return this.createClone();
	}

	@Override
	public void setSpecies(Species<? extends GeneticCarrier> species) {

	}

	@Override
	public Species<? extends GeneticCarrier> getSpecies() {
		return null;
	}

	@Override
	public double[] getNoveltyMetrics() {
		return noveltyMetrics;
	}

	public Integer[] getTASInputs() {
		return genetics.getGenes();
	}

	public Species<Agent> getParentSpecies() {
		return null;
	}

	public me.brook.tasgenetics.tools.Vector2 getLocation() {
		return location;
	}

	public UUID getUUID() {
		return uuid;
	}

	public Strat createStrat(Gb gb) {
		int length = genetics.getGenes().length;

		int[][] addr = new int[length][];
		int[] inputs = new int[length];
		int[] adv = new int[length];

		for(int i = 0; i < length; i++) {
			addr[i] = new int[] { RedBlueAddr.joypadOverworldAddr, RedBlueAddr.printLetterDelayAddr };
			inputs[i] = getInputAddressFor(genetics.getGenes()[i]);
			adv[i] = 1;

		}

		Checkable<Integer> test = new Checkable<Integer>() {

			int v = -1;

			/**
			 * Returns true if should continue, false if not.
			 */
			@Override
			public boolean check(Integer hit) {
				if(!isAlive()) {
					return false;
				}

				hasTicked = true;

				// hit checks
				if(hit == RedBlueAddr.printLetterDelayAddr) {
					status(false, "text detected");
					return false;
				}

				// other

				int inBattle = gb.readMemory(0xD057);
				int map = gb.readMemory(0xD35E);

				// 17,11 for mt moon tunnel
				Vector2 oldLoc = location == null ? null : location.copy();
				Vector2 loc = new Vector2(gb.readMemory(0xD362), gb.readMemory(0xD361));
				setLocation(loc);

				// their distance from the actual map=60 entrance
				double distanceToGoal = new Vector2(17, 11).manhattanDistance(loc);

				if(inBattle != 0) {
					setLocation(oldLoc);
					status(false, "in battle.");
					return false;
				}
				else if(map != 59) {
					if(map == 60 && distanceToGoal < 5) { // map if 60 is subroom in mt moon
						setLocation(oldLoc); // set location since it'll be giving coords for the new room
						status(false, "goal achieved!");
						return false;
					}
					else {
						setLocation(oldLoc); // set location since it'll be giving coords for the new room
						status(false, "wrong room.");
						return false;
					}
				}

				return true;
			}
		};

		File filepath = new File("E:\\Gaming\\GBA\\savestates\\speedrun\\red\\Manips\\fullmoon.gqs");
		strat = new Strat(this, uuid.toString(), filepath, 1, addr, inputs, adv);
		strat.setTest(test);

		return strat;
	}

	@Override
	public void status(boolean won, String status) {
		isAlive = won;
		this.hasWon = won;

		// if(location != null) {
		// System.out.println(location);
		// if(location.equals(0, 57)) {
		// getLocation();
		// }
		// }

//		if(status.equals("timed out")) {
//			System.out.println("timedout: " + genetics.toString());
//		}

		if(hasWon) {
//			System.out.println("Winning genetics: " + genetics.toString());
//			System.out.println("Found in " + world.getElapsedTime() / 1000.0 + " seconds");
			world.setSolutionFound(true);
		}

		this.status = status;
	}

	private Integer getInputAddressFor(Integer integer) {

		int input = switch(integer) {
			case 0 : {
				yield RedBlueTIDManip.A;
			}
			case 1 : {
				yield RedBlueTIDManip.B;
			}
			case 2 : {
				yield RedBlueTIDManip.UP;
			}
			case 3 : {
				yield RedBlueTIDManip.DOWN;
			}
			case 4 : {
				yield RedBlueTIDManip.LEFT;
			}
			case 5 : {
				yield RedBlueTIDManip.RIGHT;
			}
			default:
				yield RedBlueTIDManip.NO_INPUT;
		};

		return input;
	}

	public World getWorld() {
		return world;
	}

	public Strat getStrat() {
		return strat;
	}

	public GeneticAlgorithm<Integer> getGenetics() {
		return genetics;
	}

	private int locationsTracked = 0;

	public void setLocation(Vector2 location) {
		if(startLocation == null) {
			startLocation = location;
		}

		this.location = location == null ? null : location.copy();

		// if(this.location != null)
		// world.getNoveltyTracker().submit(uuid.toString() + "-" + String.valueOf(locationsTracked),
		// calculateNoveltyMetrics());

		locationsTracked++;
	}

	public void emulate(Gb gb) {

		try {
			createStrat(gb).execute(gb);
			setTimeAlive(strat.getLastIndex());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		getBrain();
	}

	@Override
	public void mutate(float multiplier) {
		if(!shouldMutate)
			return;
		Integer[] genes = new Integer[genetics.getGenes().length];
		System.arraycopy(genetics.getGenes(), 0, genes, 0, genes.length);

		double mutate_add = 0.5;
		double mutation_rate = 0.25;

		if(random.nextDouble() < mutate_add) {
			Integer[] added = new Integer[genes.length + 1];

			for(int i = 0; i < genes.length; i++)
				added[i] = genes[i];

			added[added.length - 1] = random.nextInt(6);

			genes = added;
		}

		int mutationStart = Math.max(0, genes.length - 10);

		double index = 1;
		for(int i = mutationStart; i < genes.length; i++) {
			double rate = mutation_rate * (1 - 1 / (index++));

			if(rate > random.nextDouble()) {
				genes[i] = random.nextInt(6);
			}
		}

		// System.out.println(
		// String.format("%s %s", (double) mutationStart / genes.length, (double) mutationEnd / genes.length));
		// System.out.println(Arrays.toString(genes));
		// System.out.println(Arrays.toString(genetics.getGenes()));

		genetics.setGenes(genes);
	}

	@Override
	public double[] calculateNoveltyMetrics() {
		if(location == null) {
			return null;
		}

		this.noveltyMetrics = new double[] { location.x, location.y };

		return getNoveltyMetrics();
	}

	@Override
	public double calculateFitness() {
		if(getNoveltyMetrics() == null)
			return 0;

		double novelty = world.getNoveltyTracker().getDistanceToClosestState(this, getNoveltyMetrics());
		double dist = location == null ? 1 : new Vector2(17, 11).distanceToRaw(location);

		double fitness = novelty * 1 / dist;

		setFitness(fitness);
		return getFitness();
	}

	public boolean validate() {
		return hasTicked;
	}

	@Override
	public void setBrain(NeatNetwork brain) {
	}

}
