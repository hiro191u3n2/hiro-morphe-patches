"""Strict syntax/topology reader for the audited FP32 legacy ByteNN operator subset.

No vendor graph or weights are embedded. Decoding the BM container belongs to model_inspect.
Parsing establishes graph structure only; it is not proof of weight layout or native equivalence.
"""
from __future__ import annotations

from dataclasses import dataclass
import re


class UnsupportedGraph(ValueError):
    pass


@dataclass(frozen=True)
class Node:
    kind: str
    name: str
    parameters: tuple[int, ...]
    inputs: tuple[str, ...]
    output: str
    mode: str | None = None


@dataclass(frozen=True)
class Graph:
    input_name: str
    input_nhwc: tuple[int, int, int, int]
    checksum: int
    nodes: tuple[Node, ...]
    output_name: str


def parse_graph(text: str) -> Graph:
    """Read normalized native lines only; do not guess delimiters/unknown fields/operators."""
    if not isinstance(text, str) or not text or len(text) > 1_000_000:
        raise UnsupportedGraph("bounded graph text required")
    if "\x00" in text or "\\" in text:
        raise UnsupportedGraph("container decoder must normalize the native terminator/delimiters")
    lines = [line.split() for line in text.splitlines() if line.strip()]
    if len(lines) < 3 or len(lines) > 1026:
        raise UnsupportedGraph("graph line count")

    def integer(value: str) -> int:
        if not re.fullmatch(r"0|[1-9][0-9]{0,9}", value):
            raise UnsupportedGraph("unsupported integer field")
        return int(value)

    def name(value: str) -> str:
        if not re.fullmatch(r"[A-Za-z0-9_.]{1,256}", value):
            raise UnsupportedGraph("unsupported tensor/node name")
        return value

    header = lines[0]
    if len(header) != 3 or integer(header[0]) != 1:
        raise UnsupportedGraph("exactly one input required")
    count, checksum = integer(header[1]), integer(header[2])
    if count != len(lines) - 2 or count == 0 or checksum > 0xffffffff:
        raise UnsupportedGraph("compute count or checksum field")
    data = lines[1]
    if len(data) != 9 or data[0] != "DataV2" or data[6:] != ["4", "0", "0"]:
        raise UnsupportedGraph("audited FP32 DataV2 declaration required")
    input_name = name(data[1])
    shape = tuple(integer(v) for v in data[2:6])
    if shape[0] != 1 or any(v <= 0 or v > 4096 for v in shape[1:]):
        raise UnsupportedGraph("bounded batch-one input shape required")
    seen = {input_name}
    node_names: set[str] = set()
    nodes: list[Node] = []
    for fields in lines[2:]:
        if len(fields) < 2:
            raise UnsupportedGraph("truncated node")
        kind, node_name = fields[0], name(fields[1])
        mode = None
        if kind in ("Convolution", "DepthwiseSeparableConvolution"):
            if len(fields) != 19:
                raise UnsupportedGraph("convolution field count")
            params = tuple(integer(v) for v in fields[2:17])
            if params[9:] != (4, 0, 4, 0, 4, 0):
                raise UnsupportedGraph("only the audited FP32 tensor declarations are supported")
            if not all(0 < v <= 4096 for v in params[:5]) or any(v > 4096 for v in params[5:7]):
                raise UnsupportedGraph("kernel/channel/stride/padding bound")
            if params[7] != 1 or params[8] not in (0, 1):
                raise UnsupportedGraph("audited bias/activation flags required")
            inputs, output = (name(fields[17]),), name(fields[18])
        elif kind == "Concat":
            if len(fields) < 7:
                raise UnsupportedGraph("concat field count")
            number = integer(fields[2])
            if number != 2 or len(fields) != number + 6 or fields[-2:] != ["4", "0"]:
                raise UnsupportedGraph("concat arity or FP32 fields")
            inputs = tuple(name(v) for v in fields[3:3+number])
            output = name(fields[3+number])
            params = (number,)
        elif kind == "Eltwise":
            if len(fields) != 8 or fields[5:7] != ["4", "0"] or fields[7] not in ("0", "1"):
                raise UnsupportedGraph("audited FP32 Add/optional-ReLU fields required")
            inputs, output = (name(fields[2]), name(fields[3])), name(fields[4])
            params = (integer(fields[7]),)
        elif kind == "UpSampling":
            if len(fields) != 5 or fields[4] != "BILINEAR":
                raise UnsupportedGraph("audited bilinear upsampling required")
            inputs, output, params, mode = (name(fields[2]),), name(fields[3]), (), fields[4]
        elif kind == "Tanh":
            if len(fields) != 6 or fields[4:] != ["4", "0"]:
                raise UnsupportedGraph("audited FP32 Tanh fields required")
            inputs, output, params = (name(fields[2]),), name(fields[3]), ()
        else:
            raise UnsupportedGraph("unsupported operator: " + kind)
        if node_name in node_names or output in seen or any(v not in seen for v in inputs):
            raise UnsupportedGraph("duplicate/cyclic/non-topological graph")
        nodes.append(Node(kind, node_name, params, inputs, output, mode))
        node_names.add(node_name)
        seen.add(output)
    # Reject hidden disconnected tails, including otherwise valid operators not contributing to output.
    required = {nodes[-1].output}
    for node in reversed(nodes):
        if node.output not in required:
            raise UnsupportedGraph("disconnected node")
        required.remove(node.output)
        required.update(node.inputs)
    if required != {input_name}:
        raise UnsupportedGraph("unresolved graph input")
    return Graph(input_name, shape, checksum, tuple(nodes), nodes[-1].output)
