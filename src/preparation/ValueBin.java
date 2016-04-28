package preparation;

public class ValueBin {
	public double[] numericAttributes;
	public double[] nominalAttributes;
	public int count;
	
	public ValueBin(double[] numericAttributes, double[] nominalAttributes, int count) {
		this.numericAttributes = numericAttributes;
		this.nominalAttributes = nominalAttributes;
		this.count = count;
	}	
}
