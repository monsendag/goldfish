package edu.ntnu.idi.goldfish;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Columns {
    Set<String> cols;

    private LinkedHashMap<String, String> printFormats;
    private LinkedHashMap<String, String> saveFormats;

	public Columns() {
        cols = new HashSet<>();
        printFormats = new LinkedHashMap<>();
        saveFormats = new LinkedHashMap<>();
    }

    public void add(String name, String printFormat, String saveFormat) {
        cols.add(name);
        printFormats.put(name, printFormat);
        saveFormats.put(name, saveFormat);
    }

    public Map<String, String> getPrintFormats() {
        return printFormats;
    }

    public Map<String, String> getSaveFormats() {
        return saveFormats;
    }

}
