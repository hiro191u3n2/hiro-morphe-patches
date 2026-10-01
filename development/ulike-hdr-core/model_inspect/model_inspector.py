#!/usr/bin/env python3
"""Bounded offline metadata inspection; never execute or convert model weights."""
import argparse
import hashlib
import json
import math
import struct
import sys
import zipfile
from pathlib import Path

MAX_BYTES = 128 * 1024 * 1024
MAX_ENTRIES = 100000
MAX_TEXT = 4096


class FormatError(ValueError):
    pass


def sha(data):
    return hashlib.sha256(data).hexdigest()


def require(ok, message):
    if not ok:
        raise FormatError(message)


def span(data, start, size, end=None):
    limit = len(data) if end is None else end
    require(0 <= start <= limit and 0 <= size <= limit - start,
            "field extends outside containing span")
    return data[start:start + size]


def u32(data, start, end=None):
    return struct.unpack("<I", span(data, start, 4, end))[0]


def name_field(data, start, limit=256):
    raw = span(data, start, limit)
    stop = raw.find(b"\0")
    require(stop >= 0, "missing bounded NUL terminator in name field")
    raw = raw[:stop]
    require(raw and all(32 <= b <= 126 for b in raw), "name is not nonempty printable ASCII")
    return raw.decode("ascii")


def payload_description(data, start, length, type_id):
    raw = span(data, start, length)
    result = {"offset": start, "bytes": length, "sha256": sha(raw)}
    # Only three parameter types are proved by the caller's native dispatch.
    # Unknown types and noncanonical sizes remain opaque and are never guessed.
    if type_id == 1 and length == 4:
        result.update(encoding="int32_le", value=struct.unpack("<i", raw)[0])
    elif type_id == 2 and length == 4:
        value = struct.unpack("<f", raw)[0]
        if math.isfinite(value):
            result.update(encoding="float32_le", value=value)
        else:
            result.update(encoding="opaque_nonfinite_float")
    elif type_id == 0 and length <= MAX_TEXT:
        try:
            value = raw.decode("utf-8")
        except UnicodeDecodeError:
            value = None
        if value is not None and all(c in "\t\n\r" or 32 <= ord(c) < 127 for c in value):
            result.update(encoding="utf8_parameter_text", value=value)
        else:
            result.update(encoding="opaque_binary_or_non_ascii_text")
    else:
        result.update(encoding="opaque_unknown_type_or_noncanonical_size")
    return result


def inspect_bach(data):
    require(len(data) >= 40, "truncated Bach header")
    require(u32(data, 0) == len(data), "Bach declared file size mismatch")
    count = u32(data, 36)
    require(0 < count <= MAX_ENTRIES, "invalid or excessive group count")
    span(data, 40, 4 * count)
    start = 40 + 4 * count
    groups = []
    total_infos = 0
    for index in range(count):
        size = u32(data, 40 + 4 * index)
        require(size >= 292, "truncated packed group header")
        span(data, start, size)
        end = start + size
        name = name_field(data, start)
        checksum = {"algorithm": "xor8", "enabled": data[4] != 0,
                    "stored": data[start + 256]}
        if checksum["enabled"]:
            computed = 0
            for byte in span(data, start + 292, size - 292):
                computed ^= byte
            checksum["computed"] = computed
            require(computed == checksum["stored"], "packed group XOR checksum mismatch")
        info_count = u32(data, start + 288, end)
        total_infos += info_count
        require(total_infos <= MAX_ENTRIES, "excessive parameter count")
        span(data, start + 292, 4 * info_count, end)
        cursor = start + 292 + 4 * info_count
        entries = []
        for entry_index in range(info_count):
            declared_size = u32(data, start + 292 + 4 * entry_index, end)
            span(data, cursor, 296, end)
            key = name_field(data, cursor)
            type_id = u32(data, cursor + 288, end)
            payload_len = u32(data, cursor + 292, end)
            require(declared_size == 296 + payload_len, "parameter size table mismatch")
            span(data, cursor + 296, payload_len, end)
            entries.append({"key": key, "offset": cursor, "bytes": declared_size,
                            "type_id": type_id,
                            "payload": payload_description(data, cursor + 296, payload_len, type_id)})
            cursor += declared_size
        require(cursor == end, "unaccounted trailing bytes in packed group")
        groups.append({"name": name, "offset": start, "bytes": size,
                       "checksum": checksum, "parameters": entries})
        start = end
    require(start == len(data), "unaccounted trailing bytes after groups")
    return {"kind": "bach_packed_buffer", "status": "metadata_structure_verified",
            "groups": groups,
            "tensor_graph_decoded": False,
            "native_model_execution_verified": False,
            "limitation": "Metadata layout only; not proof of usable weights or inference/HDR behavior."}


def inspect_legacy(data):
    require(len(data) >= 24, "truncated legacy header candidate")
    require(u32(data, 16) == len(data), "legacy declared file size mismatch")
    length = u32(data, 20)
    require(0 < length <= 255, "invalid legacy model-name length")
    raw = span(data, 24, length)
    require(all(32 <= b <= 126 for b in raw), "legacy name candidate is not printable ASCII")
    result = {"kind": "legacy_model_header_candidate", "status": "header_identified_payload_opaque",
            "name_candidate": raw.decode("ascii"), "name_offset": 24,
            "name_bytes": length, "opaque_offset": 24 + length,
            "opaque_bytes": len(data) - 24 - length,
            "limitation": "Observed header convention only; no decryption, submodel, tensor or weight parsing."}
    folded = bytes((data[n] + data[n + 8]) & 255 for n in range(8))
    if folded not in (b"v2\0\0\0\0\0\0", b"v3\0\0\0\0\0\0"):
        return result
    # libeffect 0xe3dddc reads this envelope. Its two decoder calls and
    # plaintext checksum validation are intentionally not implemented here.
    version = folded[:2].decode("ascii")
    cursor = 24 + length

    def read_word():
        nonlocal cursor
        value = u32(data, cursor)
        cursor += 4
        return value

    def read_blob():
        nonlocal cursor
        size = read_word()
        require(size <= 0x7fffffff, "negative legacy length")
        raw = span(data, cursor, size)
        info = {"offset": cursor, "bytes": size, "sha256": sha(raw)}
        cursor += size
        return info

    def read_name():
        nonlocal cursor
        size = read_word()
        require(0 < size <= MAX_TEXT, "invalid or excessive legacy name length")
        raw = span(data, cursor, size)
        cursor += size
        require(all(32 <= b < 127 for b in raw), "legacy name is not printable ASCII")
        return raw.decode("ascii")

    count = read_word()
    require(count <= MAX_ENTRIES, "excessive legacy group count")
    groups = []
    for index in range(count):
        start = cursor
        name = read_name()
        auxiliary = read_blob()
        checksum = read_word()
        encoded = read_blob()
        tail_tag = read_word()
        tail = read_blob()
        groups.append({"name": name, "offset": start, "bytes": cursor - start,
                       "opaque_auxiliary": auxiliary, "opaque_encoded_payload": encoded,
                       "opaque_tail_payload": tail, "tail_tag_u32_uninterpreted": tail_tag,
                       "decoded_payload_checksum": {"stored_u32": checksum,
                           "verified": False, "reason": "Native checksum applies after decoding; this inspector does not decode."}})
    params = []
    total = count
    if version == "v3":
        for type_id in (2, 1):
            number = read_word()
            total += number
            require(total <= MAX_ENTRIES, "excessive legacy parameter count")
            for index in range(number):
                key = read_name()
                params.append({"key": key, "type_id": type_id,
                               "payload": payload_description(data, cursor, 4, type_id)})
                cursor += 4
    require(cursor == len(data), "unaccounted trailing bytes in legacy envelope")
    return {"kind": "legacy_model_envelope", "status": "metadata_structure_verified_payload_opaque",
            "version": version, "name_candidate": result["name_candidate"],
            "name_offset": 24, "name_bytes": length, "groups": groups,
            "parameters": params, "decoded_payload_checksums_verified": False,
            "tensor_graph_decoded": False, "native_model_execution_verified": False,
            "limitation": "Outer lengths and plaintext metadata only; no decryption, decoded-payload checksum verification, tensor or weight parsing."}


def inspect_bm(data):
    require(len(data) >= 8 and data[:4] == b"BM\x00\x02", "unsupported or truncated BM header")
    require(u32(data, 4) == len(data), "BM declared file size mismatch")
    return {"kind": "bm_00_02_container", "status": "header_identified_payload_opaque",
            "opaque_offset": 8, "opaque_bytes": len(data) - 8,
            "limitation": "Only magic and observed total-byte field checked; contents and checksum semantics unparsed."}


def inspect(data, kind="auto", expected_sha256=None):
    require(0 < len(data) <= MAX_BYTES, "empty or oversized input")
    digest = sha(data)
    if expected_sha256 is not None:
        require(len(expected_sha256) == 64 and digest == expected_sha256.lower(), "SHA-256 mismatch")
    if kind == "auto":
        if data[:4] == b"BM\x00\x02":
            kind = "bm"
        elif len(data) >= 24 and u32(data, 0) == len(data) and u32(data, 16) == len(data):
            return {"kind": "ambiguous", "status": "not_decoded", "bytes": len(data),
                    "sha256": digest, "candidate_kinds": ["bach", "legacy"],
                    "limitation": "Size-field heuristics conflict; choose --kind explicitly after independent identification."}
        elif len(data) >= 24 and u32(data, 16) == len(data):
            kind = "legacy"
        elif len(data) >= 4 and u32(data, 0) == len(data):
            kind = "bach"
        else:
            return {"kind": "unknown", "status": "not_decoded", "bytes": len(data), "sha256": digest}
    require(kind in ("bach", "legacy", "bm"), "unsupported kind")
    result = {"bach": inspect_bach, "legacy": inspect_legacy, "bm": inspect_bm}[kind](data)
    return dict(result, bytes=len(data), sha256=digest)


def read_input(path, member=None):
    if member is None:
        require(0 < path.stat().st_size <= MAX_BYTES, "empty or oversized file")
        with path.open("rb") as stream:
            data = stream.read(MAX_BYTES + 1)
    else:
        with zipfile.ZipFile(path) as archive:
            matches = [info for info in archive.infolist() if info.filename == member]
            require(len(matches) == 1, "ZIP member missing or duplicated")
            info = matches[0]
            require(not info.is_dir() and not info.flag_bits & 1, "directory/encrypted ZIP member unsupported")
            require(0 < info.file_size <= MAX_BYTES, "empty or oversized ZIP member")
            with archive.open(info) as stream:
                data = stream.read(MAX_BYTES + 1)  # zipfile validates CRC at EOF.
            require(len(data) == info.file_size, "ZIP member size mismatch")
    require(0 < len(data) <= MAX_BYTES, "empty or oversized read")
    return data


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path)
    parser.add_argument("--member", help="Exact ZIP member name; no archive extraction occurs")
    parser.add_argument("--kind", choices=("auto", "bach", "legacy", "bm"), default="auto")
    parser.add_argument("--sha256", help="Optional trusted expected SHA-256")
    args = parser.parse_args()
    try:
        result = inspect(read_input(args.input, args.member), args.kind, args.sha256)
        code = 2 if result["status"] == "not_decoded" else 0
    except (FormatError, OSError, zipfile.BadZipFile, RuntimeError) as error:
        result, code = {"status": "REJECTED", "error": str(error)}, 1
    print(json.dumps(result, ensure_ascii=True, indent=2, allow_nan=False))
    return code


if __name__ == "__main__":
    sys.exit(main())
