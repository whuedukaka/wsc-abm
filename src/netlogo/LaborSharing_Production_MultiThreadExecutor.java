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
 * Simulation of labor sharing activities using Multiple NetLogo instances in
 * different threads
 * 
 * @author Peng
 * 
 */
public class LaborSharing_Production_MultiThreadExecutor {
	// public static int numHHperWard;
	// Number of threads running in parallel
	public static int NTHREDS;

	/**
	 * Each thread invokes this method to simulate the labor sharing and maize
	 * production using a different instance of NetLogo
	 * 
	 * @param macineId
	 *            - the id of the current machine, starting from 0
	 * @param totalMachineNum
	 *            - total num of machines
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void start(int machineId, int totalMachineNum)
			throws IOException, InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);

		/*
		 * Populate the parameters that will be passed to executor
		 */
		String[] start_cmds = { "import_world" };
		String[] go_cmds = { "run_for_a_year" };

		String[] attr_names = {
				// "cultiVar",
				"soilType", "search_scope", "labor_sharing_pecent",
				"output_stats_file", "output_labor_sharing_map",
				"output_yield_map", "current_day", "last_day",
				"monthly_food_consum", "labor_land_ratio_to_weed" };

		/*
		 * Read world into the model and carry out simulations
		 */
		BufferedReader br = new BufferedReader(new FileReader(
				ZambiaMultiThreadSimulator.simulation_task_file_path));
		ArrayList<String> tasks = new ArrayList<String>();
		String readLine = null;
		while ((readLine = br.readLine()) != null) {
			tasks.add(readLine);
		}

		br.close();

		// Get the share by the machine Id
		for (int i = machineId; i < tasks.size(); i += totalMachineNum) {
			String[] parameters = tasks.get(i).split(",");

			String[] attr_values = {
					"\"" + parameters[1] + "\"",
					parameters[2],
					parameters[3],
					"\"OutputData/"
							+ parameters[0].substring(0,
									parameters[0].lastIndexOf('.'))
							+ "_soilType_" + String.valueOf(parameters[1])
							+ "_searchScope_" + String.valueOf(parameters[2])
							+ "_sharingPercent_"
							+ String.valueOf(parameters[3]) + "_stats.txt\"",
					"\"OutputData/"
							+ parameters[0].substring(0,
									parameters[0].lastIndexOf('.'))
							+ "_soilType_" + String.valueOf(parameters[1])
							+ "_searchScope_" + String.valueOf(parameters[2])
							+ "_sharingPercent_"
							+ String.valueOf(parameters[3])
							+ "_labor_sharing_map.txt\"",
					"\"OutputData/"
							+ parameters[0].substring(0,
									parameters[0].lastIndexOf('.'))
							+ "_soilType_" + String.valueOf(parameters[1])
							+ "_searchScope_" + String.valueOf(parameters[2])
							+ "_sharingPercent_"
							+ String.valueOf(parameters[3])
							+ "_yield_map.txt\"",
					String.valueOf(ZambiaMultiThreadSimulator.startDay),
					String.valueOf(ZambiaMultiThreadSimulator.startDay),
					String.valueOf(ZambiaMultiThreadSimulator.foodConsumRate), 
					String.valueOf(ZambiaMultiThreadSimulator.weedingLaborLandRatio)};

			String[] attr_cmds = new String[attr_values.length];
			for (int m = 0; m < attr_cmds.length; m++) {
				attr_cmds[m] = "set " + attr_names[m] + " " + attr_values[m];
			}

			List<String> cmds = new LinkedList<String>();
			cmds.add("set input_world_file" + "\"tmp/" + parameters[0] + "\"");

			for (String cmd : start_cmds) {
				cmds.add(cmd);
			}
			for (String cmd : attr_cmds) {
				cmds.add(cmd);
			}
			for (String cmd : go_cmds) {
				cmds.add(cmd);
			}

			Runnable worker = new Execution(
					ZambiaMultiThreadSimulator.model_path, 0, 0, 0, 0, cmds);
			executor.execute(worker);
		}

		// This will make the executor accept no new threads
		// and finish all existing threads in the queue
		executor.shutdown();
		// Wait until all threads are finish
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

		System.out.println("Finished all threads");
	}
}
