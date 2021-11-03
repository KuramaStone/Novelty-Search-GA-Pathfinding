package me.brook.tasgenetics;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.brook.neat.GeneticCarrier;
import me.brook.neat.InnovationHistory;
import me.brook.neat.Population;
import me.brook.neat.network.NoveltyTracker;
import me.brook.neat.species.Species;
import me.brook.tasgenetics.tools.Vector2;
import rta.gambatte.Gb;

public class World {

	private static final InnovationHistory innovationHistory = new InnovationHistory();

	private Engine engine;

	private Species<Agent> species;
	private NoveltyTracker<Agent> noveltyTracker;
	private List<Agent> entities;
	private Map<Agent, Vector2> history;
	private Random random;

	private int currentGeneration;
	private int currentTime;
	private boolean isSimulating;

	private int population = 100;
	public int attempt;

	private int generationsSinceNovelty;
	private int lastNoveltyAmount;
	private int noveltyCreated;

	private long startTime, endTime = -1;
	private boolean searchingForSolution = true;

	// Emulator
	private Gb gb;
	private Thread thread;
	private ExecutorService executor;

	private List<Vector2> finalLocations;

	public World(Engine engine) {
		this.engine = engine;
		random = new Random();
		finalLocations = new ArrayList<>();
		history = new HashMap<>();

		startTime = System.currentTimeMillis();

		executor = Executors.newCachedThreadPool();

		noveltyTracker = new NoveltyTracker(0.5);
		species = new Species<Agent>(createStartingRep());
		species.fillFromRep(population, true, 1);
	}

	public void update() {
		if(!searchingForSolution) {
			return;
		}
		if(!engine.isRunning()) {
			return;
		}

		simulateInputs();

		// select
		selection();
	}

	private void simulateInputs() {
		isSimulating = false;
		try {
			executor.invokeAll(createTasks());
			System.out.println();
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

	private List<EmulateTask> createTasks() {
		int threads = (int) Math.max(1, Math.min(6, species.size()));
		List<EmulateTask> list = new ArrayList<>(threads);

		ConcurrentLinkedQueue<Agent> entities = new ConcurrentLinkedQueue<>(species);

		for(int i = 0; i < threads; i++)
			list.add(new EmulateTask(entities));

		return list;
	}

	private void selection() {
		// add novelty
		species.forEach(entity -> entity.calculateNoveltyMetrics());
		species.forEach(entity -> {
			if(entity.getNoveltyMetrics() != null)
				noveltyTracker.submit(entity, entity.getNoveltyMetrics());
		});
		species.forEach(agent -> {
			if(agent.validate()) {
				Vector2 loc = agent.getLocation();
				// location is null if they enter a new room

				if(loc != null) {
					finalLocations.add(loc);
				}
			}
		});

		// get fitness
		species.calculateFitness();
		species.useSorter();

		double best = species.get(0).getFitness();

		if(species.get(0).getNoveltyMetrics() != null)
			if(noveltyTracker.getDistanceToClosestState(species.get(0), species.get(0).getNoveltyMetrics()) > 0) {
				history.put(species.get(0), species.get(0).getLocation());
			}

		int historyAmount = (int) (population * 0.25);
		historyAmount = (int) (historyAmount * Math.min(1.0, generationsSinceNovelty / 10.0));

		List<Agent> historyReplacements = selectFromHistory(historyAmount);

		List<GeneticCarrier> repeats = new ArrayList<>();
		if(!history.isEmpty() && historyReplacements != null)
			for(int i = 0; i < historyReplacements.size(); i++) {
				Agent a = (Agent) historyReplacements.get(i).createClone();
				a.setTimeAlive(historyReplacements.get(i).getTimeAlive());
				a.mutate(1);

				repeats.add(a);
			}

		if(historyReplacements == null) {
			historyAmount = 0;
		}
		else {
			historyAmount -= historyAmount - historyReplacements.size();
		}

		List<GeneticCarrier> next = Population.tournamentSelection(species, population - historyAmount, 0.33, 0.9, 0.00,
				null,
				Population.HIGH_FITNESS);
		next.addAll(repeats);

		species.clear();
		next.forEach(a -> species.add((Agent) a));

		if(noveltyTracker.getPreviousStates().size() == lastNoveltyAmount) {
			generationsSinceNovelty++;
		}
		else {
			generationsSinceNovelty -= 2;
			generationsSinceNovelty = Math.max(0, generationsSinceNovelty);
		}

		noveltyCreated = noveltyTracker.getPreviousStates().size() - lastNoveltyAmount;

		System.out.println(String.format("Generation %s has %s+%s novelties. %s since novelty. %s supplemented.",
				currentGeneration,
				noveltyTracker.getPreviousStates().size(), noveltyCreated,
				generationsSinceNovelty, historyAmount));

		lastNoveltyAmount = noveltyTracker.getPreviousStates().size();
		currentGeneration++;
	}

	private List<Agent> selectFromHistory(int amount) {
		if(history.size() < amount) {
			return null;
		}

		// select an agent that has few nearby partners

		Map<Double, Agent> toSort = new HashMap<>();

		for(Agent agent : history.keySet()) {
			Vector2 loc = history.get(agent);

			double totalDist = 0;
			int nearby = 0;

			// store average distance to toSort
			for(Agent agent2 : history.keySet()) {
				if(agent != agent2) {
					double d = history.get(agent2).distanceToRaw(loc);

					totalDist += d;
					nearby++;
				}
			}

			totalDist /= nearby;

			toSort.put(totalDist, agent);
		}

		// list that will contain the sorted values
		List<Agent> sorted = new ArrayList<>();

		// we sort the keys of the map here
		List<Double> list = new ArrayList<>(toSort.keySet());
		list.sort(Comparator.reverseOrder());

		// we put each sorted key to make a sorted list of the hashmap values
		list.forEach(d -> sorted.add(toSort.get(d)));

		List<Agent> replacements = new ArrayList<>();

		double threshold = 0.33;

		// now to choose which ones to select! We'll remove the one we've chosen after choosing it to prevent repeats.
		for(int i = 0; i < amount; i++) {
			if(sorted.isEmpty())
				break;

			Agent chosen = sorted.get((int) (random.nextDouble() * sorted.size() * threshold));

			replacements.add(chosen);
			sorted.remove(chosen);
		}

		return replacements;
	}

	private Agent createStartingRep() {
		Agent agent = new Agent(innovationHistory, this);
		return agent;
	}

	public int getCurrentGeneration() {
		return currentGeneration;
	}

	public BufferedImage getBestBrainImage() {
		return null;
	}

	public int getTime() {
		return currentTime;
	}

	public int getSelectedPopulation() {
		// TODO Auto-generated method stub
		return 0;
	}

	public List<Double> getFitnessOverTime() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getLivingPopulation() {
		int alive = species.getLivingPopulation();

		return alive;
	}

	public Species<Agent> getSelectedSpecies() {
		return null;
	}

	public Species<Agent> getSpecies() {
		return species;
	}

	public Collection<Agent> getEntities() {
		return entities;
	}

	public Agent getSelectedAgent() {
		return null;
	}

	public Vector2 getCursorLocation() {
		return null;
	}

	public void setSelectedAgent(Agent closest) {

	}

	public void save() {

	}

	public void setCursorLocation(Vector2 transformScreenToWorld) {

	}

	public NoveltyTracker<Agent> getNoveltyTracker() {
		return noveltyTracker;
	}

	public List<Vector2> getFinalLocations() {
		return finalLocations;
	}

	public int getGenerationsSinceNovelty() {
		return generationsSinceNovelty;
	}

	public int getPopulation() {
		return population;
	}

	public int getNoveltyCreated() {
		return noveltyCreated;
	}

	public void setSolutionFound(boolean boo) {
		this.searchingForSolution = !boo;
		
		if(!searchingForSolution) {
			endTime = System.currentTimeMillis();
			engine.stop();
		}
	}

	public long getElapsedTime() {
		if(endTime == -1) {
			return System.currentTimeMillis() - startTime;
		}

		return endTime - startTime;
	}

}
