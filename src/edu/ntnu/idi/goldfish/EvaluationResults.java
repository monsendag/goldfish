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

public class EvaluationResults extends ArrayList<Result> {
	private static final long serialVersionUID = 7410339309503861432L;

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
					case Precision: a = self.getPrecision(); b = other.getPrecision(); break;
					case Recall: a = self.getRecall(); b = other.getRecall(); break;
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
	
	public String toCSV(SortOption sortOn) {
		sortOn(sortOn);
		return toCSV();
		
	}
	
	public String toCSV() {
		String out = "";
                out += "Recommender,RMSE,AAD,Precision,Recall,Build time,Rec time\n";
		for (Result res : this) {
			out += res.toCSV()+"\n";
		}
		return out;
	}
	
	public void save() {
		Writer writer = null;
		 
        try {
        	String datetime = String.format("%1$tY-%1$tm-%1$td-%1$tH%1$tM%1$tS", new Date());
        	String output = toCSV();
            
            File file = new File("results/"+datetime+".csv");
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(output);
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
