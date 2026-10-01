#!/usr/bin/env python3
"""Inspect the pinned model's ordinary BM-v2 graph encoding, without execution.

No model or library bytes, substitution tables, or weights are bundled. The CLI
prints a compact operator summary, never the full graph or weight values.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import struct
import sys

import model_inspector as mi

MODEL_SHA = "e64f0772bb857e4d990789237c1007b62fb86020a7edcfb0c3a61a7bedc6e2c0"
LIBRARY_SHA = "cedda347b55c03de82cc22b7f003777f40750d4f341494a29587d7b821ad7853"
GRAPH_SHA = "40e9f7491cc3be27fa63e5aef589bd9359b934403a092b75f2f2e5f3ef0e41a9"
MAX_GRAPH_BYTES = 1024 * 1024
TABLE_OFFSET = 0x1c5d31


def decode_v2(data, substitution_table):
    """Bounded implementation of the BM-v2 loader's XOR/substitution format.

    This is data decoding only. It does not load code, deserialize objects,
    execute operators, verify a license, or bypass a license check.
    """
    mi.require(len(data) >= 44 and data[:4] == b"BM\x00\x02", "unsupported BM variant")
    mi.require(mi.u32(data, 4) == len(data), "BM size mismatch")
    mi.require(mi.u32(data, 8) == 3, "only observed three-segment BM variant supported")
    ntext, ptext, nweight, pweight, tag, psalt = struct.unpack_from("<6I", data, 12)
    mi.require(0 < ntext <= MAX_GRAPH_BYTES, "empty or excessive graph size")
    mi.require(nweight >= 4 and nweight % 4 == 0, "invalid weight payload size")
    # The observed variant has contiguous metadata/text/weights/8-byte salt.
    mi.require(ptext == 36 and pweight == ptext + ntext and psalt == pweight + nweight
               and psalt + 8 == len(data), "unsupported BM segment placement")
    mi.require(len(substitution_table) == 256 and len(set(substitution_table)) == 256,
               "invalid substitution permutation")
    encoded = mi.span(data, ptext, ntext)
    salt = mi.span(data, psalt, 8)
    decoded = bytes(substitution_table[byte ^ salt[i % 8]] for i, byte in enumerate(encoded))
    mi.require(decoded[-1:] == b"\0" and b"\0" not in decoded[:-1], "invalid graph terminator")
    mi.require(all(b in (10, 13) or 32 <= b < 127 for b in decoded[:-1]), "non-text graph")
    return decoded, {"text_offset": ptext, "text_bytes": ntext,
                     "weight_offset": pweight, "weight_payload_bytes": nweight,
                     "weight_values_bytes": nweight - 4,
                     "weight_tail_marker_u32": mi.u32(data, pweight + nweight - 4),
                     "container_tag_u32_uninterpreted": tag,
                     "salt_offset": psalt, "salt_bytes": 8}


def load_pinned_model(model_path, library_path):
    """Return graph bytes and original weight span for local caller-side analysis.

    Returning these in memory is not permission to publish them. CLI output is
    deliberately restricted to summary metadata.
    """
    model = mi.read_input(model_path)
    library = mi.read_input(library_path)
    mi.require(mi.sha(model) == MODEL_SHA, "unsupported model SHA-256")
    mi.require(mi.sha(library) == LIBRARY_SHA, "unsupported library SHA-256")
    packed = mi.inspect(model, "bach", MODEL_SHA)
    group = next(g for g in packed["groups"] if g["name"] == "inference_model")
    entry = next(p for p in group["parameters"] if p["key"] == "model_name")
    mi.require(entry["type_id"] == 3, "model payload type mismatch")
    payload = entry["payload"]
    inner = mi.span(model, payload["offset"], payload["bytes"])
    table = mi.span(library, TABLE_OFFSET, 256)
    graph, metadata = decode_v2(inner, table)
    mi.require(mi.sha(graph) == GRAPH_SHA, "decoded graph SHA-256 mismatch")
    rows = [line.removesuffix("\\n").split()
            for line in graph[:-1].decode("ascii").splitlines() if line]
    mi.require(len(rows) >= 3 and len(rows[0]) == 3, "malformed graph header")
    header = [int(x) for x in rows[0]]
    mi.require(header[:2] == [1, 97] and len(rows) == 99 and rows[1][0] == "DataV2",
               "unexpected graph operator count")
    mi.require(header[2] == metadata["weight_tail_marker_u32"], "graph/weights marker mismatch")
    offset = payload["offset"] + metadata["weight_offset"]
    weights = mi.span(model, offset, metadata["weight_values_bytes"])
    # FP32 is established independently from the matching native datatype4
    # execution branches. This check only verifies representation is finite.
    import math
    mi.require(all(math.isfinite(x[0]) for x in struct.iter_unpack("<f", weights)),
               "nonfinite weight value")
    metadata.update(model_sha256=MODEL_SHA, library_sha256=LIBRARY_SHA,
                    inner_payload_sha256=mi.sha(inner), graph_sha256=GRAPH_SHA,
                    weight_values_sha256=mi.sha(weights), model_weight_offset=offset,
                    graph_marker_matches_weights=True, graph_header=header)
    return graph, weights, metadata


def summary(model_path, library_path):
    graph, weights, metadata = load_pinned_model(model_path, library_path)
    rows = [line.removesuffix("\\n").split()
            for line in graph[:-1].decode("ascii").splitlines() if line]
    counts = Counter(row[0] for row in rows[1:])
    return {"status": "PASS_PINNED_GRAPH_METADATA", **metadata,
            "input_data_row": rows[1], "last_operator": rows[-1][0],
            "last_output_name": rows[-1][3], "operator_counts_including_data": dict(counts),
            "fp32_finite_values": len(weights) // 4,
            "native_execution_verified": False,
            "limitations": ["Graph and float weight representation only; operator semantics require separate native audit.",
                            "No active-device model hash or numerical equivalence test.",
                            "No HDR or end-to-end style equivalence claim.",
                            "Full graph, original files, and weight values intentionally omitted."]}


def load_baoman(model_path, library_path):
    """Strict caller API: normalized graph text, raw FP32 bytes and metadata.

    Only the pinned graph is accepted. Its literal backslash-n row suffix and
    terminal NUL are removed for a caller-side text parser. No numeric tokens,
    row order, or weight bytes are changed. No file is written by this API.
    The shared marker match is not a claim to reproduce all native checksums.
    """
    graph, weights, metadata = load_pinned_model(Path(model_path), Path(library_path))
    rows = graph[:-1].decode("ascii").splitlines()
    mi.require(all(row.endswith("\\n") for row in rows if row),
               "unexpected pinned graph delimiter")
    normalized = "\n".join(row[:-2] for row in rows if row) + "\n"
    return {"graph_text": normalized, "weights": weights,
            "metadata": dict(metadata, weight_count=len(weights) // 4,
                             weight_dtype="float32_le",
                             graph_sha256_raw=GRAPH_SHA,
                             graph_sha256_normalized=mi.sha(normalized.encode("ascii")),
                             checksum_scope="Whole model/library/decoded graph SHA-256 pinned; graph-weight marker matched. Native aggregate checksum algorithm not reproduced.")}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("model", type=Path)
    p.add_argument("library", type=Path)
    p.add_argument("--output", type=Path)
    a = p.parse_args()
    try:
        result, code = summary(a.model, a.library), 0
    except (mi.FormatError, OSError, ValueError, StopIteration) as e:
        result, code = {"status": "REJECTED", "error": str(e)}, 1
    encoded = json.dumps(result, indent=2, allow_nan=False) + "\n"
    if a.output:
        a.output.write_text(encoded)
    else:
        print(encoded, end="")
    return code


if __name__ == "__main__":
    sys.exit(main())
