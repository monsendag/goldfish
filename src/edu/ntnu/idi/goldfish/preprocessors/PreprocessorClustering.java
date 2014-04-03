package edu.ntnu.idi.goldfish.preprocessors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;

import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import edu.ntnu.idi.goldfish.mahout.SMPreference;
import weka.classifiers.meta.ClassificationViaClustering;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.FilteredClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;
import weka.core.Instance;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class PreprocessorClustering extends Preprocessor{

	private ClassificationViaClustering cvc = null;
	private Instances data = null;
	
	public static DataModel getPreprocessedDataModel(String path) throws Exception {
		SMDataModel model;
		model = new SMDataModel(new File(path));
		PreprocessorClustering pre = new PreprocessorClustering();
		pre.preprocess(model);
		return model;
	}

	public static void main(String[] args) throws Exception {
		PreprocessorClustering pc = new PreprocessorClustering();
	}
	
	public PreprocessorClustering() throws Exception{
		
		// create the simpleKMeans cluster
		SimpleKMeans skm = new SimpleKMeans();
		skm.setNumClusters(5);
				
		//read the dataset and create instances for training and evaluating
		BufferedReader reader = new BufferedReader(
				new FileReader("datasets/yow-userstudy/arff/yow-preprocess-clustering.arff"));
		data = new Instances(reader);
		data.setClassIndex(0);
		
		cvc = new ClassificationViaClustering();
		cvc.setClusterer(skm);
		cvc.buildClassifier(data);
		
		System.out.println(cvc.toString());
		
	}
		
	public void preprocess(SMDataModel model) throws Exception {
		
		int counter = 0;
		// iterate through all items
		LongPrimitiveIterator it = model.getItemIDs();
		while (it.hasNext()) {
			
			long itemID = it.next();
			PreferenceArray prefs = model.getPreferencesForItem(itemID);
			
			// iterate through all prefs for item
			for (Preference p : prefs) {
	
				SMPreference pref = (SMPreference) p;
				boolean hasExplicit = pref.getValue(RATING_INDEX) >= 1;
	
				if (!hasExplicit) {
					Instance i = new Instance(1, new double[]{-1, pref.getValue(TIME_ON_PAGE_INDEX), 
							pref.getValue(TIME_ON_MOUSE_INDEX)});
					i.setDataset(data);
					int pseudorating = (int) (cvc.classifyInstance(i) + 1);
					
					System.out.println(String.format("%d: User %d gave item %d a pseudorating of: %d", 
							++counter, pref.getUserID(), pref.getItemID(), pseudorating));
					
					pref.setValue(pseudorating, RATING_INDEX);
					pseudoRatings.add(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
				}
			}
		}
	}

	@Override
	public DataModel preprocess(YowModel model) throws TasteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void ManualClassificationViaClustering() throws Exception{
		
		// create the simpleKMeans cluster
		SimpleKMeans clusterer = new SimpleKMeans();
		String[] options = new String[1];
		options[0] = "-V";
		clusterer.setOptions(options);
		clusterer.setNumClusters(5);
		
		//read the dataset and create instances for training and evaluating
		BufferedReader reader = new BufferedReader(
				new FileReader("datasets/yow-userstudy/arff/yow-preprocess-clustering.arff"));
		Instances data = new Instances(reader);
		Instances evalData = new Instances(data);
		evalData.setClassIndex(0);

		// remove the class index (user_like) since we are going to create clusters with time on page and mouse
		data = removeAttribute(data,1);
		
		clusterer.buildClusterer(data);
		
		// evaluate clusterer
	    ClusterEvaluation eval = new ClusterEvaluation();
	    eval.setClusterer(clusterer);
	    eval.evaluateClusterer(evalData);
	    
		System.out.println(eval.clusterResultsToString());

		Map<Integer,Integer> clusterToRating = new HashMap<Integer,Integer>();
		// map clusters to ratings
		int[] classToCluster = eval.getClassesToClusters();
		for (int i = 0; i < classToCluster.length; i++) {
			clusterToRating.put(i, (classToCluster[i]+1));
		}
		
//		<--- Put this where the pseduorating is going to be calculated --->
//		int cluster = clusterer.clusterInstance(new Instance(1, 
//		new double[]{pref.getValue(TIME_ON_PAGE_INDEX),pref.getValue(TIME_ON_MOUSE_INDEX)}));
//
//		int pseudorating = clusterToRating.get(cluster);
	}
	
	private Instances removeAttribute(Instances data, int attributeIndex) throws Exception {
		Remove remove = new Remove(); // new instance of filter
		String[] removeOptions = new String[2];
		removeOptions[0] = "-R"; // "range"
		removeOptions[1] = ""+attributeIndex; // we want to ignore the attribute that is in the position '1' (first)
		remove.setOptions(removeOptions); // set options
		remove.setInputFormat(data); // inform filter about dataset
		data = Filter.useFilter(data, remove);
		return data;
	}
}
