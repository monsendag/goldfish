package edu.ntnu.idi.goldfish.preprocessors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;

import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import edu.ntnu.idi.goldfish.mahout.SMPreference;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;
import weka.core.Instance;

public class PreprocessorClustering extends Preprocessor{

	private SimpleKMeans clusterer = null;
	
	public static DataModel getPreprocessedDataModel(String path) throws Exception {
		SMDataModel model;
		model = new SMDataModel(new File(path));
		PreprocessorClustering pre = new PreprocessorClustering();
		pre.preprocess(model);
		return model;
	}
	
	public PreprocessorClustering() throws Exception{
		String[] options = new String[1];
		options[0] = "-V";
		clusterer = new SimpleKMeans();
		
		BufferedReader reader = new BufferedReader(
				new FileReader("datasets/yow-userstudy/yow-preprocess-clustering.arff"));
		Instances data = new Instances(reader);
		
		clusterer.setOptions(options);
		clusterer.setNumClusters(5);
		clusterer.buildClusterer(data);
		
		System.out.println(clusterer.toString());
		
		
	}
	
	public int getRating(double timeOnPage, double timeOnMouse) throws Exception{
		return clusterer.clusterInstance(new Instance(1, new double[]{timeOnPage,timeOnMouse}));
	}
	
	public void preprocess(SMDataModel model) throws Exception {
		
		// iterate through all items
		LongPrimitiveIterator it = model.getItemIDs();
		while (it.hasNext()) {
			
			long itemID = it.next();
			PreferenceArray prefs = model.getPreferencesForItem(itemID);
			
			// iterate through all prefs for item
			for (Preference p : prefs) {
	
				SMPreference pref = (SMPreference) p;
				boolean hasExplicit = pref.getValue(0) >= 1;
	
				if (!hasExplicit) {
					float[] feedback = pref.getValues();
					int centroid = clusterer.clusterInstance(new Instance(1, 
							new double[]{feedback[1],feedback[2]}));
					
					System.out.println(String.format("User %d gave item %d a pseudorating of: %d", 
							pref.getUserID(), pref.getItemID(), centroid));
					int pseudorating = centroid;
					
					pref.setValue(pseudorating, 0);
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
}
