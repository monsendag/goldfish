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

import edu.ntnu.idi.goldfish.mahout.KiwiRecommender;
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
//		set = DataSet.MovielensSynthesized1M;
		set = DataSet.MovielensSynthesized200k;
//		set = DataSet.Movielens1M;
//		set = DataSet.food;
		
		DataModel dataModel = getDataModel(set);
		Evaluator evaluator = new Evaluator();
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		EvaluationResults results = new EvaluationResults();
	
		int[] topNvals = {3, 5};
		for(int topN : topNvals) {
	
//			evaluations.add(new KNN(topN, MemoryBased.Similarity.EuclideanDistance, 2));
			evaluations.add(new KiwiEvaluation(topN, 10));			

		}
		

		StopWatch.start("total evaluation");
		evaluator.evaluateUnclustered(evaluations, results, dataModel, 0.001);
//		results.save(set);
//		results.print();
		System.out.format("Completed evaluation in %s\n", StopWatch.str("total evaluation"));
		KiwiRecommender.close();

	}

	public static enum DataSet {
		Movielens1M,
		Movielens1Mbinary,
		Movielens50k,
		Movielens50kbinary,
		MovielensSynthesized1M,
		MovielensSynthesized200k,
		MovielensSynthesized50k,
		Sample100,
		VTT36k,
		food
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
		case MovielensSynthesized1M:
			return new SMDataModel(new File("datasets/movielens-synthesized/ratings-synthesized.csv"));
		case MovielensSynthesized200k:
			return new SMDataModel(new File("datasets/movielens-synthesized/ratings-synthesized-200k.csv"));
		case MovielensSynthesized50k:
			return  new SMDataModel(new File("datasets/movielens-synthesized/ratings-synthesized-50k.csv"));
		case Sample100:
			return new GroupLensDataModel(new File("datasets/sample100/ratings.dat.gz"));
		case VTT36k:
			dataModel = new FileDataModel(new File("datasets/vtt-36k/VTT_I_data.csv"));
			return new GenericBooleanPrefDataModel(GenericBooleanPrefDataModel.toDataMap(dataModel));
		case food:
			return new SMDataModel(new File("datasets/FOOD_Dataset/food-ettellerannet.csv"));
		}
		return null;
	}
}
