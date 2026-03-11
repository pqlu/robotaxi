package amodeus.amodtaxi.scenario.toronto;

import amodeus.amodeus.data.LocationSpec;
import amodeus.amodeus.data.LocationSpecDatabase;

public final class TorontoGeoInformation {
	private TorontoGeoInformation() { }
	
	public static void setup() {
		for (LocationSpec locationSpec : TorontoLocationSpecs.values()) {
			LocationSpecDatabase.INSTANCE.put(locationSpec);
		}
	}

}
