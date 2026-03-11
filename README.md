# amod-toronto
Autonomous Mobility-on-Demand (AMoD) simulation and analysis pipeline for
Toronto.

## Overview
Driverless, autonomous vehicles (AVs) pose sweeping implications for the future
of our cities and roads, from congestion to the design and usage of public
spaces. In Ontario, the Ministry of Transportation has allowed AVs to be tested
on public roads under certain conditions. Though it may be years still before
fully autonomous vehicles roam our streets, we are at a critical window of
opportunity to anticipate and plan for their impacts on our cities and citizens.
In this project, I test different operating scenarios of autonomous,
mobility-on-demand (AMoD) vehicle fleets in the City of Toronto. This pipeline
includes generating a synthetic trips dataset and using the AMoDeus library to
create, run, and view agent-based models of AV fleets. After running the simulation, I explore how metrics such as passenger wait
times and fleet travel distances vary across different operating policies and
income and age groups.

## Prerequisites
- **Java 11+**
- **Maven 3.6+**
- **Python 3.7+** with pip
- **GLPK** (optional, for linear program-based dispatchers)

## Installation

### 1. Build the scenario creation library (amodtaxi)
```bash
cd amodtaxi
mvn clean install
```

### 2. Build the simulation framework (amod)
```bash
cd amod
mvn clean package
```

### 3. Install Python dependencies
```bash
cd data_utils
pip install -r requirements.txt
```

You will need `GDAL` for the postprocessing notebook. Install via
`pip install GDAL` or `conda install GDAL`.

## Running the Simulation

### Step 1: Create a scenario
Create a working directory to store simulation results, then run scenario
creation with the city name as argument:

```bash
mkdir -p /path/to/working-directory
cd amod
mvn exec:java -Dexec.mainClass="amodeus.amod.ScenarioCreator" \
  -Dexec.args="toronto /path/to/working-directory"
```

Available scenarios: `toronto`, `chicago`, `sanfrancisco`

### Step 2: Prepare the scenario
```bash
cd /path/to/working-directory
mvn -f /path/to/amod/pom.xml exec:java \
  -Dexec.mainClass="amodeus.amod.ScenarioPreparer"
```

### Step 3: Run the simulation
```bash
mvn -f /path/to/amod/pom.xml exec:java \
  -Dexec.mainClass="amodeus.amod.ScenarioServer"
```

### Step 4 (optional): View the simulation
```bash
mvn -f /path/to/amod/pom.xml exec:java \
  -Dexec.mainClass="amodeus.amod.ScenarioViewer"
```

Or run the full pipeline (prepare + simulate + view) in sequence:
```bash
mvn -f /path/to/amod/pom.xml exec:java \
  -Dexec.mainClass="amodeus.amod.ScenarioExecutionSequence"
```

## Project Structure

```
robotaxi/
├── amod/          # Simulation framework (Java/Maven)
├── amodtaxi/      # Scenario creation library (Java/Maven)
├── data_utils/    # Data preprocessing and analysis (Python)
│   ├── input/     # Trip synthesis code and TTS datasets
│   └── output/    # Simulation results, analysis notebook, and plots
└── images/        # Demo GIFs
```

### amod
Contains code to run the scenario creation and simulation pipeline. Uses
**amodtaxi** to create the initial scenario.

### amodtaxi
Contains code and configuration for creating the MATSim/AMoDeus scenario for
Toronto.

To create your own scenario, at minimum you require:
* population (eg. taxi trips dataset)
* location specs consisting of the bounding box, EPSG code, and reference frame
center for the area of interest

The scenario settings are stored in the `src/main/resources/` folder. The
rest is a data pipeline that runs these steps: loading the map and trip dataset,
building a MATSim network, and creating a population.

### data_utils
Contains the preprocessing code for synthesizing a population/trip dataset and
postprocessing code for cleaning and plotting the results. The `input` folder
contains the trip synthesis code and all required datasets. The `output`
folder contains the AMoDeus simulation results, analysis notebook, and plots.

## Dispatching Algorithms Used
* **Global Euclidean Bipartite Matching**: Ruch, C., Horl, S. and Frazzoli, E.,
  2015. Amodeus, a simulation-based testbed for autonomous
  mobility-on-demand systems. In *2018 21st International Conference on
  Intelligent Transportation Systems (ITSC)* (pp. 3639-3644). IEEE.
* **Demand-Supply Balancing**: Maciejewski, M. and Bischoff, J.,
  2018. Large-scale microscopic simulation of taxi services. *Procedia Computer
  Science*, 52, pp.358-364.
* **Model-Free Adaptive Repositioning**: Ruch, C., Gachter, J., Hakenberg, J.
  and Frazzoli, E., 2020. The+ 1 Method: Model-Free Adaptive Repositioning
  Policies for Robotic Multi-Agent Systems. *IEEE Transactions on Network
  Science and Engineering.*

## Demo
### Global Euclidean Bipartite Matching
![GBM with fleet size = 100](images/gbm_fs_100.gif)

### Demand-Supply Balancing
![Demand-supply balancing with fleet size = 100](images/ds_fs_100.gif)

### Model-Free Adaptive Repositioning
![Model-free adaptive repositioning with fleet size = 100](images/mf_fs_100.gif)

## Sample results
Baseline results show that the choice of AV fleet policy does influence how
wait times are distributed across income groups. In particular, passengers from
lower income households tend to experience longer wait times when a fleet
rebalancing algorithm is used. Non-rebalancing policies don't show this
tendency, suggesting that the rebalancing policy could potentially exacerbate
mobility inequities between passengers in low and high income households. There
doesn't appear to be a trend in wait times or travel distances with respect to passenger age groups.

### Model-Free Adaptive Repositioning
The below results are with a fleet size of 200.

Wait times across household income | Distances across household income
:-------------------------:|:-------------------------:
![Wait Time vs. Income](data_utils/output/figures/income_w_mf_200.png) | ![Distance vs. Income ](data_utils/output/figures/income_d_mf_200.png)

Wait times across age groups | Distances across age groups
:-------------------------:|:-------------------------:
![Wait Time vs. Age](data_utils/output/figures/age_w_mf_200.png) | ![Distance vs. Age](data_utils/output/figures/age_d_mf_200.png)

### Demand-Supply Balancing
The below results are with a fleet size of 200.

Wait times across household income | Distances across household income
:-------------------------:|:-------------------------:
![Wait Time vs. Income](data_utils/output/figures/income_w_ds_200.png) | ![Distance vs. Income ](data_utils/output/figures/income_d_ds_200.png)

Wait times across age groups | Distances across age groups
:-------------------------:|:-------------------------:
![Wait Time vs. Age](data_utils/output/figures/age_w_ds_200.png) | ![Distance vs. Age](data_utils/output/figures/age_d_ds_200.png)
