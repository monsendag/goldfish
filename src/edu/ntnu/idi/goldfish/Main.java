 package edu.ntnu.idi.goldfish;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.example.grouplens.GroupLensDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;

import edu.ntnu.idi.goldfish.mahout.SMDataModel;

public class Main {
	
	// disable Mahout logging output
	static { System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog"); }
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws TasteException 
	 * @throws ClassNotFoundException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, TasteException, InterruptedException, ClassNotFoundException {
		
		DataSet set;

//		set = DataSet.Movielens1M;
//		set = DataSet.Sample100;
//		set = DataSet.Movielens1Mbinary;
//		set = DataSet.Movielens50kbinary;
//		set = DataSet.Movielens50k;
		set = DataSet.MovielensSynthesized50k;
//		set = DataSet.Movielens1M;
		
		DataModel dataModel = getDataModel(set);
		Evaluator evaluator = new Evaluator();
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		EvaluationResults results = new EvaluationResults();
	
		for(int topN = 10; topN <= 10; topN += 1) {
	
//			evaluations.add(new KNN(topN, MemoryBased.Similarity.EuclideanDistance, 2));
			evaluations.add(new KiwiEvaluation(topN, 10));
			
//			// matrix factorization
//			int[] numFeatures = new int[] {5, 10, 20};
//			int numIterations = 10;
//			double lambda = 2;
//			boolean usesImplicitFeedback = false;
//			double alpha = 40;
//			double mu0 = 0.0;
//			double decayFactor = 0.0;
//			int stepOffset = 1;
//			double forgettingExponent = 0.0;
//			double biasMuRatio = 0.0;
//			double biasLambdaRatio = 0.0;
//			double learningRate = 1.0;
//			double preventOverfitting = 1.0;
//			double randomNoise = 1.0;
//			double learningRateDecay = 0.5;
//			double regularization = 0.5;
//			
//			for(int L : numFeatures) {
////				evaluations.add(new ALSWR(topN, L, numIterations, lambda, usesImplicitFeedback, alpha));
//				evaluations.add(new ParallelSGD(topN, L, numIterations, lambda, mu0, decayFactor, stepOffset, forgettingExponent, biasMuRatio, biasLambdaRatio));
//				evaluations.add(new RatingSGD(topN, L, numIterations, learningRate, preventOverfitting, randomNoise, learningRateDecay));
//				evaluations.add(new SVDPlusPlus(topN, L, numIterations, learningRate, preventOverfitting, randomNoise, learningRateDecay));
//				//evaluations.add(new Evaluation(topN, (int) L, numIterations, randomNoise, learningRateDecay, regularization));
//			}
		}

		StopWatch.start("total evaluation");
		evaluator.evaluateUnclustered(evaluations, results, dataModel, 0.1);
//		results.save(set);
//		results.print();
		System.out.format("Completed evaluation in %s\n", StopWatch.str("total evaluation"));
	}

	public static enum DataSet {
		Movielens1M,
		Movielens1Mbinary,
		Movielens50k,
		Movielens50kbinary,
		MovielensSynthesized50k,
		Sample100,
		VTT36k
	}
	
	public static DataModel getDataModel(DataSet set) throws IOException, TasteException {
		DataModel dataModel;
		switch(set) {
		case Movielens1M:
			return new GroupLensDataModel(new File("datasets/movielens-1m/ratings.dat.gz"));
		case Movielens1Mbinary:
			dataModel = new FileDataModel(new File("datasets/movielens-1m/ratings-binary.csv"));
			return new GenericBooleanPrefDataModel(GenericBooleanPrefDataModel.toDataMap(dataModel));
		case Movielens50k:
			return new GroupLensDataModel(new File("datasets/movielens-1m/ratings-50k.dat"));
		case Movielens50kbinary:
			dataModel = new FileDataModel(new File("datasets/movielens-1m/ratings-binary-50k.csv"));
			return new GenericBooleanPrefDataModel(GenericBooleanPrefDataModel.toDataMap(dataModel));
		case MovielensSynthesized50k:
			dataModel = new SMDataModel(new File("datasets/movielens-synthesized/ratings-synthesized-50k.csv"));
			return dataModel;
		case Sample100:
			return new GroupLensDataModel(new File("datasets/sample100/ratings.dat.gz"));
		case VTT36k:
			dataModel = new FileDataModel(new File("datasets/vtt-36k/VTT_I_data.csv"));
			return new GenericBooleanPrefDataModel(GenericBooleanPrefDataModel.toDataMap(dataModel));
		}
		return null;
	}
}
