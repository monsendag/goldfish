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

import org.apache.mahout.cf.taste.model.Preference;

import java.util.Arrays;
import java.util.List;


public final class SMUserPreferenceArray extends SMPreferenceArray {

	public SMUserPreferenceArray(int size) {
        super(size);

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

    protected SMUserPreferenceArray(long[] ids, long id, float[][] values) {
        super(ids, id, values);
    }

	public void set(int i, SMPreference pref) {
        id = pref.getUserID();
        ids[i] = pref.getUserID();
        values[i] = pref.getValues();
	}

	public long getUserID(int i) {
		return id;
	}

	public void setUserID(int i, long userID) {
		id = userID;
	}

	public long getItemID(int i) {
		return ids[i];
	}

	public void setItemID(int i, long itemID) {
		ids[i] = itemID;
	}

	public void setValue(int i, float value) {

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

    protected boolean isLess(int i, int j, int type) {
        return type == ITEM ? ids[i] < ids[j] : super.isLess(i,j, type);
    }

	public SMUserPreferenceArray clone() {
		return new SMUserPreferenceArray(ids.clone(), id, values.clone());
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
}
