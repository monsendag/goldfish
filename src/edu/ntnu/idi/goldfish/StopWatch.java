package edu.ntnu.idi.goldfish;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class StopWatch {
	
	protected static HashMap<String, Long> starts = new HashMap<String,Long>();
	protected static HashMap<String, Long> timings = new HashMap<String,Long>();
	
	
	public static void start(String name) {
		timings.remove(name);
		starts.put(name, System.currentTimeMillis());
	}
	
	public static void stop(String name) {
		if(!starts.containsKey(name)) {
			System.err.format("have no start timing for %s\n", name);
			return;
		}
		if(!timings.containsKey(name)) {
			timings.put(name, System.currentTimeMillis() - starts.get(name));			
		}
	}
	
	public static String str(String name) {
		stop(name);
		long time = timings.get(name);
		return String.format("%d min, %d sec", 
			    TimeUnit.MILLISECONDS.toMinutes(time),
			    TimeUnit.MILLISECONDS.toSeconds(time) - 
			    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
		);
	}
	
	public static void print(String name) {
		System.out.format("%s: %s\n", name, str(name));
	}
	
	public static long get(String name) {
		stop(name);
		return timings.get(name);
	}
}
