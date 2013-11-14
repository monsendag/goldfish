package edu.ntnu.idi.goldfish;

import java.util.concurrent.TimeUnit;

import org.apache.mahout.cf.taste.eval.IRStatistics;


public class Result {

	RecommenderWrapper recommender;
	
	// RMSE
	double RMSE;
	
	// AAD/MAE 
	double AAD;
	
	// IR stats (precision, recall)
	IRStatistics irStats;
	
	// model build timing
	long buildTime;
	
	// recommendation timing
	long recTime;
	
	public Result(RecommenderWrapper recommender, double RMSE, double AAD, IRStatistics irStats, long buildTime, long recTime) {
		this.recommender = recommender;

		// NaN is messing up sorting, so we overwrite it
		if(Double.isNaN(RMSE)) RMSE = 10;
		
		this.RMSE = RMSE;
		this.AAD = AAD;
		this.irStats = irStats;
		
		this.buildTime = buildTime;
		this.recTime = recTime;
	}
	
	
	public String toString() {
		return String.format("%-40s | RMSE: %6.3f | AAD: %6.3f | Precision: %6.3f | Recall %6.3f | Build time %7d | Rec time %7d", 
				recommender, RMSE, AAD, getPrecision(), getRecall(), getBuildTime(), getRecTime());
	}
	
	public String toCSV() {
		return String.format("%s,%.3f,%.3f,%.3f,%.3f,%d,%d", 
				recommender.toString(true), RMSE, AAD, getPrecision(), getRecall(), getBuildTime(), getRecTime());
	}
	
	public long getBuildTime() {
		return buildTime;
		//return TimeUnit.MILLISECONDS.convert(buildTime, TimeUnit.NANOSECONDS);
	}
	
	public long getRecTime() {
		return recTime;
//		return TimeUnit.MILLISECONDS.convert(recTime, TimeUnit.NANOSECONDS);
	}
	
	public double getPrecision() {
		return irStats.getPrecision();
	}
	
	public double getRecall() {
		return irStats.getRecall();
	}

}
