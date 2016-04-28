package preparation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import netlogo.HH_allocation_MultiThreadExecutor;
import netlogo.LaborSharing_Production_MultiThreadExecutor;

/**
 * 
 * Input: wards raster + landcover raster
 * 
 * Output: landcover rasters partitioned based on wards
 * 
 * @author Peng
 * 
 */
public class WardsPartition {
	public static int width;
	public static int height;
	public static String wards_raster_path;
	public static String lc_raster_path;
	public static String road_dist_raster_path;
	public static String summary_file_path;

	public static void main(String[] args) throws IOException {
		Properties properties = new Properties();
		properties.load(new FileReader(args[0]));
		WardsPartition.width = Integer.parseInt(properties
				.getProperty("raster.width"));
		WardsPartition.height = Integer.parseInt(properties
				.getProperty("raster.height"));
		WardsPartition.wards_raster_path = properties
				.getProperty("wards_raster.path");
		WardsPartition.lc_raster_path = properties
				.getProperty("landcover_raster.path");
		WardsPartition.road_dist_raster_path = properties
				.getProperty("road_dist_raster.path");
		WardsPartition.summary_file_path = properties
				.getProperty("summary_file.path");
		partition();
	}

	public static void partition() throws IOException {
		BufferedReader br = new BufferedReader(
				new FileReader(wards_raster_path));

		Map<Integer, Integer> leftBound = new HashMap<Integer, Integer>();
		Map<Integer, Integer> rightBound = new HashMap<Integer, Integer>();
		Map<Integer, Integer> topBound = new HashMap<Integer, Integer>();
		Map<Integer, Integer> bottomBound = new HashMap<Integer, Integer>();

		// the number of rows is the height of raster and the number of columns
		// is the width
		int[][] wards_matrix = new int[height][width];
		int m = 0, n = 0;

		String readLine = null;
		while ((readLine = br.readLine()) != null) {
			String[] toks = readLine.split("\\s");

			for (String tok : toks) {

				if (tok.equals("[") || tok.equals("]"))
					continue;
				else {
					wards_matrix[m][n] = Integer.parseInt(tok);

					if (!leftBound.containsKey(wards_matrix[m][n])
							|| leftBound.get(wards_matrix[m][n]) > n) {
						leftBound.put(wards_matrix[m][n], n);
					}

					if (!rightBound.containsKey(wards_matrix[m][n])
							|| rightBound.get(wards_matrix[m][n]) < n) {
						rightBound.put(wards_matrix[m][n], n);
					}

					if (!topBound.containsKey(wards_matrix[m][n])
							|| topBound.get(wards_matrix[m][n]) < m) {
						topBound.put(wards_matrix[m][n], m);
					}

					if (!bottomBound.containsKey(wards_matrix[m][n])
							|| bottomBound.get(wards_matrix[m][n]) > m) {
						bottomBound.put(wards_matrix[m][n], m);
					}
					n++;
				}
			}
			
			m++;
			n = 0;
		}

		System.out.println("m: " + m + "\tn: " + n);

		br.close();

		// partition the landcover dataset based on wards

		int[][] landcover_matrix = new int[height][width];
		br = new BufferedReader(new FileReader(lc_raster_path));
		// read the landcover dataset into the matrix
		m = 0;
		n = 0;
		while ((readLine = br.readLine()) != null) {
			String[] toks = readLine.split("\\s");

			for (String tok : toks) {

				if (tok.equals("[") || tok.equals("]"))
					continue;
				else {
					landcover_matrix[m][n] = Integer.parseInt(tok);
					n++;
				}
			}
			
			m++;
			n = 0;
		}
		
		br.close();

		// ArrayList<int[][]> landcover_wards = new ArrayList<int[][]>();

		BufferedWriter bw_summary = new BufferedWriter(new FileWriter(
				summary_file_path));
		bw_summary.append("ward\tx\ty");
		bw_summary.newLine();

		for (Integer id : leftBound.keySet()) {
			if (id == -9999)
				continue;

			int left = leftBound.get(id);
			int right = rightBound.get(id);
			int bottom = bottomBound.get(id);
			int top = topBound.get(id);

			BufferedWriter bw = new BufferedWriter(new FileWriter(
					"InputData/lc" + id + ".txt"));
			bw.append("[ ");

			int[][] landcover_ward = new int[top - bottom + 1][right - left + 1];
			for (int i = 0; i < landcover_ward.length; i++) {
				for (int j = 0; j < landcover_ward[i].length; j++) {
					if (wards_matrix[bottom + i][left + j] == id)
						landcover_ward[i][j] = landcover_matrix[bottom + i][left
								+ j];
					else
						landcover_ward[i][j] = -9999;

					bw.append(landcover_ward[i][j] + " ");
				}
			}

			bw.append("]");
			bw.flush();
			bw.close();

			// landcover_wards.add(landcover_ward);

			System.out.println("num - (left,right,top,bottom): " + id + " - "
					+ left + " " + right + " " + top + " " + bottom + " ("
					+ (right - left + 1) + "," + (top - bottom + 1) + ")");

			bw_summary.append(id + "\t" + (right - left + 1) + "\t"
					+ (top - bottom + 1));
			bw_summary.newLine();
		}

		bw_summary.flush();
		bw_summary.close();
		
		// partition the road distance dataset based on wards
		double[][] roadDist_matrix = new double[height][width];
		br = new BufferedReader(new FileReader(road_dist_raster_path));
		// read the road distance dataset into the matrix
		m = 0;
		n = 0;
		while ((readLine = br.readLine()) != null) {
			String[] toks = readLine.split("\\s");

			for (String tok : toks) {

				if (tok.equals("[") || tok.equals("]"))
					continue;
				else {
					roadDist_matrix[m][n] = Double.parseDouble(tok);
					n++;
				}
			}
			
			m++;
			n = 0;
		}
		br.close();
		
		for (Integer id : leftBound.keySet()) {
			if (id == -9999)
				continue;

			int left = leftBound.get(id);
			int right = rightBound.get(id);
			int bottom = bottomBound.get(id);
			int top = topBound.get(id);

			BufferedWriter bw = new BufferedWriter(new FileWriter(
					"InputData/dist" + id + ".txt"));
			bw.append("[ ");

			double[][] roadDist_ward = new double[top - bottom + 1][right - left + 1];
			for (int i = 0; i < roadDist_ward.length; i++) {
				for (int j = 0; j < roadDist_ward[i].length; j++) {
					if (wards_matrix[bottom + i][left + j] == id)
						roadDist_ward[i][j] = landcover_matrix[bottom + i][left
								+ j];
					else
						roadDist_ward[i][j] = -9999;

					bw.append(roadDist_ward[i][j] + " ");
				}
			}

			bw.append("]");
			bw.flush();
			bw.close();
		}
	}
}
