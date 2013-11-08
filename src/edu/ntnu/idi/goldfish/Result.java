package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.eval.IRStatistics;

public class Result {

	String recommender;
	
	// RMSE
	double RMSE;
	
	// 
	double AAD;
	
	// IR stats (precision, recall)
	IRStatistics irStats;
	
	
	public Result(String recommender, double RMSE, double AAD, IRStatistics irStats) {
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
	
	public String toCSV() {
		return String.format("%s,%.3f,%.3f,%.3f,%.3f", recommender, RMSE, AAD, irStats.getPrecision(), irStats.getRecall());
	}
	
	public double getPrecision() {
		return irStats.getPrecision();
	}
	
	public double getRecall() {
		return irStats.getRecall();
	}

}
