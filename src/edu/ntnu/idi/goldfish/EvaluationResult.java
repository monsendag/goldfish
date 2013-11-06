package edu.ntnu.idi.goldfish;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.mahout.cf.taste.eval.IRStatistics;

public class EvaluationResult {
	
	public static enum SortOption {
		RMSE, AAD, Precision, Recall
	}
	
	String recommender;
	
	// RMSE
	double RMSE;
	
	// 
	double AAD;
	
	// IR stats (precision, recall)
	IRStatistics irStats;
	
	
	public EvaluationResult(String recommender, double RMSE, double AAD, IRStatistics irStats) {
		this.recommender = recommender;

		// NaN is messing up sorting, so we overwrite it
		if(Double.isNaN(RMSE)) RMSE = 10;
		
		this.RMSE = RMSE;
		this.AAD = AAD;
		this.irStats = irStats;
	}
	
	public String toString() {
		return String.format("%-40s | RMSE: %6.3f | AAD: %6.3f | Precision: %6.3f | Recall %6.3f", recommender, RMSE, AAD, irStats.getPrecision(), irStats.getRecall());
	}
	
	public double getPrecision() {
		return irStats.getPrecision();
	}
	
	public double getRecall() {
		return irStats.getRecall();
	}
	
	
	public static void sortList(List<EvaluationResult> results, final EvaluationResult.SortOption sortOn) {
		Collections.sort(results, new Comparator<EvaluationResult>() {  
			public int compare(EvaluationResult self, EvaluationResult other) {
				double a =0, b = 0;
				switch(sortOn) {
					case RMSE: a = self.RMSE; b = other.RMSE; break;
					case AAD: a = self.AAD; b = other.AAD; break; 
					case Precision: a = self.getPrecision(); b = other.getPrecision(); break;
					case Recall: a = self.getRecall(); b = other.getRecall(); break;
				}
				return (a > b) ? -1 : (a < b) ? 1 : 0;
			}
		});
}

}
