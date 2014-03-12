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

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.common.iterator.CountingIterator;

import java.util.Arrays;
import java.util.Iterator;

/**
 * An alternate representation of an array of {@link Preference}. Implementations, in theory, can produce a
 * more memory-efficient representation.
 */
public abstract class SMPreferenceArray implements PreferenceArray {

    protected static final int USER = 0;
    protected static final int ITEM = 1;
    protected static final int VALUE = 2;
    protected static final int VALUE_REVERSED = 3;
    protected final float[][] values;
    protected final long[] ids;
    protected long id;

    public SMPreferenceArray(int size) {
        this.ids = new long[size];
        values = new float[size][SMPreference.NUM_VALUES];
        this.id = Long.MIN_VALUE; // as a sort of 'unspecified' value
    }

    protected SMPreferenceArray(long[] ids, long id, float[][] values) {
        this.ids = ids;
        this.id = id;
        this.values = values;
    }

    public int length() {
        return ids.length;
    }

    public void set(int i, Preference pref) {
        System.err.println("Cannot set single value on SMPreferenceArray");
    }


    public SMPreference get(int i) {
        return new PreferenceView(i);
    }

    public float getValue(int i) {
        System.err.println("Cannot get single value from SMPreferenceArray");
        return -1f;
    }

    public long[] getIDs() {
        return ids;
    }

    public Iterator<Preference> iterator() {
        return Iterators.transform(new CountingIterator(length()),
                new Function<Integer, Preference>() {
                    public Preference apply(Integer from) {
                        return new PreferenceView(from);
                    }
                });
    }

    protected void lateralSort(int type) {
        //Comb sort: http://en.wikipedia.org/wiki/Comb_sort
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

    public void sortByItem() {
        lateralSort(ITEM);
    }

    public void sortByUser() {
        lateralSort(USER);
    }

    public void sortByValue() {
        lateralSort(VALUE);
    }

    public void sortByValueReversed() {
        lateralSort(VALUE_REVERSED);
    }

    private boolean isLess(int i, int j, int type) {
        switch (type) {
            case USER:
                return ids[i] < ids[j];
            case VALUE:
                return getValue(j) < getValue(j);
            case VALUE_REVERSED:
                return getValue(j) > getValue(j);
            default:
                throw new IllegalStateException();
        }
    }

    protected void swap(int i, int j) {
        long temp1 = ids[i];
        float[] temp2 = values[i];
        ids[i] = ids[j];
        values[i] = values[j];
        ids[j] = temp1;
        values[j] = temp2;
    }

    public abstract SMPreferenceArray clone();


    @Override
    public int hashCode() {
        return (int) (id >> 32) ^ (int) id ^ Arrays.hashCode(ids) ^ Arrays.hashCode(values);
    }

    protected final class PreferenceView extends SMPreference {

        private final int i;

        protected PreferenceView(int i) {
            this.i = i;
        }

        public long getUserID() {
            return SMPreferenceArray.this.getUserID(i);
        }

        public long getItemID() {
            return SMPreferenceArray.this.getItemID(i);
        }

        @Override
        public void setValue(float value, int j) {
            values[i][j] = value;
        }

        public float getValue(int j) {
            return values[i][j];
        }

        @Override
        public float[] getValues() {
            return values[i];
        }

    }

}
