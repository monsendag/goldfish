package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.configurations.Config;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Result extends HashMap<String, Object> {

    Config config;

    public Result() {

    }

    public Result(Config config) {
        this.config = config;
    }

    public Result set(String prop, Object val) {
        super.put(prop, val);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String val) {
        return containsKey(val) ? (T) super.get(val) : config!=null ? config.get(val) : null;
    }

	public String toString(Columns columns) {
        List<String> values = columns.keySet().stream().map(col ->
            String.format("%s: " + columns.get(col), col, get(col))
        ).collect(Collectors.toList());
        return StringUtils.join(values, " | ");
	}
	
	public String toTSV(Columns columns) {
        List<String> values = new ArrayList<>();
        for(String col : columns.keySet()) {
            values.add(String.format(columns.get(col), (Object)get(col)));
        }
		return StringUtils.join(values, "\t");
	}
}
