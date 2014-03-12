package edu.ntnu.idi.goldfish.mahout;

import org.apache.mahout.cf.taste.model.Preference;

import java.io.Serializable;

/**
 * <p>
 * A simple {@link Preference} encapsulating an item and preference value.
 * </p>
 */
public abstract class SMPreference implements Preference, Serializable {
  
	public static float defaultWeight = 0; 
	public static float[] customWeights = {1, 0};
	public static int NUM_VALUES = -1;
	
    public abstract void setValue(float value, int i);
    
    public abstract float getValue(int i);
    
    public abstract float[] getValues();
    
	public static float combineValues(float[] values) {
		float result = 0;
		for(int i=0; i<values.length; i++) {
			// use custom weights when available, otherwise default
			result += i >= customWeights.length ? defaultWeight : values[i] * customWeights[i];
		}
		return result;
	}
}
