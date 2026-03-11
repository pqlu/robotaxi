import ast
import re
from typing import Any, List, Union
from utils.RoboTaxiStatus import RoboTaxiStatus


def tensorStringToList(string: str) -> Union[list, str, None]:
    """Turn Java Tensor string representation into a Python list."""
    assert isinstance(string, str)
    # strip unit annotations like [m], [s], etc.
    s = re.sub(r" ?\[[^\]]+\]", "", string)
    # convert Tensor braces to Python list brackets
    s = s.replace('{', '[').replace('}', ']').replace('-Infinity', "'-Infinity'")
    for status in RoboTaxiStatus.values():
        s = s.replace(status, "'%s'" % status)
    try:
        array = ast.literal_eval(s)
    except (ValueError, SyntaxError):
        raise ValueError("Unable to translate '%s'" % string)
    return _replace_status(array)


def listToTensorString(array: list) -> str:
    """Turn Python list into Java Tensor string representation."""
    assert isinstance(array, list)
    elements = [listToTensorString(el) if isinstance(el, list) else el for el in array]
    return '{%s}' % ', '.join(map(str, elements))


def _replace_status(element: Any) -> Any:
    """Recursively replace status strings with RoboTaxiStatus enum values."""
    if isinstance(element, list):
        return [_replace_status(e) for e in element]
    try:
        return RoboTaxiStatus[element]
    except KeyError:
        return element
