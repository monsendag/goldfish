package edu.ntnu.idi.goldfish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class Columns extends LinkedHashMap<String, String> {

    List<String> cols;

    private static Columns printFormats = new Columns();
    private static Columns saveFormats = new Columns();


	public Columns() {
        cols = new ArrayList<>();
	}

    public Columns(Columns cols) {
        super(cols);
    }

    public void add(String col) {
        cols.add(col);
    }

    public void add(String... cols) {
        Collections.addAll(this.cols, cols);
    }

    public Columns getPrintFormats() {
        Columns printFormats = new Columns(Columns.printFormats);
        printFormats.keySet().retainAll(cols);
        return printFormats;
    }

    public Columns getSaveFormats() {
        Columns saveFormats = new Columns(Columns.saveFormats);
        saveFormats.keySet().retainAll(cols);

        return saveFormats;
    }

    static {
        printFormats.put("name", "%-11s");
        printFormats.put("average", "%4.0f");
        printFormats.put("similarity", "%19s");
        printFormats.put("numFeatures", "%5.2f");
        printFormats.put("TopN", "%3d");
        printFormats.put("RMSE", "%6.3f");
        printFormats.put("AAD", "%6.3f");
        printFormats.put("precision", "%6.3f");
        printFormats.put("recall", "%6.3f");
        printFormats.put("buildTime", "%4.0f");
        printFormats.put("recTime", "%2.0f");
        printFormats.put("evalTime", "%4.0f");
        printFormats.put("minTimeOnPage", "%5d");
        printFormats.put("correlationLimit", "%2.1f");
        printFormats.put("predictionMethod", "%16s");
        printFormats.put("clusterer", "%13s");
        printFormats.put("clusterDataset", "%18s");
        printFormats.put("distFunc", "%9s");
        printFormats.put("IVs", "%1d");
        printFormats.put("kernel", "%20s");
        printFormats.put("C", "%20s");
        printFormats.put("kernelGamma", "%6.3f");
        printFormats.put("kernelExponent", "%6.3f");

        saveFormats.put("name", "%s");
        saveFormats.put("average", "%.0f");
        saveFormats.put("similarity", "%s");
        saveFormats.put("numFeatures", "%.2f");
        saveFormats.put("TopN", "%d");
        saveFormats.put("RMSE", "%.3f");
        saveFormats.put("AAD", "%.3f");
        saveFormats.put("precision", "%.3f");
        saveFormats.put("recall", "%.3f");
        saveFormats.put("buildTime", "%.0f");
        saveFormats.put("recTime", "%.0f");
        saveFormats.put("evalTime", "%.0f");
        saveFormats.put("minTimeOnPage", "%d");
        saveFormats.put("correlationLimit", "%.1f");
        saveFormats.put("predictionMethod", "%s");
        saveFormats.put("clusterer", "%s");
        saveFormats.put("clusterDataset", "%s");
        printFormats.put("distFunc", "%s");
        saveFormats.put("IVs", "%d");
        saveFormats.put("kernel", "%s");
        printFormats.put("C", "%s");
        printFormats.put("kernelGamma", "%.4f");
        printFormats.put("kernelExponent", "%.4f");
    }


}
