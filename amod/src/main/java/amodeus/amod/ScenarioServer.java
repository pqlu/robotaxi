/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amod;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.apache.log4j.Logger;

import amodeus.amodeus.dispatcher.DriveByDispatcher;
import amodeus.amodeus.analysis.Analysis;
import amodeus.amodeus.data.LocationSpec;
import amodeus.amodeus.data.ReferenceFrame;
import amodeus.amodeus.linkspeed.TaxiTravelTimeRouter;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.SimulationServer;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.util.io.MultiFileTools;
import amodeus.amodeus.util.math.GlobalAssert;
import amodeus.amodeus.util.matsim.AddCoordinatesToActivities;
import org.matsim.amodeus.AmodeusConfigurator;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.framework.AmodeusUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import amodeus.amod.analysis.CustomAnalysis;
import amodeus.amod.dispatcher.DemoDispatcher;
import amodeus.amod.dispatcher.DemoDispatcherShared;
import amodeus.amod.ext.Static;
import amodeus.amod.generator.DemoGenerator;

/** This class runs an AMoDeus simulation based on MATSim. The results can be
 * viewed if the {@link ScenarioViewer} is executed in the same working
 * directory and the button "Connect" is pressed. */
/* package */ final class ScenarioServer {
    private static final Logger LOGGER = Logger.getLogger(ScenarioServer.class);
    private static final double DEFAULT_TRAVEL_TIME_ALPHA = 0.05;
    private static final double DEFAULT_TYPICAL_DURATION = 3600.0;

    private ScenarioServer() { }

    public static void main(String[] args) throws IOException {
        File workingDirectory = args.length > 0 ? new File(args[0]) : MultiFileTools.getDefaultWorkingDirectory();
        simulate(workingDirectory);
    }

    /** Runs a simulation using input data from Amodeus.properties, av.xml and MATSim config.xml.
     *
     * @param workingDirectory directory containing simulation config files
     * @throws IOException if config files cannot be read */
    public static void simulate(File workingDirectory) throws IOException {
        Static.setup();
        LOGGER.info(Static.glpInfo());

        /** Load the properties file */
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        /** set to true in order to make server wait for at least 1 client, for
         * instance viewer client, for false the ScenarioServer starts the simulation
         * immediately */
        boolean waitForClients = scenarioOptions.getBoolean("waitForClients");
        File configFile = new File(scenarioOptions.getPreparerConfigName());

        /** geographic information */
        LocationSpec locationSpec = scenarioOptions.getLocationSpec();
        ReferenceFrame referenceFrame = locationSpec.referenceFrame();

        /** open server port for clients to connect to */
        SimulationServer.INSTANCE.startAcceptingNonBlocking();
        SimulationServer.INSTANCE.setWaitForClients(waitForClients);

        /** load MATSim configs - including av.xml configurations, load routing packages */
        LOGGER.info("Loading config");
        GlobalAssert.that(configFile.exists());
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(DEFAULT_TRAVEL_TIME_ALPHA);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AmodeusConfigGroup(), dvrpConfigGroup);
        config.planCalcScore().addActivityParams(new ActivityParams("activity"));

        config.qsim().setStartTime(0.0);
        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);

        /** MATSim requires typicalDuration on activities. For scenarios generated from
         * taxi data, a default of 1 hour is used. */
        for (ActivityParams activityParams : config.planCalcScore().getActivityParams())
            activityParams.setTypicalDuration(DEFAULT_TYPICAL_DURATION);

        /** output directory for saving results */
        String outputdirectory = config.controler().getOutputDirectory();

        /** load MATSim scenario for simulation */
        LOGGER.info("Loading scenario");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        AddCoordinatesToActivities.run(scenario);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        GlobalAssert.that(Objects.nonNull(network));
        GlobalAssert.that(Objects.nonNull(population));

        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, referenceFrame);
        Controler controller = new Controler(scenario);
        AmodeusConfigurator.configureController(controller, db, scenarioOptions);

        /** Register dispatchers - select the active dispatcher in av.xml */
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.registerDispatcherFactory(binder(),
                        DemoDispatcher.class.getSimpleName(), DemoDispatcher.Factory.class);
            }
        });
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.registerDispatcherFactory(binder(),
                        DemoDispatcherShared.class.getSimpleName(), DemoDispatcherShared.Factory.class);
            }
        });

        /** With the subsequent lines, additional user-defined initial placement logic called
         * generator is added,
         * functionality in class DemoGenerator. As long as the generator is not selected in the
         * file av.xml,
         * it is not used in the simulation. */
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.registerGeneratorFactory(binder(), "DemoGenerator", DemoGenerator.Factory.class);
            }
        });

        /** With the subsequent lines, another custom router is added apart from the
         * {@link DefaultAStarLMRouter},
         * it has to be selected in the av.xml file with the lines as follows:
         * <operator id="op1">
         * <param name="routerName" value="DefaultAStarLMRouter" />
         * <generator strategy="PopulationDensity">
         * ...
         *
         * otherwise the normal {@link DefaultAStarLMRouter} will be used. */
        /** Custom router that ensures same network speeds as taxis in original data set. */
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(TaxiTravelTimeRouter.Factory.class);
                AmodeusUtils.bindRouterFactory(binder(), TaxiTravelTimeRouter.class.getSimpleName()).to(TaxiTravelTimeRouter.Factory.class);
            }
        });

        /** run simulation */
        controller.run();

        /** close port for visualization */
        SimulationServer.INSTANCE.stopAccepting();

        /** perform analysis of simulation, a demo of how to add custom analysis methods
         * is provided in the package amod.demo.analysis */
        Analysis analysis = Analysis.setup(scenarioOptions, new File(outputdirectory), network, db);
        CustomAnalysis.addTo(analysis);
        analysis.run();
    }
}
