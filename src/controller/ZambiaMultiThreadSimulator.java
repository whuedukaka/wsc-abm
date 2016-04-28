package controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import netlogo.HH_allocation_MultiThreadExecutor;
import netlogo.LaborSharing_Production_MultiThreadExecutor;
import preparation.BinMapping;
import preparation.FarmerRecord;
import preparation.SingleValueBin;
import preparation.WardLevelRasterProcessing;
import preparation.WardsPartition;

/**
 * Main entry of this multi-threaded simulator
 * 
 * Usage: java -jar ZambiaMultiThreadSimulator.jar path_to_properties_file 0|1
 * 0--create worlds; 1--run simulation
 * 
 * @author Peng
 * 
 */
public class ZambiaMultiThreadSimulator {
	static String farmerRegisterFile;
	static String syntheticPopulationFile;
	static String georeferenceHeaderFile;

	public static String summary_file_path;
	public static String simulation_task_file_path;
	// Path to the NetLogo model
	public static String model_path;

	/*
	 * Parameter space
	 */
	// Parameter 'search distance'
	public static int[] search_scope;
	// Parameter 'climate scenario'
	public static String[] climate_scenario;
	// Parameter 'labor_sharing_pecent'
	public static double[] labor_sharing_pecent;
	// Parameter 'cultiVar'
	// public static String[] cultiVar;
	// Paramter 'soilType'
	public static String[] soilType;
	public static int startDay;
	public static int foodConsumRate;
	public static double weedingLaborLandRatio;

	public static void main(String[] args) throws IOException,
			InterruptedException {
		/*
		 * Prompt for correct input arguments
		 */
		if (args.length != 2
				&& (!args[1].equalsIgnoreCase("2") || args.length != 4)) {
			String usage = "Usage: java -jar ZambiaMultiThreadSimulator.jar path_to_properties_file 0|1|(2 [machineId] [numOfMachines])"
					+ " ;;0--prepare the landcover data and the simulated HHs, 1--allocate simulated HHs to create worlds, 2--run simulation";
			System.out.println(usage);
			return;
		}

		// Remember the start time
		long startTime = new Date().getTime();

		/*
		 * Load properties from the property file and configure the
		 * corresponding static parameters in java Class
		 */
		Properties properties = new Properties();
		properties.load(new FileReader(args[0]));

		ZambiaMultiThreadSimulator.model_path = properties
				.getProperty("model.path");
		// ZambiaMultiThreadSimulator.farmerRegisterFile = properties
		// .getProperty("farmer_register.path");
		ZambiaMultiThreadSimulator.georeferenceHeaderFile = properties
				.getProperty("georeference_headers.path");
		ZambiaMultiThreadSimulator.syntheticPopulationFile = properties
				.getProperty("synthetic_population.path");
		ZambiaMultiThreadSimulator.simulation_task_file_path = properties
				.getProperty("simulation_task_file.path");
		ZambiaMultiThreadSimulator.summary_file_path = properties
				.getProperty("summary_file.path");

		// WardsPartition.width = Integer.parseInt(properties
		// .getProperty("raster.width"));
		// WardsPartition.height = Integer.parseInt(properties
		// .getProperty("raster.height"));
		// WardsPartition.wards_raster_path = properties
		// .getProperty("wards_raster.path");
		// WardsPartition.lc_raster_path = properties
		// .getProperty("landcover_raster.path");
		// WardsPartition.road_dist_raster_path = properties
		// .getProperty("road_dist_raster.path");
		// WardsPartition.summary_file_path = properties
		// .getProperty("summary_file.path");

		// BinMapping.HH_AttributeFile = properties
		// .getProperty("HH_attribute_csv.path");
		// BinMapping.ward_colName = properties.getProperty("ward_colName");

		// BinMapping.numericKey = properties
		// .getProperty("HH_attribute.numericKey");
		// String[] keyBreaks = properties.getProperty("HH_attribute.keyBreaks")
		// .split("\\s");
		//
		// BinMapping.keyBreaks = new double[keyBreaks.length];
		// for (int i = 0; i < keyBreaks.length; i++) {
		// BinMapping.keyBreaks[i] = Double.valueOf(keyBreaks[i]);
		// }
		//
		// BinMapping.numericValue = properties
		// .getProperty("HH_attribute.numericValue");
		// String[] valueBreaks = properties.getProperty(
		// "HH_attribute.valueBreaks").split("\\s");
		//
		// BinMapping.valueBreaks = new double[valueBreaks.length];
		// for (int i = 0; i < valueBreaks.length; i++) {
		// BinMapping.valueBreaks[i] = Double.valueOf(valueBreaks[i]);
		// }
		//
		// BinMapping.baseline = Integer.parseInt(properties
		// .getProperty("HH_attribute.separateBinningBaseline"));
		// BinMapping.numRanges = Integer.parseInt(properties
		// .getProperty("HH_attribute.numRanges"));

		/*
		 * Fill attributes for 'HH_allocation_MultiThreadExecutor'
		 */
		HH_allocation_MultiThreadExecutor.num_HH_allocation_repetition = Integer
				.parseInt(properties
						.getProperty("executor.num_HH_allocation_repetition"));
		HH_allocation_MultiThreadExecutor.productionModelFile = properties
				.getProperty("crop_model.path");

		String[] parameters_numSeedHH = properties.getProperty(
				"model.parameter.num_seed_HH").split("\\s");
		String[] parameters_ag_year = properties.getProperty(
				"model.parameter.ag_year").split("\\s");

		HH_allocation_MultiThreadExecutor.numSeedHH = new int[parameters_numSeedHH.length];
		for (int i = 0; i < parameters_numSeedHH.length; i++) {
			HH_allocation_MultiThreadExecutor.numSeedHH[i] = Integer
					.parseInt(parameters_numSeedHH[i]);
		}

		HH_allocation_MultiThreadExecutor.ag_year = new int[parameters_ag_year.length];
		for (int i = 0; i < parameters_ag_year.length; i++) {
			HH_allocation_MultiThreadExecutor.ag_year[i] = Integer
					.parseInt(parameters_ag_year[i]);
		}

		/*
		 * Fill attributes for 'LaborSharing_Production_MultiThreadExecutor'
		 */
		LaborSharing_Production_MultiThreadExecutor.NTHREDS = HH_allocation_MultiThreadExecutor.NTHREDS = Integer
				.parseInt(properties.getProperty("executor.number_of_threads"));

		String[] parameters_search_scope = properties.getProperty(
				"model.parameter.search_scope").split("\\s");
		// String[] parameters_cultiVar = properties.getProperty(
		// "model.parameter.cultiVar").split("\\s");
		String[] parameters_soilType = properties.getProperty(
				"model.parameter.soilType").split("\\s");
		String[] labor_sharing_pecent = properties.getProperty(
				"model.parameter.labor_sharing_percent").split("\\s");

		ZambiaMultiThreadSimulator.search_scope = new int[parameters_search_scope.length];
		ZambiaMultiThreadSimulator.labor_sharing_pecent = new double[labor_sharing_pecent.length];
		// ZambiaMultiThreadSimulator.cultiVar = new
		// String[parameters_cultiVar.length];
		ZambiaMultiThreadSimulator.soilType = new String[parameters_soilType.length];

		for (int i = 0; i < parameters_search_scope.length; i++) {
			ZambiaMultiThreadSimulator.search_scope[i] = Integer
					.parseInt(parameters_search_scope[i]);
		}

		// for (int i = 0; i < parameters_cultiVar.length; i++) {
		// ZambiaMultiThreadSimulator.cultiVar[i] = parameters_cultiVar[i];
		// }

		for (int i = 0; i < parameters_soilType.length; i++) {
			ZambiaMultiThreadSimulator.soilType[i] = parameters_soilType[i];
		}

		for (int i = 0; i < labor_sharing_pecent.length; i++) {
			ZambiaMultiThreadSimulator.labor_sharing_pecent[i] = Double
					.parseDouble(labor_sharing_pecent[i]);
		}

		startDay = Integer.parseInt(properties
				.getProperty("model.parameter.starting_day"));
		foodConsumRate = Integer.parseInt(properties
				.getProperty("model.parameter.kg_food_per_person_month"));
		weedingLaborLandRatio = Integer.parseInt(properties
				.getProperty("model.parameter.labor_land_ratio_to_weed"));

		/*
		 * Prepare the 'world' or run the simulation based on the second input
		 * argument
		 */
		if (args[1].equalsIgnoreCase("0")) {
			prepareData();
		} else if (args[1].equalsIgnoreCase("1")) {
			/*
			 * Start the NetLogo simulations
			 */
			HH_allocation_MultiThreadExecutor.start();
		} else if (args[1].equalsIgnoreCase("2") && args.length == 4) {
			LaborSharing_Production_MultiThreadExecutor.start(
					Integer.parseInt(args[2]), Integer.parseInt(args[3]));
		} else {
			System.out.println("The procedure code is wrong!");
		}

		// Remember the finish time
		long finishTime = new Date().getTime();
		System.out
				.println("Finished in: "
						+ String.format(
								"%d min, %d sec",
								TimeUnit.MILLISECONDS.toMinutes(finishTime
										- startTime),
								TimeUnit.MILLISECONDS.toSeconds(finishTime
										- startTime)
										- TimeUnit.MINUTES
												.toSeconds(TimeUnit.MILLISECONDS
														.toMinutes(finishTime
																- startTime))));
	}

	/**
	 * Create 'world' and export it to disk
	 * 
	 * @throws IOException
	 */
	public static void prepareData() throws IOException {
		/*
		 * Prepare the ward, landcover and road distance raster
		 */
		WardLevelRasterProcessing.process();

		/*
		 * Read the Farmers' data to create Household agents
		 */
		List<FarmerRecord> farmerRecords = readFarmerRegister(ZambiaMultiThreadSimulator.syntheticPopulationFile);
		Map<Integer, List<FarmerRecord>> agentGroupedByWard = new HashMap<Integer, List<FarmerRecord>>();

		// group records based on the cultArea
		for (FarmerRecord record : farmerRecords) {
			int wardId = Integer.parseInt(record.attributes.get("Ward"));
			// int cultArea =
			// Integer.parseInt(record.attributes.get("CultArea"));
			int hhSize = Integer.parseInt(record.attributes.get("HHSize"));

			// record.attributes.put("MaizeArea",
			// String.valueOf(determineActualMaizeArea(cultArea)));
			record.attributes.put("HH_Labor",
					String.valueOf(determineHHLabor(hhSize)));
			record.attributes.put("HH_PlantingDay",
					String.valueOf(determinePlantingDay()));
			record.attributes.put("HH_CultVar",
					String.valueOf(determineCultVar()));

			if (!agentGroupedByWard.containsKey(wardId)) {
				agentGroupedByWard.put(wardId, new LinkedList<FarmerRecord>());
			}

			agentGroupedByWard.get(wardId).add(record);
		}

		/*
		 * Create HH samples and write the total number of HHs for each ward
		 * back to the summary file
		 */
		BufferedReader br = new BufferedReader(
				new FileReader(summary_file_path));
		StringBuffer sb = new StringBuffer();

		String readLine = br.readLine();// read the header line
		sb.append(readLine + "\t#HH\r\n");

		int id_index = 0;
		for (String str : readLine.split("\\s")) {
			if (str.equals("ward"))
				break;
			else
				id_index++;
		}

		while ((readLine = br.readLine()) != null) {
			int ward_id = Integer.parseInt(readLine.split("\\s")[id_index]);
			if (!agentGroupedByWard.containsKey(ward_id)) {
				System.out.println("Ward " + ward_id
						+ " does not have any farmer records!");
				continue;
			}

			/*
			 * Write each household agent into an input file
			 */
			BufferedWriter bw = new BufferedWriter(new FileWriter("tmp/HH_"
					+ ward_id + ".txt"));
			List<String> attributeNames = new ArrayList<String>();
			for (String attributeName : agentGroupedByWard.get(ward_id).get(0).attributes
					.keySet()) {
				bw.append(attributeName + "\t");
				attributeNames.add(attributeName);
			}

			bw.newLine();

			for (FarmerRecord record : agentGroupedByWard.get(ward_id)) {
				for (String attributeName : attributeNames) {
					bw.append(record.attributes.get(attributeName) + "\t");
				}

				bw.newLine();
			}

			bw.flush();
			bw.close();

			sb.append(readLine + "\t" + agentGroupedByWard.get(ward_id).size()
					+ "\r\n");
		}

		br.close();

		BufferedWriter bw = new BufferedWriter(new FileWriter(
				summary_file_path, false));
		bw.append(sb.toString());
		bw.flush();
		bw.close();

		// Create the file of simulation instances for Big Red II
		int[] wardIds = new int[agentGroupedByWard.keySet().size()];
		int index = 0;
		for (Integer wardId : agentGroupedByWard.keySet()) {
			wardIds[index++] = wardId;
		}

		createSimulationTaskFile(wardIds);
	}

	/**
	 * Create 'world' and export it to disk
	 * 
	 * @throws IOException
	 */
	public static void prepareData_depracated() throws IOException {
		/*
		 * Partition wards and landcover raster
		 */
		WardsPartition.partition();

		/*
		 * Create the bin distribution
		 */
		TreeMap<Double, List<SingleValueBin>> mapping = BinMapping
				.createGlobalBinMapping();
		System.out.println("Mapping:");
		for (Double n : mapping.keySet()) {
			for (SingleValueBin bin : mapping.get(n)) {
				System.out.print(n + "->[" + bin.lowerBound + ", "
						+ bin.upperBound + "):" + bin.percent + "\t");
			}
			System.out.println("");
		}

		/*
		 * Read the Farmers' data to create Household agents
		 */
		List<FarmerRecord> farmerRecords = readFarmerRegister(ZambiaMultiThreadSimulator.farmerRegisterFile);
		Map<Double, List<FarmerRecord>> groupedFarmerRecords = new HashMap<Double, List<FarmerRecord>>();
		Map<Integer, List<FarmerRecord>> appendedFarmerRecords = new HashMap<Integer, List<FarmerRecord>>(
				farmerRecords.size());

		// group records based on the cultArea
		for (FarmerRecord record : farmerRecords) {
			double farmlandArea = Double.valueOf(record.attributes
					.get("CultArea"));
			double lowerBound = mapping.floorKey(farmlandArea);

			if (!groupedFarmerRecords.containsKey(lowerBound)) {
				groupedFarmerRecords.put(lowerBound,
						new LinkedList<FarmerRecord>());
			}

			groupedFarmerRecords.get(lowerBound).add(record);
		}

		// fill in the 'missing' fields in farmers' records using the
		// Bin-mapping
		for (double key : mapping.keySet()) {
			List<SingleValueBin> bins = mapping.get(key);
			List<FarmerRecord> records = groupedFarmerRecords.get(key);

			double totalPercent = 0;
			for (SingleValueBin bin : bins) {
				totalPercent += bin.percent;
			}

			if (totalPercent == 0) { // no non-empty mapping bins
				int hhSize = (int) ((bins.get(0).lowerBound + bins.get(4).upperBound) / 2); // use
																							// the
																							// mean
																							// value
				int hhLabor = BinMapping.determineHHLabor(hhSize);

				for (FarmerRecord record : records) {
					record.attributes.put("HH_Size", String.valueOf(hhSize));
					record.attributes.put("HH_Labor", String.valueOf(hhLabor));

					if (!appendedFarmerRecords.containsKey(Integer
							.parseInt(record.attributes.get("Ward")))) {
						appendedFarmerRecords
								.put(Integer.parseInt(record.attributes
										.get("Ward")),
										new LinkedList<FarmerRecord>());
					}
					appendedFarmerRecords.get(
							Integer.parseInt(record.attributes.get("Ward")))
							.add(record);
				}
			} else {
				Iterator<FarmerRecord> itr_bin = records.iterator();

				for (SingleValueBin bin : bins) {
					int numOfTotalHHs = (int) (records.size() * bin.percent / totalPercent);

					int numHHSizeCandidate = 0;
					for (int i = (int) Math.ceil(bin.lowerBound); i < bin.upperBound; i++) {
						numHHSizeCandidate++;
					}

					for (int i = (int) Math.ceil(bin.lowerBound); i < bin.upperBound; i++) {
						int numOfHHs = numOfTotalHHs / numHHSizeCandidate;
						while (numOfHHs-- > 0) {
							FarmerRecord record = itr_bin.next();
							record.attributes.put("HH_Size", String.valueOf(i));
							record.attributes.put("HH_Labor", String
									.valueOf(BinMapping.determineHHLabor(i)));

							if (!appendedFarmerRecords.containsKey(Integer
									.parseInt(record.attributes.get("Ward")))) {
								appendedFarmerRecords.put(
										Integer.parseInt(record.attributes
												.get("Ward")),
										new LinkedList<FarmerRecord>());
							}
							appendedFarmerRecords.get(
									Integer.parseInt(record.attributes
											.get("Ward"))).add(record);
						}
					}
				}
			}
		}

		/*
		 * Create HH samples and write the total number of HHs for each ward
		 * back to the summary file
		 */
		BufferedReader br = new BufferedReader(
				new FileReader(summary_file_path));
		StringBuffer sb = new StringBuffer();

		String readLine = br.readLine();// read the header line
		sb.append(readLine + "\t#HH\r\n");

		int id_index = 0;
		for (String str : readLine.split("\\s")) {
			if (str.equals("ward"))
				break;
			else
				id_index++;
		}

		while ((readLine = br.readLine()) != null) {
			int ward_id = Integer.parseInt(readLine.split("\\s")[id_index]);
			if (!appendedFarmerRecords.containsKey(ward_id)) {
				System.out.println("Ward " + ward_id
						+ " does not have any farmer records!");
				continue;
			}

			/*
			 * Write each household agent into an input file
			 */
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					"InputData/HH" + ward_id + ".txt"));
			List<String> attributeNames = new ArrayList<String>();
			for (String attributeName : appendedFarmerRecords.get(1).get(0).attributes
					.keySet()) {
				bw.append(attributeName + "\t");
				attributeNames.add(attributeName);
			}

			bw.newLine();

			for (FarmerRecord record : appendedFarmerRecords.get(ward_id)) {

				for (String attributeName : attributeNames) {
					bw.append(record.attributes.get(attributeName) + "\t");
				}

				bw.newLine();
			}

			bw.flush();
			bw.close();

			sb.append(readLine + "\t"
					+ appendedFarmerRecords.get(ward_id).size() + "\r\n");
		}

		br.close();

		BufferedWriter bw = new BufferedWriter(new FileWriter(
				summary_file_path, false));
		bw.append(sb.toString());
		bw.flush();
		bw.close();

		// Create the file of simulation instances for Big Red II
		int[] wardIds = new int[appendedFarmerRecords.keySet().size()];
		int index = 0;
		for (Integer wardId : appendedFarmerRecords.keySet()) {
			wardIds[index++] = wardId;
		}

		createSimulationTaskFile(wardIds);
	}

	protected static void createSimulationTaskFile(int[] wardIds)
			throws IOException {
		List<String> worldFiles = new LinkedList<String>();

		for (int i = 0; i < wardIds.length; i++) {
			for (int j = 0; j < HH_allocation_MultiThreadExecutor.ag_year.length; j++) {
				for (int k = 0; k < HH_allocation_MultiThreadExecutor.numSeedHH.length; k++) {
					for (int l = 0; l < HH_allocation_MultiThreadExecutor.num_HH_allocation_repetition; l++) {
						worldFiles
								.add("ward_"
										+ wardIds[i]
										+ "_numSeedHH_"
										+ HH_allocation_MultiThreadExecutor.numSeedHH[k]
										+ "_AgYear_"
										+ String.valueOf(HH_allocation_MultiThreadExecutor.ag_year[j])
										+ "_allocation_" + l + "_.txt");

					}
				}
			}
		}

		BufferedWriter bw = new BufferedWriter(new FileWriter(
				ZambiaMultiThreadSimulator.simulation_task_file_path, false));

		for (String worldFile : worldFiles) {
			for (int j = 0; j < ZambiaMultiThreadSimulator.labor_sharing_pecent.length; j++) {
				// for (int k = 0; k <
				// ZambiaMultiThreadSimulator.cultiVar.length; k++) {
				for (int l = 0; l < ZambiaMultiThreadSimulator.search_scope.length; l++) {
					for (int m = 0; m < ZambiaMultiThreadSimulator.soilType.length; m++) {
						bw.append(worldFile
								// + ","
								// + ZambiaMultiThreadSimulator.cultiVar[k]
								+ ","
								+ ZambiaMultiThreadSimulator.soilType[m]
								+ ","
								+ String.valueOf(ZambiaMultiThreadSimulator.search_scope[l])
								+ ","
								+ String.valueOf(ZambiaMultiThreadSimulator.labor_sharing_pecent[j]));
						bw.newLine();
					}
				}
				// }
			}
		}

		bw.flush();
		bw.close();
	}

	protected static List<FarmerRecord> readFarmerRegister(String file)
			throws IOException {
		List<FarmerRecord> records = new ArrayList<FarmerRecord>();
		BufferedReader br = new BufferedReader(new FileReader(file));

		String header = br.readLine();
		String[] attributeNames = header.split(",");

		String readLine = null;
		while ((readLine = br.readLine()) != null) {
			String[] attributeValues = readLine.split(",");
			FarmerRecord record = new FarmerRecord(attributeNames,
					attributeValues);
			records.add(record);
		}

		br.close();
		return records;
	}

	/**
	 * Function that calculate HHLabor from HHSize
	 */
	public static int determineHHLabor(int HHSize) {
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

	private static int determinePlantingDay() {
		double randomNumber = Math.random();

		if (randomNumber < 0.0625) {
			return 288;
		} else if (randomNumber < 0.1875) {
			return 302;
		} else if (randomNumber < 0.3125) {
			return 316;
		} else if (randomNumber < 0.50) {
			return 330;
		} else if (randomNumber < 0.6875) {
			return 344;
		} else if (randomNumber < 0.8125) {
			return 358;
		} else if (randomNumber < 0.9375) {
			return 7;
		} else {
			return 21;
		}

		// for testing purposes; to measure against DSSAT
		// if (randomNumber < 0.125) {
		// return 288;
		// } else if (randomNumber < 0.25) {
		// return 302;
		// } else if (randomNumber < 0.375) {
		// return 316;
		// } else if (randomNumber < 0.5) {
		// return 330;
		// } else if (randomNumber < 0.625) {
		// return 344;
		// } else if (randomNumber < 0.75) {
		// return 358;
		// } else if (randomNumber < 0.875) {
		// return 7;
		// } else {
		// return 21;
		// }

	}

	private static int determineCultVar() {
		double randomNumber = Math.random();

		if (randomNumber < 0.5) {
			return 1;
		} else {
			return 3;
		}
	}

	private static int determineActualMaizeArea(int cultArea) {
		// 30% are non-maize area
		int maizeArea = (int) Math.round(cultArea * 0.7);

		// roundup to 1ha, unneccessary since 0.7 -> 1
		// if (maizeArea == 0) {
		// maizeArea = 1;
		// }

		return maizeArea;
	}
}
