package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.configurations.Configuration;
import edu.ntnu.idi.goldfish.configurations.Lynx;
import org.apache.mahout.cf.taste.model.DataModel;

import java.util.ArrayList;
import java.util.List;

import static org.bitbucket.dollar.Dollar.$;

public class Main {

    public static DataSet set;
	
	// disable Mahout logging output
	static {
		 System.setProperty("org.apache.commons.logging.Log",
		 "org.apache.commons.logging.impl.NoOpLog");
	}

	public static void main(String[] args) throws Exception {

//		 set = DataSet.Movielens1M;
//		 set = DataSet.Sample100;
//		 set = DataSet.Movielens1Mbinary;
//		 set = DataSet.Movielens50kbinary;

//		 set = DataSet.MovielensSynthesized1M;
//		 set = DataSet.MovielensSynthesized200k;
//		set = DataSet.Movielens1M;
//		 set = DataSet.Movielens50k;
//		 set = DataSet.yow10kratings;
		 set = DataSet.yow10kprocessed;
		// set = DataSet.food;

		DataModel dataModel = set.getModel();
		Evaluator evaluator = new Evaluator();
		List<Configuration> configurations = new ArrayList<Configuration>();
		ResultList results = new ResultList(dataModel);

		List<Integer> topNvals = $(10, 111).toList();
		for (int topN : topNvals) {

			int numFeatures = 10;
			int numIterations = 20;
			double lambda = 0.01;
			// these next two control decayFactor^steps exponential type of
			// annealing learning rate and decay factor
			double mu0 = 0.01;
			double decayFactor = 1;

			// these next two control 1/steps^forget type annealing
			int stepOffset = 0;
			// -1 equals even weighting of all examples, 0 means only use
			// exponential annealing
			double forgettingExponent = 0;

			// The following two should be inversely proportional :)
			double biasMuRatio = 0.5;
			double biasLambdaRatio = 0.1;
			// Learning rate (step size)
			double learningRate = 0.01;
			// Parameter used to prevent overfitting
			double preventOverfitting = 0.1;
			// Standard deviation for random initialization of features
			double randomNoise = 0.01;
			// Multiplicative decay factor for learning_rate
			double learningRateDecay = 1.0;

			configurations.add(new Lynx(topN, numFeatures, numIterations, lambda, mu0, decayFactor, stepOffset,
					forgettingExponent, biasMuRatio, biasLambdaRatio));
			// configurations.add(new RatingSGD(topN, numFeatures, numIterations,
			// learningRate, preventOverfitting, randomNoise,
			// learningRateDecay));
			//
			// configurations.add(new KNN(topN,
			// MemoryBased.Similarity.EuclideanDistance, 2));
			// configurations.add(new KiwiConfiguration(topN, new double[]{2, 1}, new
			// double[]{10, 10, 2}));
			// configurations.add(new SVDPlusPlus(topN, numFeatures, numIterations,
			// learningRate, preventOverfitting, randomNoise,
			// learningRateDecay));
		}

		StopWatch.start("total configuration");
		evaluator.evaluateUnclustered(configurations, results, dataModel, 1.0);
		results.save();
		// results.print();
		System.out.format("Completed configuration in %s\n", StopWatch.str("total configuration"));
	}
}
