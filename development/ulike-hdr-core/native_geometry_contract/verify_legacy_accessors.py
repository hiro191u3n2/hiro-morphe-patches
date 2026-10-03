#!/usr/bin/env python3
"""Audit complete pinned legacy Lua method tables, without executing vendor code.

The conclusion is limited to these two registered method tables. It does not
claim that an unknown native ABI, inherited API, or another library cannot
expose equivalent data. Such a route has not been established or tested.
"""
import argparse
import hashlib
import json
import struct
from pathlib import Path

EFFECT = 'd40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e'
TABLES = {
    'FaceMakeupV2Feature': (0x1980ef0, 0x1980f18, 0x1980f48, [
        ('initWithConfig', 0x48ccec), ('initWithFile', 0x48d028),
        ('checkResExistOrLoaded', 0x48d3e8), ('setMakeupResource', 0x48d4c0),
        ('setFeatureEnable', 0x48d624), ('getFeatureEnable', 0x48d728),
        ('setIntensity', 0x48d800), ('getIntensity', 0x48db50),
        ('setOpacity', 0x48dd24), ('play', 0x48defc),
        ('playClip', 0x48dffc), ('playClipFromTo', 0x48e13c),
        ('setHideWhenPlayEnd', 0x48e33c), ('pause', 0x48e48c),
        ('seek', 0x48e58c), ('show', 0x48e81c), ('hide', 0x48e91c),
        ('isInit', 0x48ea1c), ('setUniform', 0x48eb1c),
        ('getUniform', 0x48ecac), ('setOptimize', 0x48ee00),
        ('setUseAmazing', 0x48ef08), ('getUseAmazing', 0x48f008),
        ('getAMGScene', 0x48f0e0),
    ]),
    'FaceMakeupV2Filter': (0x1981138, 0x1981160, 0x1981190, [
        ('checkResExistOrLoaded', 0x48f2c8), ('setRTTextureFlagMakeupV2', 0x48f3a8),
        ('initialize', 0x48f4b4), ('draw', 0x48f588), ('buildParam', 0x48f6c4),
        ('setIntensity', 0x48f794), ('getIntensity', 0x48faa4),
        ('setActiveIntensity', 0x48fc78), ('setIntensityOpacity', 0x48fe4c),
        ('getActiveIntensity', 0x490008), ('setExclusive', 0x4901dc),
        ('clearExclusive', 0x49036c), ('getRequirement', 0x49043c),
        ('getOutputTexture', 0x490548), ('setHideWhenPlayEnd', 0x490628),
        ('playClip', 0x490780), ('playClipFromTo', 0x4908c4),
        ('play', 0x490ab0), ('pause', 0x490bbc), ('seek', 0x490cc8),
        ('show', 0x490f60), ('hide', 0x49106c), ('isInit', 0x491178),
        ('setUniform', 0x491284), ('getUniform', 0x491410),
        ('InitEventSystem', 0x491574), ('setOptimize', 0x4916b8),
        ('getAMGScene', 0x4917d0), ('setBackupOutputTexture', 0x4918b8),
    ]),
}


def verify_bytes(raw):
    if hashlib.sha256(raw).hexdigest() != EFFECT:
        raise ValueError('unsupported effect library')
    if raw[:6] != b'\x7fELF\x02\x01' or struct.unpack_from('<H', raw, 18)[0] != 183:
        raise ValueError('expected little-endian AArch64 ELF64')
    phoff = struct.unpack_from('<Q', raw, 32)[0]
    phsize, phcount = struct.unpack_from('<HH', raw, 54)
    segments = []
    for index in range(phcount):
        kind, _, offset, address, _, length, _, _ = struct.unpack_from(
            '<IIQQQQQQ', raw, phoff + index * phsize)
        if kind == 1:
            segments.append((address, offset, length))

    def offset(address, length):
        for address0, offset0, size in segments:
            if address0 <= address and address + length <= address0 + size:
                return offset0 + address - address0
        raise ValueError('address is not a complete file-backed range')

    def qword(address):
        return struct.unpack_from('<Q', raw, offset(address, 8))[0]

    def text(address):
        result = bytearray()
        for index in range(128):
            value = raw[offset(address + index, 1)]
            if value == 0:
                return result.decode('ascii')
            result.append(value)
        raise ValueError('unbounded registration name')

    shoff = struct.unpack_from('<Q', raw, 40)[0]
    shsize, shcount = struct.unpack_from('<HH', raw, 58)
    relocations = {}
    for index in range(shcount):
        _, kind, _, _, start, size, _, _, _, stride = struct.unpack_from(
            '<IIQQQQIIQQ', raw, shoff + index * shsize)
        if kind == 4:
            if stride != 24 or size % stride:
                raise ValueError('unexpected RELA record layout')
            for at in range(start, start + size, stride):
                address, info, addend = struct.unpack_from('<QQq', raw, at)
                if info & 0xffffffff == 1027:
                    if address in relocations:
                        raise ValueError('duplicate relative relocation')
                    relocations[address] = addend

    result = {}
    for class_name, (name_slot, table_slot, base, expected) in TABLES.items():
        name_address = qword(name_slot)
        if relocations.get(name_slot) != name_address or text(name_address) != class_name:
            raise ValueError('class name registration mismatch')
        if qword(table_slot) != base or relocations.get(table_slot) != base:
            raise ValueError('class method table pointer mismatch')
        entries = []
        for index, (name, wrapper) in enumerate(expected):
            slot = base + index * 16
            name_address = qword(slot)
            if (relocations.get(slot) != name_address or text(name_address) != name
                    or qword(slot + 8) != wrapper or relocations.get(slot + 8) != wrapper):
                raise ValueError('complete registered method list mismatch')
            entries.append({'name': name, 'wrapper_address_evidence_only': hex(wrapper)})
        terminator = base + len(expected) * 16
        if any(qword(at) != 0 or at in relocations for at in (terminator, terminator + 8)):
            raise ValueError('method list is not null-terminated at expected boundary')
        result[class_name] = {'method_count': len(expected), 'entries': entries,
                              'verified_null_terminator': hex(terminator)}
    return {
        'status': 'PASS_PINNED_COMPLETE_LEGACY_METHOD_TABLES',
        'effect_sha256': EFFECT,
        'tables': result,
        'findings': [
            'Neither registered table contains a per-vertex opacity/mesh readback method.',
            'setOpacity/setIntensityOpacity are setters; they are not observations of attOpacity.',
            'getUniform is registered, while the pinned legacy shader declares attOpacity as a vertex attribute.',
            'getAMGScene is registered but its separate pinned implementation may return null.',
        ],
        'limits': [
            'Only the two complete direct method tables are audited; this is not proof that every possible native route is unavailable.',
            'Addresses are static evidence and must not be invoked or used as guessed runtime ABI.',
            'No runtime geometry, source routing, per-face opacity, GPU state, or camera frame was observed.',
        ],
        'completion_blockers': [
            'Same-still final face-to-draw routing and material/texture/matrix state.',
            'Same-still legacy attOpacity values or a validated precision-preserving rendered equivalent.',
            'Native skin-mask sample precision, exact input/output geometry, and completed-frame ownership.',
            'Connected Android device execution and comparison for zero, single, multiple, and occluded faces.',
        ],
        'production_binding_complete': False,
        'device_execution': False,
    }


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--effect', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    report = verify_bytes(args.effect.read_bytes())
    report['verifier_sha256'] = hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
    args.output.write_text(json.dumps(report, indent=2) + '\n')
    print(report['status'])
