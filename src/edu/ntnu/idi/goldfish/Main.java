package edu.ntnu.idi.goldfish;

import java.io.File;
import java.io.IOException;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.example.grouplens.GroupLensDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;

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
		EvaluationResults results = Evaluator.evaluateMemoryBased(dataModel);
		
	 	
		
		// irstats: precision at X, amount of recommendations to consider, threshold
		
		
		
		
		results.print();
		results.save(set);
		
//		
		/**
		 * MODEL-based evaluation
		 */
		
		// clustering models (KMeans ... EM?)
//		
//		DataModel[] dataModels = UserClusterer.clusterUsers(dataModel, 5, new EuclideanDistanceMeasure());
//		
//		for(DataModel model : dataModels) {
//			
//		}
//		
//		for(EvaluationResult re : results) {
//			System.out.println(re);
//		}
//		
//		
		/*List<EvaluationResult> res;
		for(DataModel clusteredModel : dataModels) {
			res = evaluateMemoryBased(clusteredModel);
		}
		*/
		// latent semantic models (Matrix Factorizations, etc..)
		
		
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
