/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amod;

import java.io.IOException;

/** Helper class to run a default preparer and server, the typical
 * sequence of execution. */
/* package */ final class ScenarioExecutionSequence {
    private ScenarioExecutionSequence() { }

    public static void main(String[] args) throws IOException {
        // preliminary steps, e.g., setting up data structures required by operational policies
        ScenarioPreparer.main(args);
        // running the simulation
        ScenarioServer.main(args);
        // viewing results, the viewer can also connect to a running simulation
        ScenarioViewer.main(args);
    }
}
