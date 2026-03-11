/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amod;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import amodeus.amodeus.util.io.MultiFileTools;
import amodeus.amodeus.util.math.GlobalAssert;
import amodeus.amodtaxi.scenario.ScenarioCreation;
import amodeus.amodtaxi.scenario.chicago.ChicagoScenarioCreation;
import amodeus.amodtaxi.scenario.sanfrancisco.SanFranciscoScenarioCreation;
import amodeus.amodtaxi.scenario.toronto.TorontoScenarioCreation;

/** provides a quick access to the implementations of {@link ScenarioCreation}:
 * {@link ChicagoScenarioCreation}
 * {@link SanFranciscoScenarioCreation}
 * {@link TorontoScenarioCreation}
 */
public enum ScenarioCreator {
    CHICAGO {
        @Override
        public ScenarioCreation in(File workingDirectory) throws Exception {
            return ChicagoScenarioCreation.in(workingDirectory);
        }
    },
    SAN_FRANCISCO {
        @Override
        public ScenarioCreation in(File workingDirectory) throws Exception {
            return SanFranciscoScenarioCreation.of(workingDirectory, (File) null).get(0);
        }
    },
    TORONTO {
        @Override
        public ScenarioCreation in(File workingDirectory) throws Exception {
            return TorontoScenarioCreation.in(workingDirectory);
        }
    };

    /** creates a scenario with some basic settings in
     * @param workingDirectory
     * @return {@link ScenarioCreation}
     * @throws Exception if scenario could not be properly created */
    public abstract ScenarioCreation in(File workingDirectory) throws Exception;

    // ---

    private static final Map<String, ScenarioCreator> SCENARIOS_BY_NAME = new HashMap<>();
    static {
        SCENARIOS_BY_NAME.put("chicago", CHICAGO);
        SCENARIOS_BY_NAME.put("sanfrancisco", SAN_FRANCISCO);
        SCENARIOS_BY_NAME.put("san_francisco", SAN_FRANCISCO);
        SCENARIOS_BY_NAME.put("san-francisco", SAN_FRANCISCO);
        SCENARIOS_BY_NAME.put("toronto", TORONTO);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1)
            throw new IllegalArgumentException("No scenario specified. Available: " + SCENARIOS_BY_NAME.keySet());

        String scenarioName = args[0].toLowerCase();
        ScenarioCreator creator = SCENARIOS_BY_NAME.get(scenarioName);
        if (creator == null)
            throw new IllegalArgumentException("Unknown scenario: " + scenarioName + ". Available: " + SCENARIOS_BY_NAME.keySet());

        File workingDirectory = args.length > 1 ? new File(args[1]) : MultiFileTools.getDefaultWorkingDirectory();
        ScenarioCreation scenario = creator.in(workingDirectory);
        GlobalAssert.that(scenario.directory().exists());
        System.out.println("Created a ready to use AMoDeus scenario in " + scenario.directory());
    }
}
