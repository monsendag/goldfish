package edu.ntnu.idi.goldfish.mahout;

import java.io.Serializable;

import org.apache.mahout.cf.taste.model.Preference;

import com.google.common.base.Preconditions;

/**
 * <p>
 * A simple {@link Preference} encapsulating an item and preference value.
 * </p>
 */
public abstract class SMPreference implements Preference, Serializable {
  
	public static final int NUM_VALUES = 2;
    public static float[] weights = {1, 2.5f};  // needs to be reflected in Matlab code (Kiwi.m)
	
    public abstract void setValue(float value, int i);
    
    public abstract float getValue(int i);
    
    public abstract float[] getValues();
    
	public static float combineValues(float[] values) {
		Preconditions.checkArgument(values.length == weights.length, "Values and weights arrays are of different size");
		float result = 0;
		for(int i=0; i<values.length; i++) {
			result += values[i] * weights[i];
		}
		return result;
	}
    
}
