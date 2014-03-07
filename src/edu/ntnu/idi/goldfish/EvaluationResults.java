package edu.ntnu.idi.goldfish;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;

import edu.ntnu.idi.goldfish.Main.DataSet;

public class EvaluationResults extends ArrayList<Result> {
	private static final long serialVersionUID = 7410339309503861432L;

	private DataModel model;
	
	public EvaluationResults(DataModel model) {
		this.model = model;
	}
	
	public static enum SortOption {
		RMSE, AAD, Precision, Recall
	}
	
	public void sortOn(final SortOption option) {
		Collections.sort(this, new Comparator<Result>() {  
			public int compare(Result self, Result other) {
				double a =0, b = 0;
				switch(option) {
					case RMSE: a = self.RMSE; b = other.RMSE; break;
					case AAD: a = self.AAD; b = other.AAD; break; 
					case Precision: a = self.precision; b = other.precision; break;
					case Recall: a = self.recall; b = other.recall; break;
				}
				return (a > b) ? -1 : (a < b) ? 1 : 0;
			}
		});
	}
	
	public void print(SortOption sortOn) {
		sortOn(sortOn);
		print();
	}
	
	public void print() {
		for (Result res : this) {
			System.out.println(res);
		}
	}
	
	public String toTSV(SortOption sortOn) {
		sortOn(sortOn);
		return toTSV();
		
	}
	
	public String toTSV() {
		
		String out = "";
		String[] headers = {"Recommender", "Similarity", "KTL", "TopN", "RMSE", "AAD", "Precision", "Recall", "Build time", "Rec time", "Eval time"};

		out += StringUtils.join(headers,"\t") +"\n";
		for (Result res : this) {
			out += res.toTSV()+"\n";
		}
		out += Result.getTotal(this)+"\n";
		return out;
	}
	
	public void save() {
		try {
			String prepend = ""; 
			Result total = Result.getTotal(this);
			prepend += String.format("# Dataset: %s  (%d users, %d items) \n", Main.set.toString(), model.getNumUsers(), model.getNumItems());
			prepend += String.format("# Evaluated %d configurations in %s \n", size(), StopWatch.getString(total.evalTime));
			save(prepend, "");
		} catch (TasteException e) {
			e.printStackTrace();
		}
	}
	public void save(String prependToFile, String appendToFileName) {
		Writer writer = null;
		
		// prepend "-" to append if it's not empty
		appendToFileName = appendToFileName.length() > 0 ? "-"+appendToFileName : "";
		 
        try {
        	String dateTime = String.format("%1$tY-%1$tm-%1$td-%1$tH%1$tM%1$tS", new Date());
        	
        	String output = prependToFile + toTSV();
            String fileName = String.format("results/%s-%s%s.tsv", dateTime, Main.set.toString(), appendToFileName);
            File file = new File(fileName);
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(output);
            System.out.println("Saved to file: "+fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
    }

}
