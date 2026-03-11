/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;

import amodeus.amodeus.data.LocationSpec;
import amodeus.amodeus.data.ReferenceFrame;
import amodeus.amodeus.gfx.AmodeusComponent;
import amodeus.amodeus.gfx.AmodeusViewerFrame;
import amodeus.amodeus.gfx.ViewerConfig;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.util.io.MultiFileTools;
import amodeus.amodeus.util.matsim.NetworkLoader;
import amodeus.amodeus.virtualnetwork.core.VirtualNetworkGet;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import amodeus.amod.ext.Static;

/** the viewer allows to connect to the scenario server or to view saved simulation results. */
/* package */ final class ScenarioViewer {
    private static final Logger LOGGER = Logger.getLogger(ScenarioViewer.class);

    private ScenarioViewer() { }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        File workingDirectory = args.length > 0 ? new File(args[0]) : MultiFileTools.getDefaultWorkingDirectory();
        run(workingDirectory);
    }

    /** Execute in simulation folder to view past results or connect to simulation server
     * 
     * @param workingDirectory
     * @throws FileNotFoundException
     * @throws IOException */
    public static void run(File workingDirectory) throws FileNotFoundException, IOException {
        Static.setup();
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        /** load options */
        Config config = ConfigUtils.loadConfig(scenarioOptions.getPreparerConfigName());
        LOGGER.info("MATSim config file: " + scenarioOptions.getPreparerConfigName());
        final File outputSubDirectory = new File(config.controler().getOutputDirectory()).getAbsoluteFile();
        if (!outputSubDirectory.isDirectory())
            throw new FileNotFoundException("output directory: " + outputSubDirectory.getAbsolutePath() + " not found.");
        LOGGER.info("outputSubDirectory=" + outputSubDirectory.getAbsolutePath());
        File outputDirectory = outputSubDirectory.getParentFile();
        LOGGER.info("showing simulation results from outputDirectory=" + outputDirectory);

        /** geographic information, .e.g., coordinate system */
        LocationSpec locationSpec = scenarioOptions.getLocationSpec();
        ReferenceFrame referenceFrame = locationSpec.referenceFrame();

        /** MATSim simulation network */
        Network network = NetworkLoader.fromConfigFile(new File(workingDirectory, scenarioOptions.getString("simuConfig")));
        LOGGER.info("network loaded");
        LOGGER.info("total links " + network.getLinks().size());
        LOGGER.info("total nodes " + network.getNodes().size());

        /** initializing the viewer */
        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, referenceFrame);
        AmodeusComponent amodeusComponent = AmodeusComponent.createDefault(db, workingDirectory);

        /** virtual network layer, should not cause problems if layer does not exist */
        amodeusComponent.virtualNetworkLayer.setVirtualNetwork(VirtualNetworkGet.readDefault(network, scenarioOptions));

        /** starting the viewer */
        ViewerConfig viewerConfig = ViewerConfig.from(db, workingDirectory);
        LOGGER.info("Used viewer config: " + viewerConfig);
        AmodeusViewerFrame amodeusViewerFrame = new AmodeusViewerFrame(amodeusComponent, outputDirectory, network, scenarioOptions);
        amodeusViewerFrame.setDisplayPosition(viewerConfig.settings.coord, viewerConfig.settings.zoom);
        amodeusViewerFrame.jFrame.setSize(viewerConfig.settings.dimensions);
        amodeusViewerFrame.jFrame.setVisible(true);
    }
}
