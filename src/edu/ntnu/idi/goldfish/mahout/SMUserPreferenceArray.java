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

package edu.ntnu.idi.goldfish.mahout;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.common.iterator.CountingIterator;

/**
 * <p>
 * Like {@link SMItemPreferenceArray} but stores preferences for one user (all
 * user IDs the same) rather than one item.
 * </p>
 * 
 * <p>
 * This implementation maintains two parallel arrays, of item IDs and values.
 * The idea is to save allocating {@link Preference} objects themselves. This
 * saves the overhead of {@link Preference} objects but also duplicating the
 * user ID value.
 * </p>
 * 
 * @see BooleanUserPreferenceArray
 * @see SMItemPreferenceArray
 * @see GenericPreference
 */
public final class SMUserPreferenceArray implements SMPreferenceArray {

	private static final int ITEM = 1;
	private static final int VALUE = 2;
	private static final int VALUE_REVERSED = 3;

	private final long[] ids;
	private long id;
	private final float[][] values;

	public SMUserPreferenceArray(int size) {
		this.ids = new long[size];
		values = new float[size][SMPreference.NUM_VALUES];
		this.id = Long.MIN_VALUE; // as a sort of 'unspecified' value

	}

	public SMUserPreferenceArray(List<? extends Preference> prefs) {
		this(prefs.size());
		int size = prefs.size();
		long userID = Long.MIN_VALUE;
		for (int i = 0; i < size; i++) {
			SMPreference pref = (SMPreference) prefs.get(i);
			if (i == 0) {
				userID = pref.getUserID();
			} else {
				if (userID != pref.getUserID()) {
					throw new IllegalArgumentException(
							"Not all user IDs are the same");
				}
			}
			ids[i] = pref.getItemID();
			values[i] = pref.getValues();
		}
		id = userID;
	}

	/**
	 * This is a private copy constructor for clone().
	 */
	private SMUserPreferenceArray(long[] ids, long id, float[][] values) {
		this.ids = ids;
		this.id = id;
		this.values = values;
	}

	public int length() {
		return ids.length;
	}

	public SMPreference get(int i) {
		return new PreferenceView(i);
	}

	public void set(int i, Preference pref) {

	}

	public void set(int i, SMPreference pref) {

	}

	public long getUserID(int i) {
		return id;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Note that this method will actually set the user ID for <em>all</em>
	 * preferences.
	 */
	public void setUserID(int i, long userID) {
		id = userID;
	}

	public long getItemID(int i) {
		return ids[i];
	}

	public void setItemID(int i, long itemID) {
		ids[i] = itemID;
	}

	/**
	 * @return all item IDs
	 */
	public long[] getIDs() {
		return ids;
	}

	public float getValue(int i) {
		return SMPreference.combineValues(values[i]);
	}

	public void setValue(int i, float value) {
	}

	public void sortByUser() {
	}

	public void sortByItem() {
		lateralSort(ITEM);
	}

	public void sortByValue() {
		lateralSort(VALUE);
	}

	public void sortByValueReversed() {
		lateralSort(VALUE_REVERSED);
	}

	public boolean hasPrefWithUserID(long userID) {
		return id == userID;
	}

	public boolean hasPrefWithItemID(long itemID) {
		for (long id : ids) {
			if (itemID == id) {
				return true;
			}
		}
		return false;
	}

	private void lateralSort(int type) {
		// Comb sort: http://en.wikipedia.org/wiki/Comb_sort
		int length = length();
		int gap = length;
		boolean swapped = false;
		while (gap > 1 || swapped) {
			if (gap > 1) {
				gap /= 1.247330950103979; // = 1 / (1 - 1/e^phi)
			}
			swapped = false;
			int max = length - gap;
			for (int i = 0; i < max; i++) {
				int other = i + gap;
				if (isLess(other, i, type)) {
					swap(i, other);
					swapped = true;
				}
			}
		}
	}

	private boolean isLess(int i, int j, int type) {
		switch (type) {
		case ITEM:
			return ids[i] < ids[j];
		case VALUE:
			return getValue(j) < getValue(j);
		case VALUE_REVERSED:
			return getValue(j) > getValue(j);
		default:
			throw new IllegalStateException();
		}
	}

	private void swap(int i, int j) {
		long temp1 = ids[i];
		float[] temp2 = values[i];
		ids[i] = ids[j];
		values[i] = values[j];
		ids[j] = temp1;
		values[j] = temp2;
	}

	@Override
	public SMUserPreferenceArray clone() {
		return new SMUserPreferenceArray(ids.clone(), id, values.clone());
	}

	@Override
	public int hashCode() {
		return (int) (id >> 32) ^ (int) id ^ Arrays.hashCode(ids)
				^ Arrays.hashCode(values);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof SMUserPreferenceArray)) {
			return false;
		}
		SMUserPreferenceArray otherArray = (SMUserPreferenceArray) other;
		return id == otherArray.id && Arrays.equals(ids, otherArray.ids)
				&& Arrays.equals(values, otherArray.values);
	}

	public Iterator<Preference> iterator() {
		return Iterators.transform(new CountingIterator(length()),
				new Function<Integer, Preference>() {
					public SMPreference apply(Integer from) {
						return new PreferenceView(from);
					}
				});
	}

	public String toString() {
		if (ids == null || ids.length == 0) {
			return "GenericUserPreferenceArray[{}]";
		}
		StringBuilder result = new StringBuilder(20 * ids.length);
		result.append("GenericUserPreferenceArray[userID:");
		result.append(id);
		result.append(",{");
		for (int i = 0; i < ids.length; i++) {
			if (i > 0) {
				result.append(',');
			}
			result.append(ids[i]);
			result.append('=');
			result.append(values[i]);
		}
		result.append("}]");
		return result.toString();
	}

	private final class PreferenceView extends SMPreference {

		private final int i;

		private PreferenceView(int i) {
			this.i = i;
		}

		public float getValue(int j) {
			return values[i][j];
		}

		public void setValue(float value, int i) {
		}

		public float getValue() {
			return combineValues(values[i]);
		}

		public long getUserID() {
			return SMUserPreferenceArray.this.getUserID(i);
		}

		public long getItemID() {
			return SMUserPreferenceArray.this.getItemID(i);
		}

		public void setValue(float value) {
		}

		@Override
		public float[] getValues() {
			return values[i];
		}

	}

}