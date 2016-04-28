package preparation;

import java.util.HashMap;
import java.util.Map;

public class FarmerRecord {
	public Map<String, String> attributes = new HashMap<String, String>();;

	public FarmerRecord(String[] attributeNames, String[] attributeValues) {
		for (int i = 0; i < attributeNames.length; i++) {
			// attributes read from CSV file may have double quotes on it
			// remove the double quotes
			if (attributeNames[i].startsWith("\"")
					&& attributeNames[i].endsWith("\"")) {
				attributeNames[i] = attributeNames[i].substring(
						Math.min(1, attributeNames[i].length() - 1),
						attributeNames[i].length() - 1);
			}

			if (attributeValues[i].startsWith("\"")
					&& attributeValues[i].endsWith("\"")) {
				attributeValues[i] = attributeValues[i].substring(
						Math.min(1, attributeValues[i].length() - 1),
						attributeValues[i].length() - 1);
			}

			attributes.put(attributeNames[i], attributeValues[i]);
		}
	}
}
