#!/usr/bin/env python3
"""Adversarial controls for the .107 regional native inverse-build proof."""
from pathlib import Path
import copy
import json
import shutil

import native_identity19101 as identity


def test(source, rawGPU, rawNR, report, work):
    """Return the count only after all payload/source/proof mutations are rejected."""
    source, work = Path(source), Path(work)
    identity.need(not work.exists(), 'Fresh native negative-control directory required')
    work.mkdir(parents=True)
    fingerprint = identity.current_fingerprint(source)
    identity.verify(rawGPU, fingerprint, report, source, rawNR)
    count = 1

    def reject(action, label):
        nonlocal count
        try:
            action()
        except (AssertionError, ValueError, TypeError, KeyError, OSError, RuntimeError):
            count += 1
        else:
            raise AssertionError('Native negative control accepted: ' + label)

    for label, raw in [('GPU', rawGPU), ('NR1955', rawNR)]:
        table = identity.sections(raw)
        offsets = {0, 4, 5, 6, 16, 18, 32, 40, 58, 60, 62, len(raw) - 1}
        for row in table.values():
            start, size = row[4:6]
            if row[1] != 8 and size:
                offsets.update((start, start + size // 2, start + size - 1))
        for offset in sorted(offsets):
            mutated = bytearray(raw)
            mutated[offset] ^= 1
            payload = bytes(mutated)
            reject(lambda payload=payload, label=label: identity.verify(
                payload if label == 'GPU' else rawGPU, fingerprint, report, source,
                payload if label == 'NR1955' else rawNR), label + ' byte ' + str(offset))
        for suffix, payload in [('append', raw + b'\0'), ('truncate', raw[:-1]),
                                ('empty', b''), ('wrong-type', bytearray(raw))]:
            reject(lambda payload=payload, label=label: identity.verify(
                payload if label == 'GPU' else rawGPU, fingerprint, report, source,
                payload if label == 'NR1955' else rawNR), label + ' ' + suffix)

    for bad in ['0' * 64, '', fingerprint[:-1], fingerprint.upper(), None]:
        reject(lambda bad=bad: identity.verify(rawGPU, bad, report, source, rawNR), 'fingerprint')

    # Every reported proof field is mandatory and tied to a fixed or recomputed value.
    for key, value in report.items():
        missing = copy.deepcopy(report)
        del missing[key]
        reject(lambda missing=missing: identity.verify(rawGPU, fingerprint, missing, source, rawNR),
               'missing proof field ' + key)
        bad = copy.deepcopy(report)
        if isinstance(value, bool):
            bad[key] = not value
        elif isinstance(value, str):
            bad[key] = value + '-mutated'
        elif isinstance(value, int):
            bad[key] = value + 1
        elif isinstance(value, list):
            bad[key] = value + ['unreviewed']
        elif isinstance(value, dict):
            bad[key]['unreviewed'] = True
        else:
            bad[key] = 'unreviewed'
        reject(lambda bad=bad: identity.verify(rawGPU, fingerprint, bad, source, rawNR),
               'changed proof field ' + key)
    for key in report['native_source_sha256']:
        bad = copy.deepcopy(report)
        bad['native_source_sha256'][key] = '0' * 64
        reject(lambda bad=bad: identity.verify(rawGPU, fingerprint, bad, source, rawNR),
               'reported source SHA ' + key)
    for key in ('sha256', 'bytes', 'byte_identical_to_19106'):
        bad = copy.deepcopy(report)
        bad['nr1955_inherited'][key] = None
        reject(lambda bad=bad: identity.verify(rawGPU, fingerprint, bad, source, rawNR),
               'inherited NR field ' + key)
    for key in ('current_bytes', 'baseline_bytes'):
        bad = copy.deepcopy(report)
        bad[key] = float(bad[key])
        reject(lambda bad=bad: identity.verify(rawGPU, fingerprint, bad, source, rawNR),
               'non-integer proof byte count ' + key)
    bad = copy.deepcopy(report)
    bad['native_delta_limited_to_regional_interpolation'] = 1
    reject(lambda: identity.verify(rawGPU, fingerprint, bad, source, rawNR), 'numeric source identity flag')
    for key, value in [('bytes', float(identity.NR_BYTES)), ('byte_identical_to_19106', 1)]:
        bad = copy.deepcopy(report)
        bad['nr1955_inherited'][key] = value
        reject(lambda bad=bad: identity.verify(rawGPU, fingerprint, bad, source, rawNR),
               'non-exact inherited NR proof type ' + key)
    bad = copy.deepcopy(report)
    bad['reviewed_compute_manifest_delta']['excluded_runtime_java'] = {identity.DIAGNOSTIC_NAME: 'changed'}
    reject(lambda: identity.verify(rawGPU, fingerprint, bad, source, rawNR), 'reported exclusion reason')

    # The inverse is a closed exact byte transformation, not a license to
    # replace a function wholesale or discard an arbitrary marked region.
    from legacy_shader19106 import (restore_regional19107, production_text,
        REGIONAL_BEGIN, REGIONAL_END, REGIONAL_AFTER, REGIONAL_BEFORE)
    current=(source/'native1960/strong1960.comp').read_text()
    frozen=identity.baseline(source)['restored_source_utf8']['native1960/strong1960.comp']
    identity.need(restore_regional19107(current,required=True)==frozen,'Exact regional inverse changed')
    identity.need(production_text(current)==production_text(frozen),'Inherited production contract changed')
    count+=2
    start=current.index(REGIONAL_BEGIN);finish=current.index(REGIONAL_END)+len(REGIONAL_END)
    block=current[start:finish]
    inverse_mutations=[
        ('missing helper',current[:start]+current[finish:]),
        ('duplicate helper',current[:start]+block+current[start:]),
        ('missing call',current.replace(REGIONAL_AFTER,'',1)),
        ('duplicate call',current.replace(REGIONAL_AFTER,REGIONAL_AFTER*2,1)),
        ('old call restored while helper present',current.replace(REGIONAL_AFTER,REGIONAL_BEFORE,1)),
        ('changed call',current.replace('regionalWeight19107(fx)','regionalWeight19107(fy)',1)),
        ('stray regional code',current+'\nuint unreviewed19107=0u;\n'),
        ('missing .107 delta',frozen),
    ]
    # Every helper byte participates in the pin. Mutating each non-whitespace
    # byte exercises operators, constants, masks, branches and marker tokens.
    for offset,char in enumerate(block):
        if not char.isspace():
            replacement='X' if char!='X' else 'Y'
            inverse_mutations.append(('helper byte '+str(offset),current[:start+offset]+replacement+current[start+offset+1:]))
    for label,mutated in inverse_mutations:
        reject(lambda mutated=mutated:restore_regional19107(mutated,required=True),label)
    for name in ('unchanged_preprocessed_program_sha256','reviewed_regional_preprocessed_program_sha256','reviewed_regional_inverse_program_sha256'):
        for program in report[name]:
            bad=copy.deepcopy(report);bad[name][program]='0'*64
            reject(lambda bad=bad:identity.verify(rawGPU,fingerprint,bad,source,rawNR),'program proof '+name+'/'+program)

    copied = work / 'sources'
    copied.mkdir()
    manifest = identity.strict_json((source / identity.MANIFEST_PATH).read_bytes())
    names = set(manifest['native_dependencies'] + manifest['numerical_java'])
    names.update(path.name for path in source.glob('*.java'))
    names.update((identity.BASELINE_FILE, identity.MANIFEST_PATH))
    for name in sorted(names):
        target = copied / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source / name, target)
    identity.verify(rawGPU, fingerprint, report, copied, rawNR)
    count += 1

    def reject_file(name, data, label):
        target = copied / name
        original = target.read_bytes()
        try:
            target.write_bytes(data)
            reject(lambda: identity.verify_native_sources(copied), label)
        finally:
            target.write_bytes(original)

    for name in manifest['native_dependencies']:
        target = copied / name
        original = target.read_bytes()
        reject_file(name, original + b'\n/* negative control */\n', 'native source ' + name)

    manifest_cases = []

    def case(label, mutate):
        value = copy.deepcopy(manifest)
        mutate(value)
        manifest_cases.append((label, (json.dumps(value, indent=2) + '\n').encode()))

    case('missing diagnostic exclusion', lambda value: value['excluded_runtime_java'].pop(identity.DIAGNOSTIC_NAME))
    case('changed diagnostic exclusion', lambda value: value['excluded_runtime_java'].__setitem__(identity.DIAGNOSTIC_NAME, 'Numerical processing.'))
    case('additional exclusion', lambda value: value['excluded_runtime_java'].__setitem__('Unreviewed.java', 'Diagnostics.'))
    old_exclusion = next(name for name in manifest['excluded_runtime_java'] if name != identity.DIAGNOSTIC_NAME)
    case('changed existing exclusion', lambda value: value['excluded_runtime_java'].__setitem__(old_exclusion, 'changed'))
    case('deleted existing exclusion', lambda value: value['excluded_runtime_java'].pop(old_exclusion))
    case('schema', lambda value: value.__setitem__('schema', 'unreviewed'))
    case('discovery policy', lambda value: value.__setitem__('runtime_discovery', 'unreviewed'))
    case('additional manifest field', lambda value: value.__setitem__('unreviewed', True))
    for name in ('native_dependencies', 'numerical_java'):
        case(name + ' removal', lambda value, name=name: value[name].pop())
        case(name + ' addition', lambda value, name=name: value[name].append('unreviewed.java'))
        case(name + ' duplicate', lambda value, name=name: value[name].append(value[name][0]))
        case(name + ' order', lambda value, name=name: value[name].reverse())
    original_manifest = (copied / identity.MANIFEST_PATH).read_bytes()
    duplicate = original_manifest.decode().rstrip()
    duplicate = duplicate[:-1] + ',"schema":"ulike1990-compute-dependencies-v1"}\n'
    manifest_cases.append(('duplicate JSON key', duplicate.encode()))
    manifest_cases.append(('malformed JSON', b'{'))
    for label, data in manifest_cases:
        reject_file(identity.MANIFEST_PATH, data, 'manifest ' + label)

    baseline = (copied / identity.BASELINE_FILE).read_bytes()
    pins = identity.strict_json(baseline)
    reject_file(identity.BASELINE_FILE, baseline + b' ', 'baseline bytes')
    for key, value in pins.items():
        mutated = copy.deepcopy(pins)
        if isinstance(value, dict):
            mutated[key]['unreviewed'] = True
        else:
            mutated[key] = str(value) + ' changed'
        reject_file(identity.BASELINE_FILE, (json.dumps(mutated, sort_keys=True, indent=2) + '\n').encode(),
                    'baseline field ' + key)
    for name in pins['native_source_sha256']:
        mutated = copy.deepcopy(pins)
        mutated['native_source_sha256'][name] = '0' * 64
        reject_file(identity.BASELINE_FILE, (json.dumps(mutated, sort_keys=True, indent=2) + '\n').encode(),
                    'baseline native pin ' + name)

    for name in (identity.BASELINE_FILE, identity.MANIFEST_PATH, manifest['native_dependencies'][0]):
        target = copied / name
        original = target.read_bytes()
        link_source = work / ('symlink-' + target.name)
        link_source.write_bytes(original)
        target.unlink()
        try:
            target.symlink_to(link_source.resolve())
            reject(lambda: identity.verify_native_sources(copied), 'symlink ' + name)
        finally:
            target.unlink()
            target.write_bytes(original)
    identity.verify(rawGPU, fingerprint, report, copied, rawNR)
    count += 1
    identity.need(count > 0, 'Native negative controls absent')
    return count
