package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.configurations.Config;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Result extends HashMap<String, Object> {


    public Result() {

    }

    public Result set(String prop, Object val) {
        super.put(prop, val);
        return this;
    }

    public boolean has(String prop) {
        return containsKey(prop) || (containsKey("config") && ((Config) get("config")).containsKey(prop));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String prop) {
        return containsKey(prop) ? (T) super.get(prop) : containsKey("config") ? ((Config) get("config")).get(prop) : null;
    }

    public Result remove(String prop) {
        super.remove(prop);
        return this;
    }

	public String toString(Map<String, String> columns) {
        List<String> values = columns.keySet().stream().map(col ->
                has(col) ? String.format("%s: " + columns.get(col), col, get(col)) : "-")
        .collect(Collectors.toList());
        return StringUtils.join(values, " | ");
	}
	
	public String toTSV(Map<String, String> columns) {
        List<String> values = columns.keySet().stream().map(col ->
                has(col) ? String.format("%s" + columns.get(col), "", get(col)) : "null")
        .collect(Collectors.toList());
        return StringUtils.join(values, "\t");
	}
}
