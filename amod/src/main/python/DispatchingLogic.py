import random
from typing import List, Tuple
from utils.RoboTaxiStatus import RoboTaxiStatus


class DispatchingLogic:
    """
    Dispatching logic for the AidoGuest demo. Computes dispatching instructions
    that are forwarded to the AidoHost.
    """
    DISPATCH_INTERVAL_SECONDS = 60

    def __init__(self, bottom_left: List[float], top_right: List[float]):
        """
        :param bottom_left: [longitude_min, latitude_min]
        :param top_right: [longitude_max, latitude_max]
        """
        self.lng_min = bottom_left[0]
        self.lng_max = top_right[0]
        self.lat_min = bottom_left[1]
        self.lat_max = top_right[1]

        print("minimum longitude in network: ", self.lng_min)
        print("maximum longitude in network: ", self.lng_max)
        print("minimum latitude  in network: ", self.lat_min)
        print("maximum latitude  in network: ", self.lat_max)

        self.matched_requests: set = set()
        self.matched_taxis: set = set()

    def of(self, status: list) -> List[list]:
        assert isinstance(status, list)
        pickup: List[list] = []
        rebalance: List[list] = []

        time = status[0]
        if time % self.DISPATCH_INTERVAL_SECONDS == 0:
            index = 0

            # sort requests by submission time
            requests = sorted(status[2].copy(), key=lambda request: request[1])

            # for each unassigned request, find an available taxi in STAY mode
            for request in requests:
                if request[0] not in self.matched_requests:
                    while index < len(status[1]):
                        robo_taxi = status[1][index]
                        if robo_taxi[2] is RoboTaxiStatus.STAY:
                            pickup.append([robo_taxi[0], request[0]])
                            self.matched_requests.add(request[0])
                            self.matched_taxis.add(robo_taxi[0])
                            index += 1
                            break
                        index += 1

            # rebalance one of the remaining unmatched STAY taxis
            for robo_taxi in status[1]:
                if robo_taxi[2] is RoboTaxiStatus.STAY and robo_taxi[0] not in self.matched_taxis:
                    rebalance_location = self._get_random_rebalance_location()
                    rebalance.append([robo_taxi[0], rebalance_location])
                    break

        return [pickup, rebalance]

    def _get_random_rebalance_location(self) -> List[float]:
        """
        AMoDeus internally uses the convention (longitude, latitude) for a WGS:84 pair.
        """
        return [random.uniform(self.lng_min, self.lng_max),
                random.uniform(self.lat_min, self.lat_max)]
