package edu.ntnu.idi.goldfish.mahout;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Like {@link GenericUserPreferenceArray} but stores preferences for one item (all item IDs the same) rather
 * than one user.
 * </p>
 * 
 * @see BooleanItemPreferenceArray
 * @see GenericUserPreferenceArray
 * @see GenericPreference
 */
public final class SMItemPreferenceArray extends SMPreferenceArray {

  public SMItemPreferenceArray(int size) {
      super(size);
  }

  public SMItemPreferenceArray(List<? extends SMPreference> prefs) {
    this(prefs.size());
    int size = prefs.size();
    long itemID = Long.MIN_VALUE;
    for (int i = 0; i < size; i++) {
      SMPreference pref = prefs.get(i);
      ids[i] = pref.getUserID();
      if (i == 0) {
        itemID = pref.getItemID();
      } else {
        if (itemID != pref.getItemID()) {
          throw new IllegalArgumentException("Not all item IDs are the same");
        }
      }
      values[i] = pref.getValues();
    }
    id = itemID;
  }


    private SMItemPreferenceArray(long[] ids, long id, float[][] values) {
        super(ids, id, values);
    }

  
  public void set(int i, SMPreference pref) {
      id = pref.getItemID();
      ids[i] = pref.getUserID();
      values[i] = pref.getValues();
  }

  public long getUserID(int i) {
    return ids[i];
  }

  public void setUserID(int i, long userID) {
    ids[i] = userID;
  }

  public long getItemID(int i) {
    return id;
  }

  public void setItemID(int i, long itemID) {
    id = itemID;
  }

  public float getValue(int i) {
    return SMPreference.combineValues(values[i]);
  }

  public void setValue(int i, float value) {
  }

  public boolean hasPrefWithUserID(long userID) {
    for (long id : ids) {
      if (userID == id) {
        return true;
      }
    }
    return false;
  }

  protected boolean isLess(int i, int j, int type) {
      return type == USER ? ids[i] < ids[j] : super.isLess(i,j, type);
  }


  public boolean hasPrefWithItemID(long itemID) {
    return id == itemID;
  }

  public SMItemPreferenceArray clone() {
    return new SMItemPreferenceArray(ids.clone(), id, values.clone());
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof SMItemPreferenceArray)) {
      return false;
    }
    SMItemPreferenceArray otherArray = (SMItemPreferenceArray) other;
    return id == otherArray.id && Arrays.equals(ids, otherArray.ids) && Arrays.equals(values, otherArray.values);
  }



}
