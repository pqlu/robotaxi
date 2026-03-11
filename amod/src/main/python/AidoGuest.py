import sys
from typing import Optional
from DispatchingLogic import DispatchingLogic
from utils.StringSocket import StringSocket


class AidoGuest:
    """
    AidoGuest is a simple demo client that interacts with AidoHost.
    """

    SCENARIO = 'SanFrancisco.20080518'
    REQUEST_NUMBER_DESIRED = 500
    NUMBER_OF_VEHICLES = 20
    PRINT_SCORE_PERIOD = 200

    def __init__(self, ip: str = 'localhost'):
        """
        :param ip: for instance "localhost"
        """
        self.ip = ip

    def run(self) -> None:
        """connect to AidoHost and run the dispatching loop"""
        string_socket = StringSocket(self.ip)
        string_socket.writeln('{%s}' % self.SCENARIO)

        # receive scenario info: bounding box and number of requests
        num_req, bbox, nominal_fleet_size = string_socket.read_line()
        bottom_left, top_right = bbox

        assert self.REQUEST_NUMBER_DESIRED <= num_req
        print("Nominal fleet size:", nominal_fleet_size)
        print("Chosen fleet size: ", self.NUMBER_OF_VEHICLES)

        config_size = [self.REQUEST_NUMBER_DESIRED, self.NUMBER_OF_VEHICLES]
        string_socket.writeln(config_size)

        dispatching_logic = DispatchingLogic(bottom_left, top_right)

        count = 0
        while True:
            status = string_socket.read_line()
            if status == '':
                raise IOError("server terminated prematurely")
            elif not status:
                break
            else:
                count += 1
                score = status[3]
                if count % self.PRINT_SCORE_PERIOD == 0:
                    print("score = %s at %s" % (score, status[0]))

                command = dispatching_logic.of(status)
                string_socket.writeln(command)

        # receive final performance scores
        final_scores = string_socket.read_line()
        print("final service quality score:  ", final_scores[1])
        print("final efficiency score:       ", final_scores[2])
        print("final fleet size score:       ", final_scores[3])

        string_socket.close()


if __name__ == '__main__':
    try:
        aido_guest = AidoGuest(sys.argv[1])
    except IndexError:
        aido_guest = AidoGuest()
    aido_guest.run()
