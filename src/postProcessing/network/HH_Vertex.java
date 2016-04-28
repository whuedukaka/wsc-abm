package postProcessing.network;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertex representing a HH in the HHs' network
 * 
 * @author Peng
 * 
 */
public class HH_Vertex {
	// HH id
	public int id;
	// average yield
	public double avg_yield;
	// area in hectare
	public double area_ha;
	// HH size
	// public double hh_size;
	// adult equivalent
	public double adult_eq;
	// 1 stands for hybrid maize
	public int is_hybrid;

	// the seed cell (primary cell) for this HH
	public int seed_x, seed_y;
	// records of labor exports
	public List<Labor_Export_Edge> exportRelations = new ArrayList<Labor_Export_Edge>();
	// records of labr imports
	public List<Labor_Export_Edge> importRelations = new ArrayList<Labor_Export_Edge>();

	public HH_Vertex(int id, double avg_yield) {
		this.id = id;
		this.avg_yield = avg_yield;
	}
}
