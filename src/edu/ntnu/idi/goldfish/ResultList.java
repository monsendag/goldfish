package edu.ntnu.idi.goldfish;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class ResultList extends ArrayList<Result> {


	public ResultList() {

	}

    public synchronized boolean add(Result res) {
        return super.add(res);
    }

    public void printSummary(Map<String, String> columns) {
        System.out.println(getTotal().toString(columns));
        System.out.println(getAverage().toString(columns));
    }

	public void print(Map<String, String> columns) {
        forEach(r -> System.out.println(r.toString(columns)));
    }

	public String toTSV(Map<String, String> columns) {
		String out = "";
		out += StringUtils.join(columns.keySet(), "\t") +"\n";
        out += getTotal().toTSV(columns)+"\n";
        out += getAverage().toTSV(columns)+"\n";
		for (Result res : this) {
			out += res.toTSV(columns)+"\n";
		}
        return out;
	}

    public void save(Map<String, String> columns) throws IOException {
        String output = toTSV(columns);
        String dateTime = String.format("%1$tY-%1$tm-%1$td-%1$tH%1$tM%1$tS", new Date());
        String fileName = String.format("results/%s-%s%s.tsv", dateTime, "", "");
        File file = new File(fileName);
        Writer writer = new BufferedWriter(new FileWriter(file));
        writer.write(output);
        writer.close();
        System.out.println("Saved to file: " + fileName);
    }

    public Result getTotal() {
        // list of properties relevant to calculate totals of
        String[] properties = new String[]{"RMSE", "AAD", "precision", "recall", "buildTime", "recTime", "evalTime"};

        // create result object
        Result total = new Result().set("name", "# TOTAL");

        // ensure we have a few results
        if(size() == 0) return total;

        // for each property, calculate totals and set them on total object
        for(String property : properties) {
            Result first = get(0);
            if (first.has(property)) {
                double sum = 0;
                for (Result res : this) {
                    Number value = res.get(property);
                    sum += value.doubleValue();
                }
                total.set(property, sum);
            }
        }
        return total;
    }

    public Result getAverage() {
        // list of properties relevant to calculate averages of
        String[] properties = new String[]{"RMSE", "AAD", "precision", "recall", "buildTime", "recTime", "evalTime"};
        Result total = getTotal();
        Result average = new Result().set("name", "# AVERAGE");
        int N = size();

        for(String property : properties) {
            if(total.has(property)) {
                // the returned type can be Double or Long, so we need to check in order to cast to the right primitive
                // too bad java generics doesn't support primitives
                Number sum = total.get(property);
                average.set(property, (sum.doubleValue() / N));
            }

        }
        return average;
    }

}
