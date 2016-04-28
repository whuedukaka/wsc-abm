package postProcessing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import postProcessing.network.HH_Vertex;
import postProcessing.network.Labor_Export_Edge;
import postProcessing.network.Labor_Export_Network;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Transform the output labor sharing network (topology and attrbutes) into two
 * CSV files (one has all the nodes, the other has all the edges) which can be
 * later imported into visualization tools like Gephi
 * 
 * @author Peng
 * 
 */
public class NetworkExtractor {
	// public static String filePath =
	// "OutputData/ward_33_numSeedHH_10_allocation_0__reqadult_4.876_alpha_0.5_searchScope_7_network.txt";

	public static List<HH_Vertex> ls = new LinkedList<HH_Vertex>();

	public static void main(String[] args) throws IOException {
		/*
		 * Prompt for correct input arguments
		 */
		if (args.length != 1) {
			System.out
					.println("Usage: java NetworkTopologyExtractor path_to_input_network_file");
			return;
		}

		BufferedReader br = new BufferedReader(new FileReader(args[0]));

		Labor_Export_Network network = new Labor_Export_Network();

		String readLine = br.readLine();// skip the header

		/*
		 * First section is about HH's yield and labor exchange stats
		 */
		while ((readLine = br.readLine()) != null) {
			if (readLine.startsWith("#"))// comment line
				break;

			System.out.println(readLine);
			parseLine_Sec1(readLine, network);
		}

		/*
		 * Second section is about HH's coordinates
		 */
		while ((readLine = br.readLine()) != null) {
			if (readLine.startsWith("#"))// comment line
				break;

			System.out.println(readLine);
			parseLine_Sec2(readLine, network);
		}

		/*
		 * Third section is about some anchor points
		 */
		while ((readLine = br.readLine()) != null) {
			if (readLine.startsWith("#"))// comment line
				break;

			System.out.println(readLine);
			parseLine_Sec3(readLine, network);
		}

		/*
		 * Write the network into csv files
		 */
		writeNetwork(network, "OutputData/nodes.txt", "OutputData/edges.txt");
	}

	public static void parseLine_Sec1(String line, Labor_Export_Network network) {
		char[] chars = line.toCharArray();

		int pre = 0, cur = 0;

		// look for HH id
		while (chars[cur++] != ' ')
			;

		int id = Integer.parseInt(line.substring(pre, cur - 1));// the blank
																// space is the
																// endIndex
		pre = cur;

		// look for HH yield
		while (chars[cur++] != ' ')
			;

		double yield = Double.parseDouble(line.substring(pre, cur - 1));
		pre = cur;

		HH_Vertex hh = network.addHH(id, yield);
		System.out.println("Add HH: " + id + " yield " + yield);

		Stack<Character> s = new Stack<Character>();
		if (chars[cur] != '[') {
			System.out.println("Error: cannot match the first '['");
			return;
		}

		s.add(chars[cur++]);

		// find all imports
		int source = 0;
		while (true) {
			char c = chars[cur++];

			if (c == '[') {
				s.push(c);
				pre = cur;
			} else if (c == ' ' && pre != cur - 1) {
				source = Integer.parseInt(line.substring(pre, cur - 1));
				pre = cur;
			} else if (c == ']') {
				s.pop();

				if (!s.isEmpty()) {
					double labor = Double.parseDouble(line.substring(pre,
							cur - 1));

					Labor_Export_Edge rel = null;
					if ((rel = network.getRelation(source, id)) != null) {
						rel.labor += labor;
					} else
						network.addRelation(source, id, labor);

					System.out.println("Add import: " + source + " labor "
							+ labor);
					pre = cur;
				} else
					break;
			}
		}

		cur++;
		if (chars[cur] != '[') {
			System.out.println("Error: cannot match the second '['");
			return;
		}

		s.add(chars[cur++]);

		// find all exports
		int dest = 0;
		while (true) {
			char c = chars[cur++];

			if (c == '[') {
				s.push(c);
				pre = cur;
			} else if (c == ' ' && pre != cur - 1) {
				dest = Integer.parseInt(line.substring(pre, cur - 1));
				pre = cur;
			} else if (c == ']') {
				s.pop();

				if (!s.isEmpty()) {
					double labor = Double.parseDouble(line.substring(pre,
							cur - 1));
					// avoid adding the same edge twice
					// network.addRelation(id, dest, labor);
					System.out.println("Add export: " + dest + " labor "
							+ labor);
					pre = cur;
				} else
					break;
			}
		}

		pre = ++cur;
		// look for HH_SIZE
		// while (chars[cur++] != ' ')
		// ;
		//
		// double hh_size = Double.parseDouble(line.substring(pre, cur - 1));
		// pre = cur;
		//
		// hh.hh_size = hh_size;
		// System.out.println("hh_size: " + hh_size);

		// look for ADULT_EQ
		while (chars[cur++] != ' ')
			;

		double adult_eq = Double.parseDouble(line.substring(pre, cur - 1));
		pre = cur;

		hh.adult_eq = adult_eq;
		System.out.println("adult_eq: " + adult_eq);

		// look for AREA_HA
		while (chars[cur++] != ' ')
			;

		double area_ha = Double.parseDouble(line.substring(pre, cur - 1));
		pre = cur;

		hh.area_ha = area_ha;
		System.out.println("area_ha: " + area_ha);

		// look for IS_HYBRID
		// while (cur++ < chars.length)
		// ;
		while (chars[cur++] != ' ')
			;

		int is_hybrid = Integer.parseInt(line.substring(pre, cur - 1));
		pre = cur;

		hh.is_hybrid = is_hybrid;
		System.out.println("is_hybrid: " + is_hybrid);
	}

	public static void parseLine_Sec2(String line, Labor_Export_Network network) {
		String[] strs = line.split("\\s");
		HH_Vertex hh = network.getHH(Integer.parseInt(strs[0]));

		hh.seed_x = Integer.parseInt(strs[1]);
		hh.seed_y = Integer.parseInt(strs[2]);

		System.out.println("Set HH " + hh.id + " x:" + hh.seed_x + " y:"
				+ hh.seed_y);
	}

	public static void parseLine_Sec3(String line, Labor_Export_Network network) {
		String[] strs = line.split("\\s");

		int id = network.getMaxID() + 1;
		network.addHH(id, 0);
		HH_Vertex hh = network.getHH(id);

		hh.seed_x = Integer.parseInt(strs[1]);
		hh.seed_y = Integer.parseInt(strs[2]);

		System.out.println("Set HH " + hh.id + " x:" + hh.seed_x + " y:"
				+ hh.seed_y);
	}

	/**
	 * Write the labor sharing network into two csv files
	 * 
	 * @param network
	 * @param nodeCSV
	 *            csv file that holds all the nodes
	 * @param edgeCSV
	 *            csv file that holds all the edges
	 * @throws IOException
	 */
	public static void writeNetwork(Labor_Export_Network network,
			String nodeCSV, String edgeCSV) throws IOException {
		CSVWriter cw_node = new CSVWriter(new FileWriter(nodeCSV));
		String[] header_node = { "Id", "avg_yield", "x_cor", "y_cor",
				"adult_eq", "area_ha",
				// "hh_size",
				"is_hybrid" };
		cw_node.writeNext(header_node);

		CSVWriter cw_edge = new CSVWriter(new FileWriter(edgeCSV));
		String[] header_edge = { "Source", "Target", "labor" };
		cw_edge.writeNext(header_edge);

		for (HH_Vertex hh : network.HHs.values()) {
			String[] data = { String.valueOf(hh.id),
					String.valueOf(hh.avg_yield), String.valueOf(hh.seed_x),
					String.valueOf(hh.seed_y), String.valueOf(hh.adult_eq),
					String.valueOf(hh.area_ha),
					// String.valueOf(hh.hh_size),
					String.valueOf(hh.is_hybrid) };
			cw_node.writeNext(data);
		}

		for (Labor_Export_Edge e : network.Relations) {
			String[] data = { String.valueOf(e.source.id),
					String.valueOf(e.dest.id), String.valueOf(e.labor) };
			cw_edge.writeNext(data);
		}

		cw_node.flush();
		cw_node.close();
		cw_edge.flush();
		cw_edge.close();
	}
}
