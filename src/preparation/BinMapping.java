package preparation;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Create bins HH size and area of cultivated land
 * 
 * Use the mean of the top and bottom bound of each dimension (ceiling, rounded
 * to Integer) to characterize the HH size
 * 
 * Output is a mapping from the value of the area of cultivated land to the
 * value of HH size
 * 
 * @author Peng
 * 
 */
public class BinMapping {
	// Path to HH attribute file
	public static String HH_AttributeFile;
	// create separate bins for ward that has
	// number of HHs larger than baseline, otherwise create bins using the
	// entire sample
	public static int baseline = 20;

	// number of breaks used in creating bins
	public static int numRanges = 5;

	// name of ward id column
	public static String ward_colName;

	// name of area of cultivated land column
	public static String numericKey;
	// bin bounds
	public static double[] keyBreaks;

	// name of column that has numeric values
	public static String numericValue;
	public static double[] valueBreaks;

	/**
	 * Output: actual number of HHs created;
	 */
	public static int determineNumOfHHs(int totalPopulation, int population,
			int totalNumHHs) {
		return (int) (population * 1.0 / totalPopulation * totalNumHHs);
	}
	
	/**
	 * Function that calculate HHLabor from HHSize
	 */
	public static int determineHHLabor (int HHSize) {
		if (HHSize == 1) {
			return 1;
		} else if (HHSize >= 2 && HHSize <= 4) {
			return 2;
		} else if (HHSize == 5) {
			return 3;
		} else if (HHSize == 6) {
			return 4;
		} else if (HHSize >= 7 && HHSize <= 9) {
			return 5;
		} else if (HHSize >= 10 && HHSize <= 12) {
			return 6;
		} else { // HHSize >= 13
			return 7;
		}
	}

	/**
	 * @return mappings
	 * @throws IOException
	 */
	public static TreeMap<Double, List<SingleValueBin>> createGlobalBinMapping()
			throws IOException {
		// csv file containing data
		CSVReader reader = new CSVReader(new FileReader(HH_AttributeFile));
		List<String[]> all_records = reader.readAll();
		reader.close();

		Map<String, Integer> colIndex = new HashMap<String, Integer>();
		int index = 0;
		for (String str : all_records.get(0)) {
			colIndex.put(str, index++);
		}

		if (!colIndex.containsKey(ward_colName)) {
			System.out.println("Attribute csv doesn't have ward column!");
			System.exit(-1);
		}

		/*
		 * Create matrix for numeric attributes
		 */
		String[] allNumeric = { numericKey, numericValue };
		int[] numericIndexs = new int[allNumeric.length];
		for (int i = 0; i < allNumeric.length; i++) {
			if (!colIndex.containsKey(allNumeric[i])) {
				System.out.println("Numeric value " + allNumeric[i]
						+ " doesn't exist!");
				System.exit(-1);
			} else {
				numericIndexs[i] = colIndex.get(allNumeric[i]);
			}
		}

		double[][] numericAttributes_all = new double[all_records.size() - 1][numericIndexs.length]; // the
																										// first
																										// record
																										// is
																										// header
		Iterator<String[]> itr_all = all_records.iterator();
		itr_all.next();// drop header

		int row = 0, col = 0;
		while (itr_all.hasNext()) {
			col = 0;
			String[] line = itr_all.next();
			for (int i : numericIndexs) {
				numericAttributes_all[row][col++] = Double.parseDouble(line[i]);
			}

			row++;
		}

		double[][] brks = {keyBreaks, valueBreaks};
		System.out.println("Breaks:");
		for (int i = 0; i < brks.length; i++) {
			for (int j = 0; j < brks[i].length; j++) {
				System.out.print(brks[i][j] + "\t");
			}
			System.out.println("");
		}
		
		/*
		 * We create the mapping from the characterized attribute for each bin
		 * (using mean here)
		 */
		// Number of bins is decided by numBreaks ^ numericAttributes
		int numBins = (int) Math
				.pow(numRanges, numericAttributes_all[0].length);

		int[] bin_counts = new int[numBins];
		/*
		 * assign HHs to bins, i.e. count how many HHs are in each bin
		 */
		for (int i = 0; i < numericAttributes_all.length; i++) {
			// this value indicates the index of numeric bin to assign
			int assign = 0;
			for (int j = 0; j < numericAttributes_all[i].length; j++) {
				int k = brks[j].length - 2;
				for (; k >= 0; k--) {
					if (numericAttributes_all[i][j] >= brks[j][k]) {
						break;
					}
				}

				/*
				 * if the bin number is 1,2,3 then the index is 1* numBreaks^2 +
				 * 2* numBreaks^1 + 3*numBreaks^0
				 */
				assign += k
						* Math.pow(numRanges, (numericAttributes_all[i].length
								- j - 1));
			}

			bin_counts[assign]++;
		}

		/*
		 * Return the function that maps an attribute range to a bin
		 */
		TreeMap<Double, List<SingleValueBin>> mapping = new TreeMap<Double, List<SingleValueBin>>();

		for (int i = 0; i < numBins; i++) {
			int keyIndex = i / numRanges; // more general, i / (int)
											// Math.pow(numRanges,
											// numericValues.length)
			int valueIndex = i % numRanges; // (i / (int) Math.pow(numRanges,
											// numericValues.length - j - 1)) %
											// numRanges;

			SingleValueBin bin = new SingleValueBin(brks[1][valueIndex],
					brks[1][valueIndex + 1], bin_counts[i] * 1.0
							/ numericAttributes_all.length);

			if (!mapping.containsKey(brks[0][keyIndex])) {
				List<SingleValueBin> ls = new ArrayList<SingleValueBin>(numRanges);
				mapping.put(brks[0][keyIndex], ls);
			}

			mapping.get(brks[0][keyIndex]).add(bin);
		}

		return mapping;
	}
}