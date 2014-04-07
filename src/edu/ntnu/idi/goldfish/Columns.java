package edu.ntnu.idi.goldfish;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class Columns extends LinkedHashMap<String, String> {

	List<String> properties;
	List<String> formats;
	
	public Columns() {
		properties = new ArrayList<>();
		formats = new ArrayList<>();
	}
	

	public String join(String delimiter) {
		String fs = StringUtils.join(properties.toArray(),delimiter);
		return String.format(fs, formats.toArray());
	}

    public static Columns getPrintFormats(String... properties) {

        Columns columns = new Columns();
        columns.put("name", "%-11s");
        columns.put("average", "%4d");
        columns.put("similarity", "%19s");
        columns.put("numFeatures", "%5.2f");
        columns.put("TopN", "%3d");
        columns.put("RMSE", "%6.3f");
        columns.put("AAD", "%6.3f");
        columns.put("precision", "%6.3f");
        columns.put("recall", "%6.3f");
        columns.put("buildTime", "%4.0f");
        columns.put("recTime", "%2.0f");
        columns.put("evalTime", "%4.0f");
        columns.put("minTimeOnPage", "%5d");
        columns.put("correlationLimit", "%2.1f");
        columns.put("predictionMethod", "%16s");
        columns.put("clusterer", "%13s");
        columns.put("clusterDataset", "%18s");
        columns.put("numberOfIndependentVariables", "%1d");
        columns.put("classifier", "%16s");

        // filter by arguments
        columns.keySet().retainAll(Arrays.asList(properties));

        return columns;
    }

    public static Columns getSaveFormats(String... properties) {
        Columns columns = new Columns();
        columns.put("name", "%s");
        columns.put("average", "%d");
        columns.put("similarity", "%s");
        columns.put("numFeatures", "%.2f");
        columns.put("TopN", "%d");
        columns.put("RMSE", "%.3f");
        columns.put("AAD", "%.3f");
        columns.put("precision", "%.3f");
        columns.put("recall", "%.3f");
        columns.put("buildTime", "%.0f");
        columns.put("recTime", "%.0f");
        columns.put("evalTime", "%0.f");
        columns.put("minTimeOnPage", "%d");
        columns.put("correlationLimit", "%.1f");
        columns.put("predictionMethod", "%s");
        columns.put("clusterer", "%s");
        columns.put("clusterDataset", "%s");
        columns.put("numberOfIndependentVariables", "%d");
        columns.put("classifier", "%s");

        // filter by arguments
        columns.keySet().retainAll(Arrays.asList(properties));

        return columns;
    }
}
