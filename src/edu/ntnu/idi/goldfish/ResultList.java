package edu.ntnu.idi.goldfish;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;

public class ResultList extends ArrayList<Result> {

    Columns columns;

	public ResultList() {

	}

    public void setColumns(String... properties) {
        columns = Columns.getPrintFormats(properties);
    }

    public void print() {
        forEach(r -> System.out.println(r.toString(columns)));
    }

    public void printSummary() {
        System.out.println(getTotal().toString(columns));
        System.out.println(getAverage().toString(columns));
    }



	public void print(String... properties) {
        Columns columns = Columns.getPrintFormats(properties);
        forEach(r -> System.out.println(r.toString(columns)));
    }

    public void save() throws IOException {
        save(columns, "", "");
    }

    public void save(String... properties) throws IOException {
        Columns columns = Columns.getSaveFormats(properties);
        save(columns, "", "");
    }
	
	public String toTSV(Columns columns) {
		String out = "";
		out += StringUtils.join(columns.keySet(), "\t") +"\n";
        out += getTotal()+"\n";
        out += getAverage()+"\n";
		for (Result res : this) {
			out += res.toTSV(columns)+"\n";
		}
		return out;
	}
	

	public void save(Columns columns, String prependToFile, String appendToFileName) throws IOException {
        // prepend "-" to append if it's not empty
        appendToFileName = appendToFileName.length() > 0 ? "-"+appendToFileName : "";

        String dateTime = String.format("%1$tY-%1$tm-%1$td-%1$tH%1$tM%1$tS", new Date());

        String output = prependToFile + toTSV(columns);
        String fileName = String.format("results/%s-%s%s.tsv", dateTime, Main.set.toString(), appendToFileName);
        File file = new File(fileName);
        Writer writer = new BufferedWriter(new FileWriter(file));
        writer.write(output);
        System.out.println("Saved to file: " + fileName);
    }

    public Result getTotal() {
        String[] properties = new String[]{"RMSE", "AAD", "precision", "recall", "buildTime", "recTime", "evalTime"};

        // create result object
        Result total = new Result().set("name", "# TOTAL");

        // ensure we have a few results
        if(size() == 0) return total;

        // for each property, calculate totals and set them on total object
        for(String property : properties) {
            Result first = get(0);
            if (first.containsKey(property)) {
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
        String[] properties = new String[]{"RMSE", "AAD", "precision", "recall", "buildTime", "recTime", "evalTime"};
        Result total = getTotal();
        Result average = new Result().set("name", "# AVERAGE");
        int N = size();

        for(String property : properties) {
            if(total.containsKey(property)) {
                // the returned type can be Double or Long, so we need to check in order to cast to the right primitive
                // too bad java generics doesn't support primitives
                Number sum = total.get(property);
                average.set(property, (sum.doubleValue() / N));
            }

        }
        return average;
    }

}
