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
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.common.distance.TanimotoDistanceMeasure;

import edu.ntnu.idi.goldfish.MemoryBased.Similarity;

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
		set = DataSet.VTT36k;
		
		DataModel dataModel = getDataModel(set);
		Evaluator evaluator = new Evaluator();
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		EvaluationResults results = new EvaluationResults();
		
		for(int topN = 10; topN <= 10; topN += 1) {
			// kNN
			int[] Ks = new int[] {2,3,5,7,9,15,20,25};
			for(int K : Ks) {
	            evaluations.add(new KNN(topN, Similarity.TanimotoCoefficient, K));      
	            evaluations.add(new KNN(topN, Similarity.LogLikelihood, K));                        
	        }
			
			// Threshold
			double lowT = 0.05;
			double highT = 0.40;
			double incrT = 0.05;
			for(double T = lowT; T <= highT; T += incrT) {
				evaluations.add(new Threshold(topN, Similarity.TanimotoCoefficient, T));
				evaluations.add(new Threshold(topN, Similarity.LogLikelihood, T));
			}
		
//			// matrix factorization
//			int[] numFeatures = new int[] {10, 20, 30};
//			int numIterations = 10;
//			double lambda = 2;
//			boolean usesImplicitFeedback = true;
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
//			
//			for(int L : numFeatures) {
//				evaluations.add(new ALSWR(topN, L, numIterations, lambda, usesImplicitFeedback, alpha));
//				evaluations.add(new ParallelSGD(topN, L, numIterations, lambda, mu0, decayFactor, stepOffset, forgettingExponent, biasMuRatio, biasLambdaRatio));
//				evaluations.add(new RatingSGD(topN, L, numIterations, learningRate, preventOverfitting, randomNoise, learningRateDecay));
//				evaluations.add(new SVDPlusPlus(topN, L, numIterations, learningRate, preventOverfitting, randomNoise, learningRateDecay));
//			}
		}
		int[] clusterSizes = new int[] {2,3,5,7,9,11};
		StopWatch.start("total evaluation");
		for(int size : clusterSizes) {
			results = new EvaluationResults();
			evaluator.evaluateClustered(size, new TanimotoDistanceMeasure(), evaluations, results, dataModel, 0.1);		
			results.print();
			results.save(set,String.format("clusters-%d", size));
		}
		System.out.format("Completed evaluation in %s\n", StopWatch.str("total evaluation"));
		
//		evaluator.evaluateUnclustered(evaluations, results, dataModel, 0.1);
		
	}
	

	public static enum DataSet {
		Movielens1M,
		Sample100,
		VTT36k
	}
	
	public static DataModel getDataModel(DataSet set) throws IOException, TasteException {
		
		switch(set) {
		case Movielens1M:
			return new GroupLensDataModel(new File("datasets/movielens-1m/ratings.dat.gz"));
		case Sample100:
			return new GroupLensDataModel(new File("datasets/sample100/ratings.dat.gz"));
		case VTT36k:
			DataModel dataModel = new FileDataModel(new File("datasets/vtt-36k/VTT_I_data.csv"));
			return new GenericBooleanPrefDataModel(GenericBooleanPrefDataModel.toDataMap(dataModel));
		}
		
		return null;
	}

}
