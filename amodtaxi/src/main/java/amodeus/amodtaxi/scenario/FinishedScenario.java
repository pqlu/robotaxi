/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodtaxi.scenario;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;

import amodeus.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.io.DeleteDirectory;

public final class FinishedScenario {
    private static final Logger LOGGER = Logger.getLogger(FinishedScenario.class);

    private FinishedScenario() { }

    public static void copyToDir(String processingDir, String destinDir, String... fileNames) throws IOException {
        LOGGER.info("Copying scenario from : " + processingDir);
        LOGGER.info("to :                    " + destinDir);

        File destinDirFile = new File(destinDir);
        if (destinDirFile.exists())
            DeleteDirectory.of(destinDirFile, 2, 10);
        destinDirFile.mkdir();
        GlobalAssert.that(destinDirFile.isDirectory());

        { // files from processing directory
            for (String fileName : fileNames) {
                Path source = Paths.get(processingDir, fileName);
                Path target = Paths.get(destinDir, fileName);
                try {
                    Files.copy(source, target /* , options */);
                } catch (Exception e) {
                    LOGGER.error("Failed to copy " + fileName, e);
                }
            }
        }
    }
}
