package edu.ntnu.idi.goldfish.mahout;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.Serializable;

import org.apache.mahout.cf.taste.model.Preference;

import com.google.common.base.Preconditions;

/**
 * <p>
 * A simple {@link Preference} encapsulating an item and preference value.
 * </p>
 */
public class GenericSMPreference implements SMPreference {
  
  private final long userID;
  private final long itemID;
  private float[] values;
  
  public GenericSMPreference(long userID, long itemID, float[] values) {
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
  
  public float getValue() {
    float sum = 0;
    for(float f : values) {
        sum += f;
    }
    return sum;
  }
  
  public void setValue(float value) {
    
  }
  
  @Override
  public String toString() {
    return "GenericSMPreference[userID: " + userID + ", itemID:" + itemID + ", values:" + values.toString() + ']';
  }

  public void setValue(float value, int i) {
    values[i] = value;
    
  }

  public float getValue(int i) {
    return values[i];
  }
  
}
