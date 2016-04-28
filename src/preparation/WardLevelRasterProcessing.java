package preparation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import controller.ZambiaMultiThreadSimulator;

public class WardLevelRasterProcessing {
	public static void main(String[] args) {
		process();
	}

	public static void process() {
		BufferedReader br_ward = null;
		BufferedReader br_lc = null;
		BufferedReader br_dist = null;

		BufferedWriter bw_ward = null;
		BufferedWriter bw_lc = null;
		BufferedWriter bw_dist = null;

		BufferedWriter bw_header = null;
		BufferedWriter bw_summary = null;

		try {
			bw_summary = new BufferedWriter(new FileWriter(
					ZambiaMultiThreadSimulator.summary_file_path));
			List<String> summaryLines = new LinkedList<String>();

			for (int i = 1; i <= 22; i++) {

				br_ward = new BufferedReader(new FileReader("InputData/ward_"
						+ i + ".txt"));
				br_lc = new BufferedReader(new FileReader("InputData/lc_" + i
						+ ".txt"));
				br_dist = new BufferedReader(new FileReader("InputData/dist_"
						+ i + ".txt"));

				bw_ward = new BufferedWriter(new FileWriter("tmp/ward_" + i
						+ ".txt"));
				bw_lc = new BufferedWriter(new FileWriter("tmp/lc_" + i
						+ ".txt"));
				bw_dist = new BufferedWriter(new FileWriter("tmp/dist_" + i
						+ ".txt"));

				bw_header = new BufferedWriter(new FileWriter("tmp/header_" + i
						+ ".txt"));

				List<String> commonHeader = new LinkedList<String>();
				if (false == processFile(br_ward, commonHeader, bw_ward)) {
					System.out.println("Geo-header mismatch for ward " + i);
					System.exit(-1);
				}

				if (false == processFile(br_lc, commonHeader, bw_lc)) {
					System.out
							.println("Geo-header mismatch for landcover " + i);
					System.exit(-1);
				}

				if (false == processFile(br_dist, commonHeader, bw_dist)) {
					System.out.println("Geo-header mismatch for road distance "
							+ i);
					System.exit(-1);
				}

				// for (String line : commonHeader) {
				Iterator<String> header_itr = commonHeader.iterator();
				String ncols = null, nrows = null;
				for (int j = 0; j < commonHeader.size(); j++) {
					String line = header_itr.next();

					if (j == 0) {
						ncols = line.split("\\s+")[1].trim();
					} else if (j == 1) {
						nrows = line.split("\\s+")[1].trim();
					} else if (j == 5) {
						bw_header.append("NODATA_value  " + 0); // replace the
																// NA_value
						bw_header.newLine();
						continue;
					}

					bw_header.append(line);
					bw_header.newLine();
				}
				bw_header.flush();

				summaryLines.add(i + "\t" + ncols + "\t" + nrows);
			}

			bw_summary.append("ward\tx\ty");
			bw_summary.newLine();
			for (String line : summaryLines) {
				bw_summary.append(line);
				bw_summary.newLine();
			}
			bw_summary.flush();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (br_ward != null) {
				try {
					br_ward.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (br_lc != null) {
				try {
					br_lc.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (br_dist != null) {
				try {
					br_dist.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (bw_ward != null) {
				try {
					bw_ward.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (bw_lc != null) {
				try {
					bw_lc.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (bw_dist != null) {
				try {
					bw_dist.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (bw_header != null) {
				try {
					bw_header.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (bw_summary != null) {
				try {
					bw_summary.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private static boolean processFile(BufferedReader br,
			List<String> geoHeader, BufferedWriter bw) throws IOException {
		String readLine = null;

		int count = 0;
		List<String> header = new LinkedList<String>();
		List<String> data = new LinkedList<String>();
		while ((readLine = br.readLine()) != null) {
			// System.out.println(readLine);
			if (count <= 5) {
				header.add(readLine);
			} else {
				data.add(readLine);
			}

			count++;
		}

		bw.append("[ ");
		for (String line : data) {
			bw.append(line);
			bw.newLine();
		}
		bw.append("]");
		bw.flush();

		if (geoHeader.size() != 0) {
			if (geoHeader.size() != header.size()) {
				return false;
			} else {
				Iterator<String> target_itr = geoHeader.iterator();
				Iterator<String> itr = header.iterator();
				while (target_itr.hasNext()) {
					if (!target_itr.next().equals(itr.next())) {
						return false;
					}
				}
			}

			return true;
		} else {
			for (String line : header) {
				geoHeader.add(line);
			}
			return true;
		}
	}
}
