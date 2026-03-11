FROM maven:3.8-openjdk-11 AS builder

WORKDIR /app

# Copy POMs first for dependency caching
COPY pom.xml ./
COPY amodtaxi/pom.xml amodtaxi/
COPY amod/pom.xml amod/

# Download dependencies (cached unless POMs change)
RUN mvn dependency:go-offline -pl amodtaxi || true
RUN mvn dependency:go-offline -pl amod || true

# Copy source and build
COPY amodtaxi/ amodtaxi/
RUN cd amodtaxi && mvn clean install -DskipTests

COPY amod/ amod/
RUN cd amod && mvn clean package -DskipTests

# Runtime image
FROM openjdk:11-jre-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 python3-pip libgdal-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy built JARs
COPY --from=builder /root/.m2/repository /root/.m2/repository
COPY --from=builder /app/amod/target/ /app/amod/target/
COPY --from=builder /app/amod/pom.xml /app/amod/

# Copy Python utilities
COPY data_utils/ /app/data_utils/
RUN pip3 install --no-cache-dir -r /app/data_utils/requirements.txt

# Copy config resources
COPY amodtaxi/src/main/resources/ /app/amodtaxi/src/main/resources/

ENV WORKING_DIR=/app/simulation
RUN mkdir -p $WORKING_DIR

WORKDIR /app/simulation

ENTRYPOINT ["java", "-cp", "/app/amod/target/*:/app/amod/target/dependency/*"]
CMD ["amodeus.amod.ScenarioExecutionSequence"]
