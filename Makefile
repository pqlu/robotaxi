WORKING_DIR ?= $(CURDIR)/simulation
SCENARIO ?= toronto
AMOD_DIR = $(CURDIR)/amod
AMODTAXI_DIR = $(CURDIR)/amodtaxi

.PHONY: all build build-amodtaxi build-amod create prepare simulate view analyze clean help

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

all: build ## Build both Java modules

build: build-amodtaxi build-amod ## Build amodtaxi then amod

build-amodtaxi: ## Build and install amodtaxi
	cd $(AMODTAXI_DIR) && mvn clean install -DskipTests

build-amod: ## Build amod (requires amodtaxi)
	cd $(AMOD_DIR) && mvn clean package -DskipTests

create: build ## Create a scenario (set SCENARIO=toronto|chicago|sanfrancisco)
	@mkdir -p $(WORKING_DIR)
	cd $(WORKING_DIR) && mvn -f $(AMOD_DIR)/pom.xml exec:java \
		-Dexec.mainClass="amodeus.amod.ScenarioCreator" \
		-Dexec.args="$(SCENARIO) $(WORKING_DIR)"

prepare: ## Prepare the scenario in WORKING_DIR
	cd $(WORKING_DIR) && mvn -f $(AMOD_DIR)/pom.xml exec:java \
		-Dexec.mainClass="amodeus.amod.ScenarioPreparer"

simulate: ## Run the simulation in WORKING_DIR
	cd $(WORKING_DIR) && mvn -f $(AMOD_DIR)/pom.xml exec:java \
		-Dexec.mainClass="amodeus.amod.ScenarioServer"

view: ## Launch the simulation viewer
	cd $(WORKING_DIR) && mvn -f $(AMOD_DIR)/pom.xml exec:java \
		-Dexec.mainClass="amodeus.amod.ScenarioViewer"

run: prepare simulate ## Prepare and run the full simulation pipeline

analyze: ## Run Python analysis notebooks
	cd data_utils && jupyter nbconvert --execute output/analysis.ipynb

install-python: ## Install Python dependencies
	cd data_utils && pip install -r requirements.txt

clean: ## Remove build artifacts
	cd $(AMODTAXI_DIR) && mvn clean
	cd $(AMOD_DIR) && mvn clean
