package edu.ntnu.idi.goldfish.mahout;

import java.io.Serializable;

import org.apache.mahout.cf.taste.model.Preference;

import com.google.common.base.Preconditions;

/**
 * <p>
 * A simple {@link Preference} encapsulating an item and preference value.
 * </p>
 */
public class SMPreference implements Preference, Serializable {
  
  private final long userID;
  private final long itemID;
  private float[] values;
  
  public SMPreference(long userID, long itemID, float[] values) {
    this.userID = userID;
    this.itemID = itemID;
    this.values = values;
  }
  
  public long getUserID() {
    return userID;
  }
  
  public long getItemID() {
    return itemID;
  }
  
  public float getValue(int i) {
	  return values[i];
  }
  
  public void setValue(float value, int i) {
    Preconditions.checkArgument(!Float.isNaN(value), "NaN value");
    this.values[i] = value;
  }
  
  public float getValue() {
	float sum = 0;
	for(float f : values) {
		sum += f;
	}
	return sum;
  }

	public void setValue(float value) {
		
	}
  
  
  public String toString() {
    return "SMPreference[userID: " + userID + ", itemID:" + itemID + ", value:" + values + ']';
  }


  
}
