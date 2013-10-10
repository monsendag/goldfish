package edu.ntnu.idi;

import java.io.File;
import java.io.IOException;

import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;

public class Main {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		FileDataModel movielens = new FileDataModel(new File("data/movielens.dat"));
		System.out.println("koko");
	}
}
