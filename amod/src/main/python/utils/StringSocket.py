import socket
from typing import Union
from utils import Translator


class StringSocket:
    """Socket wrapper for line-based string communication with the AMoDeus server."""

    DEFAULT_PORT = 9382
    BUFFER_SIZE = 4096

    def __init__(self, ip: str, port: int = DEFAULT_PORT):
        try:
            self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self._socket.connect((ip, port))
        except socket.error as e:
            raise IOError(f"Unable to connect to {ip}:{port}") from e
        self._buffer = ''

    def writeln(self, message: Union[str, list]) -> None:
        assert isinstance(message, (str, list)), \
            "Input %s <%s> must be str or list" % (message, type(message))
        msg = Translator.listToTensorString(message) if isinstance(message, list) else message
        self._socket.sendall(str.encode(msg + '\n'))

    def read_line(self):
        while '\n' not in self._buffer:
            data = self._socket.recv(self.BUFFER_SIZE).decode()
            if not data:
                return None
            self._buffer += data
        lines = self._buffer.split('\n')
        line = lines.pop(0)
        self._buffer = '\n'.join(lines)
        return Translator.tensorStringToList(line)

    def close(self) -> None:
        self._socket.close()
