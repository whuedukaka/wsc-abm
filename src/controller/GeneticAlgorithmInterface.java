package controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.math3.distribution.NormalDistribution;

import netlogo.Combined_allocation_production_MultiThreadExecutor;
import preparation.FarmerRecord;
import preparation.WardLevelRasterProcessing;
import utils.Util;

public class GeneticAlgorithmInterface {
	private static String propertyFile = "simulator.properties.txt";
	private static String[] soilTypes = { "WI_FLBD007", "WI_FRAO009",
			"WI_ACLS021", "WI_CMZR003", "WI_VRZM080", "WI_GLBW752",
			"WI_VRBW446", "WI_CMTR038", "WI_ARBW401", "WI_LVUY032",
			"WI_CMTN008", "WI_FLSO001", "WI_LVLS007", "WI_PHCF014",
			"WI_CMYE107" };
	private static String outputDir = "OutputData";

	public static String[] getSoilTypes() {
		return soilTypes;
	}
	
	public static Random rnd;

	/*
	 * @param: pDayStd, the standard deviation of the normal distribution of
	 * planting days (0 - 0.167)
	 */
	public static void initializeModel(double localToHybridCU, int soilIndex,
			double pDayStd, int year, int randomSeed) throws FileNotFoundException, IOException {
		Combined_allocation_production_MultiThreadExecutor.randomSeed = randomSeed;
		rnd = new Random(randomSeed);
		
		readPropertyFile(propertyFile, soilTypes[soilIndex], year);
		prepareData(localToHybridCU, pDayStd);
	}

	// public static void createWorld() throws IOException {
	// HH_allocation_MultiThreadExecutor.start();
	// }

	// public static void runSim() throws IOException, InterruptedException {
	// LaborSharing_Production_MultiThreadExecutor.start(0, 1);
	// }

	public static void runSim() throws IOException, InterruptedException {
		Combined_allocation_production_MultiThreadExecutor.start();
	}

	public static double getAvgYield(int year, String soil) {
		double simulating_wards_yield = 0;
		double allocated_wards_area = 0;

		File dir = new File(outputDir);
		String regex = "ward_\\d+.*" + year + ".*" + soil + ".*_stats.txt";

		if (dir.isDirectory()) {
			String[] fNames = dir.list(new FilenameFilter() {
				Pattern p;

				private FilenameFilter init(String regex) {
					p = Pattern.compile(regex);
					return this;
				}

				public boolean accept(File dir, String name) {
					return p.matcher(name).matches();
				}
			}.init(regex));

			for (int i = 0; i < fNames.length; i++) {
				// System.out.println(fNames[i]);
				BufferedReader br = null;

				try {
					br = new BufferedReader(new FileReader(outputDir
							+ File.separator + fNames[i]));

					double ward_cultArea = 0;
					double unallocated_cultArea = 0;

					String readLine = null;
					while ((readLine = br.readLine()) != null) {
						if (readLine.startsWith("total yield")) {
							simulating_wards_yield += Double
									.parseDouble(readLine.split(":")[1]);
						} else if (readLine.startsWith("total area of Ag land")) {
							ward_cultArea = Double.parseDouble(readLine
									.split(":")[1]);
						} else if (readLine
								.startsWith("area of Ag land that is not allocated")) {
							unallocated_cultArea = Double.parseDouble(readLine
									.split(":")[1]);
						}
					}

					allocated_wards_area += ward_cultArea
							- unallocated_cultArea;
				} catch (IOException e) {
					if (br != null) {
						try {
							br.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}

					e.printStackTrace();
				}
			}
		}

		return simulating_wards_yield / allocated_wards_area;
	}

	public static double getYieldStandardDev(int year, String soil) {
		List<Double> yieldValues = new LinkedList<Double>();

		File dir = new File(outputDir);
		String regex = "ward_\\d+.*" + year + ".*" + soil + ".*_yield_list.txt";

		if (dir.isDirectory()) {
			String[] fNames = dir.list(new FilenameFilter() {
				Pattern p;

				private FilenameFilter init(String regex) {
					p = Pattern.compile(regex);
					return this;
				}

				public boolean accept(File dir, String name) {
					return p.matcher(name).matches();
				}
			}.init(regex));

			for (int i = 0; i < fNames.length; i++) {
				// System.out.println(fNames[i]);
				BufferedReader br = null;

				try {
					br = new BufferedReader(new FileReader(outputDir
							+ File.separator + fNames[i]));

					String readLine = null;
					while ((readLine = br.readLine()) != null) {
						yieldValues.add(Double.parseDouble(readLine));
					}
				} catch (IOException e) {
					if (br != null) {
						try {
							br.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}

					e.printStackTrace();
				}
			}
		}

		double sum = 0;
		for (double n : yieldValues) {
			sum += n;
		}

		double avg = sum / yieldValues.size();
		double variance = 0;

		for (double n : yieldValues) {
			double dev = n - avg;
			variance += dev * dev;
		}

		variance = variance / yieldValues.size();

		return Math.sqrt(variance);
	}

	public static double getKLDivergence(int year, String soil,
			double[] distribution, double[] breaks) {
		if (breaks[0] != 0) {
			// make sure the breaks start from zero
			double[] startFromZero = new double[breaks.length + 1];

			startFromZero[0] = 0;
			for (int i = 0; i < breaks.length; i++) {
				startFromZero[i + 1] = breaks[i];
			}

			breaks = startFromZero;
		}

		int[] countArray = new int[breaks.length];
		for (int i = 0; i < countArray.length; i++) {
			countArray[i] = 0;
		}

		File dir = new File(outputDir);
		String regex = "ward_\\d+.*" + year + ".*" + soil + ".*_yield_list.txt";

		if (dir.isDirectory()) {
			String[] fNames = dir.list(new FilenameFilter() {
				Pattern p;

				private FilenameFilter init(String regex) {
					p = Pattern.compile(regex);
					return this;
				}

				public boolean accept(File dir, String name) {
					return p.matcher(name).matches();
				}
			}.init(regex));

			for (int i = 0; i < fNames.length; i++) {
				// System.out.println(fNames[i]);
				BufferedReader br = null;

				try {
					br = new BufferedReader(new FileReader(outputDir
							+ File.separator + fNames[i]));

					String readLine = null;
					while ((readLine = br.readLine()) != null) {
						double yield = Double.parseDouble(readLine);

						int upperEndIndex = 1;
						for (; upperEndIndex < breaks.length; upperEndIndex++) {
							if (breaks[upperEndIndex] > yield) { // yield falls
																	// into this
																	// bin
								break;
							}
						}

						countArray[upperEndIndex - 1] = countArray[upperEndIndex - 1] + 1;
					}
				} catch (IOException e) {
					if (br != null) {
						try {
							br.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}

					e.printStackTrace();
				}
			}
		}

		double[] probDist = new double[countArray.length];
		double totalCount = 0;
		for (int n : countArray) {
			totalCount += n;
		}

		System.out.print("Simulated Yield Distribution (lower end): ");
		for (int i = 0; i < countArray.length; i++) {
			probDist[i] = countArray[i] * 1.0 / totalCount;
			System.out.print(probDist[i] + "(" + breaks[i] + ")\t");
		}
		System.out.println();

		return Util.klDivergence(probDist, distribution);
	}

	private static void readPropertyFile(String file, String soilType, int year)
			throws FileNotFoundException, IOException {
		Properties properties = new Properties();
		properties.load(new FileReader(file));

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

		/*
		 * Fill attributes for 'HH_allocation_MultiThreadExecutor'
		 */
		Combined_allocation_production_MultiThreadExecutor.num_HH_allocation_repetition = Integer
				.parseInt(properties
						.getProperty("executor.num_HH_allocation_repetition"));
		Combined_allocation_production_MultiThreadExecutor.productionModelFile = properties
				.getProperty("crop_model.path");

		String[] parameters_numSeedHH = properties.getProperty(
				"model.parameter.num_seed_HH").split("\\s");
		// String[] parameters_ag_year = properties.getProperty(
		// "model.parameter.ag_year").split("\\s");

		Combined_allocation_production_MultiThreadExecutor.numSeedHH = new int[parameters_numSeedHH.length];
		for (int i = 0; i < parameters_numSeedHH.length; i++) {
			Combined_allocation_production_MultiThreadExecutor.numSeedHH[i] = Integer
					.parseInt(parameters_numSeedHH[i]);
		}

		// HH_allocation_MultiThreadExecutor.ag_year = new
		// int[parameters_ag_year.length];
		// for (int i = 0; i < parameters_ag_year.length; i++) {
		// HH_allocation_MultiThreadExecutor.ag_year[i] = Integer
		// .parseInt(parameters_ag_year[i]);
		// }
		Combined_allocation_production_MultiThreadExecutor.ag_year = new int[1];
		Combined_allocation_production_MultiThreadExecutor.ag_year[0] = year;

		/*
		 * Fill attributes for 'LaborSharing_Production_MultiThreadExecutor'
		 */
		Combined_allocation_production_MultiThreadExecutor.NTHREDS = Integer
				.parseInt(properties.getProperty("executor.number_of_threads"));

		String[] parameters_search_scope = properties.getProperty(
				"model.parameter.search_scope").split("\\s");
		// String[] parameters_cultiVar = properties.getProperty(
		// "model.parameter.cultiVar").split("\\s");
		// String[] parameters_soilType = properties.getProperty(
		// "model.parameter.soilType").split("\\s");
		String[] labor_sharing_pecent = properties.getProperty(
				"model.parameter.labor_sharing_percent").split("\\s");

		ZambiaMultiThreadSimulator.search_scope = new int[parameters_search_scope.length];
		ZambiaMultiThreadSimulator.labor_sharing_pecent = new double[labor_sharing_pecent.length];
		// ZambiaMultiThreadSimulator.cultiVar = new
		// String[parameters_cultiVar.length];
		// ZambiaMultiThreadSimulator.soilType = new
		// String[parameters_soilType.length];
		ZambiaMultiThreadSimulator.soilType = new String[1];
		ZambiaMultiThreadSimulator.soilType[0] = soilType;

		for (int i = 0; i < parameters_search_scope.length; i++) {
			ZambiaMultiThreadSimulator.search_scope[i] = Integer
					.parseInt(parameters_search_scope[i]);
		}

		// for (int i = 0; i < parameters_cultiVar.length; i++) {
		// ZambiaMultiThreadSimulator.cultiVar[i] = parameters_cultiVar[i];
		// }

		// for (int i = 0; i < parameters_soilType.length; i++) {
		// ZambiaMultiThreadSimulator.soilType[i] = parameters_soilType[i];
		// }

		for (int i = 0; i < labor_sharing_pecent.length; i++) {
			ZambiaMultiThreadSimulator.labor_sharing_pecent[i] = Double
					.parseDouble(labor_sharing_pecent[i]);
		}

		ZambiaMultiThreadSimulator.startDay = Integer.parseInt(properties
				.getProperty("model.parameter.starting_day"));
		ZambiaMultiThreadSimulator.foodConsumRate = Integer.parseInt(properties
				.getProperty("model.parameter.kg_food_per_person_month"));
		ZambiaMultiThreadSimulator.weedingLaborLandRatio = Integer
				.parseInt(properties
						.getProperty("model.parameter.labor_land_ratio_to_weed"));
	}

	private static void prepareData(double localToHybridCU, double pDayStd)
			throws IOException {
		/*
		 * Prepare the ward, landcover and road distance raster
		 */
		WardLevelRasterProcessing.process();

		/*
		 * Read the Farmers' data to create Household agents
		 */
		List<FarmerRecord> farmerRecords = ZambiaMultiThreadSimulator
				.readFarmerRegister(ZambiaMultiThreadSimulator.syntheticPopulationFile);
		Map<Integer, List<FarmerRecord>> agentGroupedByWard = new HashMap<Integer, List<FarmerRecord>>();

		// group records based on the cultArea
		for (FarmerRecord record : farmerRecords) {
			int wardId = Integer.parseInt(record.attributes.get("Ward"));
			// int cultArea =
			// Integer.parseInt(record.attributes.get("CultArea"));
			int hhSize = Integer.parseInt(record.attributes.get("HHSize"));

			// record.attributes.put("MaizeArea",
			// String.valueOf(determineActualMaizeArea(cultArea)));
			record.attributes.put("HH_Labor", String
					.valueOf(ZambiaMultiThreadSimulator
							.determineHHLabor(hhSize)));
			record.attributes.put("HH_PlantingDay",
					String.valueOf(determinePlantingDay(pDayStd)));
			record.attributes.put("HH_CultVar",
					String.valueOf(determineCultVar(localToHybridCU)));

			if (!agentGroupedByWard.containsKey(wardId)) {
				agentGroupedByWard.put(wardId, new LinkedList<FarmerRecord>());
			}

			agentGroupedByWard.get(wardId).add(record);
		}

		/*
		 * Create HH samples and write the total number of HHs for each ward
		 * back to the summary file
		 */
		BufferedReader br = new BufferedReader(new FileReader(
				ZambiaMultiThreadSimulator.summary_file_path));
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
				ZambiaMultiThreadSimulator.summary_file_path, false));
		bw.append(sb.toString());
		bw.flush();
		bw.close();

		// Create the file of simulation instances for Big Red II
		int[] wardIds = new int[agentGroupedByWard.keySet().size()];
		int index = 0;
		for (Integer wardId : agentGroupedByWard.keySet()) {
			wardIds[index++] = wardId;
		}

		// ZambiaMultiThreadSimulator.createSimulationTaskFile(wardIds);
	}

	private static int determinePlantingDay(double std) {

		// mean 0.5, std from 0 to 0.167 (the distribution needs to contain
		// most of the data points within 0-1, and about 99.7% are within three
		// standard deviations. Thus, 0.5/3 ~= 0.167)
		NormalDistribution nd = new NormalDistribution(0.5, std);

		double randomNumber = rnd.nextDouble();
		Math.random();
		// for testing purposes; to measure against DSSAT
		if (randomNumber < nd.cumulativeProbability(0.125 * 1)) {
			return 288;
		} else if (randomNumber < nd.cumulativeProbability(0.125 * 2)) {
			return 302;
		} else if (randomNumber < nd.cumulativeProbability(0.125 * 3)) {
			return 316;
		} else if (randomNumber < nd.cumulativeProbability(0.125 * 4)) {
			return 330;
		} else if (randomNumber < nd.cumulativeProbability(0.125 * 5)) {
			return 344;
		} else if (randomNumber < nd.cumulativeProbability(0.125 * 6)) {
			return 358;
		} else if (randomNumber < nd.cumulativeProbability(0.125 * 7)) {
			return 7;
		} else {
			return 21;
		}
	}

	private static int determineCultVar(double ratio) {
		double randomNumber = rnd.nextDouble();

		if (randomNumber < ratio) {
			return 1; // OP0001
		} else {
			return 3; // HY0001
		}
	}

	public static void main(String[] args) {
		// double[] breaks = { 0, 1000, 2000, 3000, 4000, 5000, 6000, 7000,
		// 8000, 9000 };
		// int[] counts = { 1015, 714, 496, 166, 134, 63, 40, 16, 26, 4 };
		// int totalCount = 0;
		// for (int n : counts) {
		// totalCount += n;
		// }
		//
		// double[] dist = new double[counts.length];
		// for (int i = 0; i < counts.length; i++) {
		// dist[i] = counts[i] * 1.0 / totalCount;
		// System.out.println(dist[i]);
		// }
		try {
			Random rd = new Random();
			int soilIndex = rd.nextInt(15);
			soilIndex = soilIndex % soilTypes.length;
			int randomSeed = rd.nextInt();

			int year = 2011;
			initializeModel(0.5, soilIndex, 0.167, year, randomSeed);

			GeneticAlgorithmInterface.runSim();
			// System.out.println(GeneticAlgorithmInterface.getAvgYield(year,
			// soilTypes[soilIndex]));
			System.out.println(GeneticAlgorithmInterface.getYieldStandardDev(
					year, soilTypes[soilIndex]));

			double[] breaks = { 0, 1000, 2000, 3000, 4000, 5000, 6000, 7000,
					8000, 9000 };
			int[] counts = { 1015, 714, 496, 166, 134, 63, 40, 16, 26, 4 };
			int totalCount = 0;
			for (int n : counts) {
				totalCount += n;
			}

			double[] dist = new double[counts.length];
			for (int i = 0; i < counts.length; i++) {
				dist[i] = counts[i] * 1.0 / totalCount;
				System.out.println(dist[i]);
			}

			System.out.println(GeneticAlgorithmInterface.getKLDivergence(2011,
					soilTypes[soilIndex], dist, breaks));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
