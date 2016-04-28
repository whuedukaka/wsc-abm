package postProcessing.network;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Network of HH vertices and edges that represent the sharing activities.
 * 
 * @author Peng
 * 
 */
public class Labor_Export_Network {
	// map of all HHs
	public TreeMap<Integer, HH_Vertex> HHs = new TreeMap<Integer, HH_Vertex>();
	// list of all edges
	public List<Labor_Export_Edge> Relations = new ArrayList<Labor_Export_Edge>();

	/**
	 * Add a new HH vertex into the network
	 * 
	 * @param id
	 * @param avg_yield
	 * @return the new HH vertex
	 */
	public HH_Vertex addHH(int id, double avg_yield) {
		if (!HHs.containsKey(id)) {
			HH_Vertex v = new HH_Vertex(id, avg_yield);
			this.HHs.put(id, v);
			return v;
		} else {
			HH_Vertex v = this.HHs.get(id);
			v.avg_yield = avg_yield;
			return v;
		}
	}

	public HH_Vertex getHH(int id) {
		return this.HHs.get(id);
	}

	public int getMaxID() {
		return HHs.lastKey();
	}

	/**
	 * Add an edge between two HH vertices
	 * 
	 * @param source
	 * @param dest
	 * @param weight
	 *            amount of shared labor
	 * @return
	 */
	public boolean addRelation(int source, int dest, double weight) {
		if (!HHs.containsKey(source)) {
			this.HHs.put(source, new HH_Vertex(source, -1));
		}

		if (!HHs.containsKey(dest)) {
			this.HHs.put(dest, new HH_Vertex(dest, -1));
		}

		Labor_Export_Edge rel = new Labor_Export_Edge(HHs.get(source),
				HHs.get(dest), weight);
		Relations.add(rel);
		HHs.get(source).exportRelations.add(rel);
		HHs.get(dest).importRelations.add(rel);

		return true;
	}

	/**
	 * Return the edge from source vertex to dest vertex
	 * 
	 * @param source
	 * @param dest
	 * @return
	 */
	public Labor_Export_Edge getRelation(int source, int dest) {
		if (HHs.containsKey(source)) {
			for (Labor_Export_Edge rel : HHs.get(source).exportRelations) {
				if (rel.dest == HHs.get(dest))
					return rel;
			}
		}

		return null;
	}
}
