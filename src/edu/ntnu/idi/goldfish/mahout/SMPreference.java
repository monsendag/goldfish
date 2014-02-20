package edu.ntnu.idi.goldfish.mahout;

import java.io.Serializable;

import org.apache.mahout.cf.taste.model.Preference;

import com.google.common.base.Preconditions;

/**
 * <p>
 * A simple {@link Preference} encapsulating an item and preference value.
 * </p>
 */
public interface SMPreference extends Preference, Serializable {
  
    
    public void setValue(float value, int i);
    
    public float getValue(int i);
    
}
