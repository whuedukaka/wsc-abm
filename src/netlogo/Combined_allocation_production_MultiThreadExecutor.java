package netlogo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import controller.ZambiaMultiThreadSimulator;

/**
 * Multi-threaded: used by the GeneticAlgorithmInterface.
 * 
 * @author Peng
 * 
 */
public class Combined_allocation_production_MultiThreadExecutor {
	// random seed in NetLogo
	public static int randomSeed = 0;
	
	// number of HH per ward
	// public static int numHHperWard;
	// number of threads running in parallel
	public static int NTHREDS;
	// number of seed HHs
	public static int[] numSeedHH;
	// repeat the allocation process to create different worlds
	public static int num_HH_allocation_repetition;
	// Parameter 'ag_year'
	public static int[] ag_year;

	// Climate-production model file
	public static String productionModelFile;

	/**
	 * Method invoked in each thread that creates the 'world' and export to disk
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void start() throws IOException, InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);

		/*
		 * Populate the parameters that will be passed to executor
		 */
		String[] start_cmds = { "setup" };
		String[] go_cmds = { "while [ run_biweekly != 288 ] []",
				// "output_stats",
				"output_yield" };

		String[] attr_names = { "total_number_HH", "landcover_file",
				"HH_attribute_file", "road_dist_file", "production_model_file",
				"gis_header_file", "num_seed_HH", "global_maize_ratio",
				"current_year", "soilType", "search_scope",
				"labor_sharing_pecent",
				// "output_stats_file",
				"output_yield_list", "current_day", "last_day",
				"monthly_food_consum", "labor_land_ratio_to_weed" };

		/*
		 * Read ward info from "summary" file
		 */
		BufferedReader br = new BufferedReader(new FileReader(
				ZambiaMultiThreadSimulator.summary_file_path));
		List<String[]> summary = new ArrayList<String[]>();
		String readLine = br.readLine();// header
		int ward_id = 0, x_cor = 0, y_cor = 0, num_HH = 0;
		String[] headers = readLine.split("\\s");

		for (int i = 0; i < headers.length; i++) {
			if (headers[i].equals("ward"))
				ward_id = i;
			else if (headers[i].equals("x"))
				x_cor = i;
			else if (headers[i].equals("y"))
				y_cor = i;
			else if (headers[i].equals("#HH"))
				num_HH = i;
		}

		while ((readLine = br.readLine()) != null) {
			summary.add(readLine.split("\\s"));
		}

		br.close();

		int[] lc = new int[summary.size()];
		int[] noHH = new int[summary.size()];
		int[] xCor = new int[summary.size()];
		int[] yCor = new int[summary.size()];

		for (int i = 0; i < summary.size(); i++) {
			lc[i] = Integer.parseInt(summary.get(i)[ward_id]);
			noHH[i] = Integer.parseInt(summary.get(i)[num_HH]);
			xCor[i] = Integer.parseInt(summary.get(i)[x_cor]);
			yCor[i] = Integer.parseInt(summary.get(i)[y_cor]);
		}

		/*
		 * For each combination of parameters, create the 'world'
		 */
		for (int i = 0; i < lc.length; i++) {
			for (int j = 0; j < ag_year.length; j++) {
				for (int k = 0; k < numSeedHH.length; k++) {
					for (int l = 0; l < num_HH_allocation_repetition; l++) {
						for (int m = 0; m < ZambiaMultiThreadSimulator.labor_sharing_pecent.length; m++) {
							for (int n = 0; n < ZambiaMultiThreadSimulator.search_scope.length; n++) {
								for (int o = 0; o < ZambiaMultiThreadSimulator.soilType.length; o++) {
									String[] attr_values = {
											String.valueOf(noHH[i]),
											"\"tmp/lc_" + lc[i] + ".txt\"",
											"\"tmp/HH_" + lc[i] + ".txt\"",
											"\"tmp/dist_" + lc[i] + ".txt\"",
											"\"" + productionModelFile + "\"",
											"\"tmp/header_" + lc[i] + ".txt\"",
											String.valueOf(numSeedHH[k]),
											"0.7",
											String.valueOf(ag_year[j]),
											"\""
													+ ZambiaMultiThreadSimulator.soilType[o]
													+ "\"",
											String.valueOf(ZambiaMultiThreadSimulator.search_scope[n]),
											String.valueOf(ZambiaMultiThreadSimulator.labor_sharing_pecent[m]),
											// "\"OutputData/ward_"
											// + lc[i]
											// + "_numSeedHH_"
											// + numSeedHH[k]
											// + "_AgYear_"
											// + String.valueOf(ag_year[j])
											// + "_allocation_"
											// + l
											// + "_soilType_"
											// +
											// String.valueOf(ZambiaMultiThreadSimulator.soilType[o])
											// + "_searchScope_"
											// +
											// String.valueOf(ZambiaMultiThreadSimulator.search_scope[n])
											// + "_sharingPercent_"
											// +
											// String.valueOf(ZambiaMultiThreadSimulator.labor_sharing_pecent[m])
											// + "_stats.txt\"",
											"\"OutputData/ward_"
													+ lc[i]
													+ "_numSeedHH_"
													+ numSeedHH[k]
													+ "_AgYear_"
													+ String.valueOf(ag_year[j])
													+ "_allocation_"
													+ l
													+ "_soilType_"
													+ String.valueOf(ZambiaMultiThreadSimulator.soilType[o])
													+ "_searchScope_"
													+ String.valueOf(ZambiaMultiThreadSimulator.search_scope[n])
													+ "_sharingPercent_"
													+ String.valueOf(ZambiaMultiThreadSimulator.labor_sharing_pecent[m])
													+ "_yield_list.txt\"",
											String.valueOf(ZambiaMultiThreadSimulator.startDay),
											String.valueOf(ZambiaMultiThreadSimulator.startDay),
											String.valueOf(ZambiaMultiThreadSimulator.foodConsumRate),
											String.valueOf(ZambiaMultiThreadSimulator.weedingLaborLandRatio) };

									String[] attr_cmds = new String[attr_names.length];
									for (int x = 0; x < attr_cmds.length; x++) {
										attr_cmds[x] = "set " + attr_names[x]
												+ " " + attr_values[x];
									}

									List<String> cmds = new LinkedList<String>();
									for (String cmd : attr_cmds) {
										cmds.add(cmd);
									}
									
									// expose random seed to the GA
									cmds.add("random-seed " + randomSeed);
									
									for (String cmd : start_cmds) {
										cmds.add(cmd);
									}
									for (String cmd : go_cmds) {
										cmds.add(cmd);
									}

									Runnable worker = new Execution(
											ZambiaMultiThreadSimulator.model_path,
											0, 0, xCor[i] - 1, yCor[i] - 1,
											cmds);
									executor.execute(worker);
								}
							}
						}
					}
				}
			}
		}

		// This will make the executor accept no new threads
		// and finish all existing threads in the queue
		executor.shutdown();
		// Wait until all threads are finish
		// Wait until all threads are finish
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

		System.out.println("Finished all threads");
	}
}
