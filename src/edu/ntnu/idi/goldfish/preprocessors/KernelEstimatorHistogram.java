package edu.ntnu.idi.goldfish.preprocessors;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;

import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import edu.ntnu.idi.goldfish.mahout.SMPreference;

public class KernelEstimatorHistogram {
	private final int BIN_SIZE = 500;
	private final int MAXBIN = 50000;
	private final int NUMBER_OF_HISTOGRAMS = 100;
	
	private int[] histograms;
	
	public KernelEstimatorHistogram(SMDataModel dataModel) throws TasteException{
		histograms = new int[NUMBER_OF_HISTOGRAMS];
		
		init(dataModel);
		
//		printHistograms();
	}
	
	public void printHistograms(){
		System.out.println("Kernel estimator:");
		for (int i = 0; i < histograms.length; i++) {
			System.out.print(histograms[i] + ", ");
		}
		System.out.println("");
	}
	
	public void init(SMDataModel dataModel) throws TasteException{
		// iterate through all items
				LongPrimitiveIterator it = dataModel.getItemIDs();
				while (it.hasNext()) {
					
					long itemID = it.next();

					// iterate through all prefs for item
					PreferenceArray prefs = dataModel.getPreferencesForItem(itemID);
					for (Preference p : prefs) {
						SMPreference pref = (SMPreference) p;
						if(pref.getValue(0) == 4){
							updateKernelEstimator(pref.getValue(1));
						}
					}
				}
	}
	
	public void updateKernelEstimator(float f){
		if(f <= MAXBIN && f >= BIN_SIZE) {
			int index = (int) Math.floor(f/BIN_SIZE);
			histograms[index]++;
		}
	}
	
	public int getMostDenseImplicit(){
		int max = -1;
		int index = -1;

		for (int i = 0; i < histograms.length; i++) {
			if(histograms[i] >= max){
				max = histograms[i];
				index = i;
			}
		}
//		System.out.println(String.format("Most dense implicit value is %d",(index+1)*BIN_SIZE));
		return (index + 1)*BIN_SIZE;
	}
}
