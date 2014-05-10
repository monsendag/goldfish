package edu.ntnu.idi.goldfish.preprocessors;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;

import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import edu.ntnu.idi.goldfish.mahout.SMPreference;
import weka.estimators.KernelEstimator;

public class KernelEstimatorHandler extends KernelEstimator{
	
	private double arithmeticMean = 0;

	public double getArithmeticMean() {
		return arithmeticMean;
	}

	public void setArithmeticMean(double arithmeticMean) {
		this.arithmeticMean = arithmeticMean;
	}

	public KernelEstimatorHandler(double precision, SMDataModel model) throws TasteException {
		super(precision);
		// iterate through all items
		LongPrimitiveIterator it = model.getItemIDs();
		int counter = 0;
		int sum = 0;
		while (it.hasNext()) {
			
			long itemID = it.next();
			
			// iterate through all prefs for item
			PreferenceArray prefs = model.getPreferencesForItem(itemID);
			for (Preference p : prefs) {
				SMPreference pref = (SMPreference) p;
				if(pref.getValue(0) == 4 && pref.getValue(1) < 50000){
					addValue(pref.getValue(1), 1);
					
					counter++;
					sum += pref.getValue(1);
				}
			}
		}
		
		double mean = sum/counter;
		setArithmeticMean(mean);
	}
	
	public double getHighestDensityValue(){
		double[] weights = getWeights();
		double probability = 0;
		
		double maxProb = 0;
		int kernelIndex = 0;
		
		for (int i = 0; i < weights.length; i++) {
			probability = getProbability(weights[i]);
			if(probability >= maxProb){
				maxProb = probability;
				kernelIndex = i;
			}
		}
		
		double[] means = getMeans();
		return means[kernelIndex];
	}
	
	public double meanShift(){
		return Math.abs(Math.sqrt(3)*getStdDev()-getArithmeticMean());
	}

}
