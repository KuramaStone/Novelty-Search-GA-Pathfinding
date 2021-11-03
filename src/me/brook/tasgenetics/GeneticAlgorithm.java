package me.brook.tasgenetics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

import me.brook.neat.randomizer.SexualReproductionMixer.Crossover;

public class GeneticAlgorithm<T extends Serializable> implements Serializable {

	private static final long serialVersionUID = 4396681564794700728L;

	private T[] genes;
	private Random random;

	private Mutate mutationFunction;

	public GeneticAlgorithm(Mutate mutate, T[] genes) {
		random = new Random();
		this.genes = genes;
		this.mutationFunction = mutate;
	}

	public GeneticAlgorithm() {
	}

	public void mutate(double mutationRate) {
		for(int i = 0; i < genes.length; i++) {
			T t = genes[i];

			if(random.nextDouble() < mutationRate)
				genes[i] = (T) mutationFunction.mutate(t);
		}
	}

	public void crossover(Crossover cross, GeneticAlgorithm<T> ga) {

		int multipleCrossLength = (int) (random.nextDouble() * 0.25 * genes.length);
		boolean isInCrossover = false;

		int singleCrossPoint = (int) (random.nextDouble() * genes.length);
		boolean singleCrossFlipped = random.nextBoolean();

		for(int i = 0; i < genes.length; i++) {

			if(ga.genes.length > i) {
				boolean shouldCross = false;

				if(cross == Crossover.UNIFORM) {
					shouldCross = random.nextBoolean();
				}
				else if(cross == Crossover.MULTIPLE) {
					if(i % multipleCrossLength == 0) {
						isInCrossover = !isInCrossover;
					}

					shouldCross = isInCrossover;
				}
				else if(cross == Crossover.SINGLE) {
					if(singleCrossFlipped)
						shouldCross = i <= singleCrossPoint;
					else
						shouldCross = i > singleCrossPoint;
				}

				if(shouldCross)
					this.genes[i] = ga.genes[i];
			}

		}

	}

	public T[] getGenes() {
		return genes;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		int index = 0;
		for(T t : genes) {
			sb.append(t.toString());
			
			if(index++ != genes.length) {
				sb.append(", ");
			}
			
		}
		
		return sb.toString();
	}

	public static abstract class Mutate implements Serializable {

		private static final long serialVersionUID = -5839060184034230953L;

		public abstract Object mutate(Object t);

	}

	public GeneticAlgorithm<T> copy() throws Exception {

		T[] genes = this.genes.clone();

		for(int i = 0; i < genes.length; i++) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bos);
			out.writeObject(this.genes[i]);

			ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
			ObjectInputStream in = new ObjectInputStream(bis);
			
			genes[i] = (T) in.readObject();
		}

		GeneticAlgorithm<T> ga = new GeneticAlgorithm<>(mutationFunction, genes);
		return ga;
	}
	
	public void setGenes(T[] genes) {
		this.genes = genes;
	}

}
