package postProcessing.network;

/**
 * Edge between two HH vertices that expresses a labor sharing activity
 * 
 * @author Peng
 * 
 */
public class Labor_Export_Edge {
	// labor export from source to dest
	public HH_Vertex source, dest;
	// amount of labor shared
	public double labor;

	public Labor_Export_Edge(HH_Vertex source, HH_Vertex dest, double labor) {
		this.source = source;
		this.dest = dest;
		this.labor = labor;
	}
}
