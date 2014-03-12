package edu.ntnu.idi.goldfish;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Formatter {

	List<String> formats; 
	List<Object> values;
	
	public Formatter() {
		formats = new ArrayList<String>();
		values = new ArrayList<Object>();
	}
	
	public void put(String key, Object value) {
		formats.add(key);
		values.add(value);
	}
	
	public String join(String delimiter) {
		String fs = StringUtils.join(formats.toArray(),delimiter);
		return String.format(fs, values.toArray());
	}
	
	
}
