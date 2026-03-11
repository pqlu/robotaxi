from enum import Enum
from typing import List


class RoboTaxiStatus(Enum):
    DRIVEWITHCUSTOMER = 'DRIVEWITHCUSTOMER'
    DRIVETOCUSTOMER = 'DRIVETOCUSTOMER'
    REBALANCEDRIVE = 'REBALANCEDRIVE'
    STAY = 'STAY'
    OFFSERVICE = 'OFFSERVICE'

    @staticmethod
    def values() -> List[str]:
        return [e.value for e in RoboTaxiStatus]
