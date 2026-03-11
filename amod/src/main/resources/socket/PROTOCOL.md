# Socket Communication Protocol

The AMoDeus socket interface allows external dispatching logic (e.g., Python)
to control the simulation via TCP/IP on port **9382**.

## Message Format

All messages are newline-delimited Tensor strings using `{...}` braces.

## Handshake

1. **Client -> Host**: Scenario name
   ```
   {SanFrancisco.20080518}
   ```

2. **Host -> Client**: Scenario info
   ```
   {numRequests, {{lngMin, latMin}, {lngMax, latMax}}, nominalFleetSize}
   ```

3. **Client -> Host**: Desired configuration
   ```
   {numRequestsDesired, fleetSize}
   ```

## Simulation Loop

Repeats every `dispatchPeriod` (default: 30) simulation seconds:

4. **Host -> Client**: Current status
   ```
   {time, roboTaxis, requests, scores}
   ```
   - `time`: simulation time in seconds (integer)
   - `roboTaxis`: `{{id, linkIndex, STATUS}, ...}` where STATUS is one of
     `STAY`, `DRIVETOCUSTOMER`, `DRIVEWITHCUSTOMER`, `REBALANCEDRIVE`, `OFFSERVICE`
   - `requests`: `{{id, submissionTime, fromLng, fromLat, toLng, toLat}, ...}`
   - `scores`: `{serviceQuality, efficiency, fleetSize}`

5. **Client -> Host**: Dispatch commands
   ```
   {{pickups}, {rebalances}}
   ```
   - `pickups`: `{{roboTaxiId, requestId}, ...}`
   - `rebalances`: `{{roboTaxiId, {lng, lat}}, ...}`

## Termination

6. **Host -> Client**: Empty tensor signals end of simulation
   ```
   {}
   ```

7. **Host -> Client**: Final scores
   ```
   {totalWaitingTime, totalDistanceWithCustomer, totalEmptyDistance}
   ```

## Constraints

- Each RoboTaxi may appear in at most one command per step
- Each request may be assigned at most once per step
- Coordinates use **WGS:84** in **(longitude, latitude)** order
