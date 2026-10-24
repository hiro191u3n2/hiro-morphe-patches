#!/usr/bin/env python3
"""Reject corrupted copies of this release's recorded APK/QA evidence."""
from pathlib import Path
import argparse, copy, importlib.util, json

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--dist', type=Path, required=True)
args = parser.parse_args(); root = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('publication', root/'publish.py')
pub = importlib.util.module_from_spec(spec); spec.loader.exec_module(pub)
qa = json.loads((args.dist/pub.QA_NAME).read_text())
pub.validate_qa(qa)
original = json.loads((pub.SOURCE_ROOT/'src/validation/real-apk.json').read_text())
pub.validate_real_apk(original, qa['artifacts'])
count = 0

def challenge(label, original_value, mutate, validator):
    global count
    value = copy.deepcopy(original_value)
    mutate(value)
    try:
        validator(value)
    except (RuntimeError, KeyError, ValueError, TypeError):
        count += 1
        print('PASS reject '+label)
        return
    raise AssertionError('Unsafe proof was accepted: '+label)

def reject(label, mutate):
    challenge(label, original, mutate, lambda p: pub.validate_real_apk(p, qa['artifacts']))

def reject_qa(label, mutate):
    challenge(label, qa, mutate, pub.validate_qa)

def case(proof, identity='4743_standalone_native'):
    return next(c for c in proof['cases'] if c['id'] == identity)

def denied(proof, identity='4753_standalone_rejected'):
    return next(c for c in proof['rejection']['cases'] if c['id'] == identity)

reject('different original package', lambda p: p['inputs']['47.4.3'].update(package='com.ss.android.ugc.trill'))
reject('different original version', lambda p: p['inputs']['47.4.3'].update(version_name='47.5.3'))
reject('missing signature verification', lambda p: p['inputs']['47.4.3'].update(signature_verified=False))
reject('wrong original SHA', lambda p: p['inputs']['47.4.3'].update(sha256='0'*64))
reject('unperformed device claim', lambda p: p.update(physical_android_tested=True))
reject('missing aggregate supported case', lambda p: p.update(cases=[case(p)]))
reject('wrong positive case version', lambda p: case(p).update(version='47.5.3'))
reject('wrong per-case input APK', lambda p: case(p).update(input_sha256=pub.ORIGINAL_4753_APK_SHA256))
reject('removed version advertised as supported', lambda p: p['inputs'].update(p['unsupported_inputs']))
reject('missing rejection input', lambda p: p.update(unsupported_inputs={}))
reject('missing original preflight', lambda p: p.update(original_preflight={}))
reject('different tested MPP bytes', lambda p: case(p).update(artifact_sha256='0'*64))
reject('different tested MPP filename', lambda p: case(p).update(artifact=pub.COMBINED))
reject('top-level success with failed pipeline step', lambda p: (p.update(success=True), case(p).update(steps_all_success=False)))
reject('top-level success with failed patch', lambda p: (p.update(success=True), case(p).update(failed_patches=['fixture failed patch'])))
reject('unselected native patch', lambda p: case(p)['selected_patches'].pop())
reject('missing applied native patch', lambda p: case(p)['applied_patches'].pop())
reject('repeated patch substituted for another', lambda p: case(p)['applied_patches'].__setitem__(0, case(p)['applied_patches'][1]))
reject('missing privacy output audit', lambda p: case(p)['audits'].pop('privacy'))
reject('missing native output audit', lambda p: case(p)['audits'].pop('native'))
reject('wrong OneBack audit type', lambda p: case(p)['audits'].update(oneback='PASS host only'))
reject('unchanged output APK', lambda p: case(p).update(output_apk_sha256=pub.ORIGINAL_APK_SHA256))
reject('wrong upstream Settings bytes', lambda p: case(p,'4743_aggregate_settings_ja').update(upstream_sha256='0'*64))
reject('missing Japanese native settings audit', lambda p: case(p,'4743_aggregate_settings_ja')['audits'].pop('settings_japanese'))
reject('missing limitations', lambda p: p.update(limitations=[]))
reject('missing entire removed-version evidence', lambda p: p.pop('rejection'))
reject('old supported-version proof substituted for rejection', lambda p: p['rejection'].update(schema='tiktok4743-real-apk-part-v1'))
reject('rejection for wrong original', lambda p: p['rejection']['input'].update(sha256=pub.ORIGINAL_APK_SHA256))
reject('rejection on unreviewed SDK', lambda p: p['rejection'].update(patcher_sdk_sha256='0'*64))
reject('missing aggregate rejection', lambda p: p['rejection'].update(cases=[denied(p)]))
reject('rejection for stale MPP', lambda p: denied(p).update(artifact_sha256='0'*64))
reject('metadata-only rejection', lambda p: denied(p).update(forced_selection=False))
reject('unsupported version produced an APK', lambda p: denied(p).update(output_apk_created=True))
reject('unsupported version applied one feature', lambda p: denied(p).update(applied_patches=[pub.ONEBACK_NAME]))
reject('one feature not tested for rejection', lambda p: denied(p)['selected_patches'].pop())
reject('Japanese feature not rejected', lambda p: denied(p)['rejected_patches'].remove(pub.JAPANESE_NAME))
reject('dependency crash mistaken for version rejection', lambda p: denied(p).update(failure_messages=['Instagram 452 Reels Back method missing']))
reject('missing failure causes', lambda p: denied(p).update(failure_messages=[]))
reject('missing process completion', lambda p: denied(p).pop('process_exit_code'))
reject('continue-on-error substituted for individual rejection', lambda p: denied(p).update(selection_mode='batch_continue'))
reject('rejection process returned success', lambda p: denied(p).update(process_exit_code=0))
reject('rejection reported success', lambda p: denied(p).update(reported_top_level_success=True))
reject('one individual rejection attempt missing', lambda p: denied(p)['attempts'].pop())
reject('individual attempt returned success', lambda p: denied(p)['attempts'][0].update(process_exit_code=0))
reject('individual attempt reported success', lambda p: denied(p)['attempts'][0].update(reported_top_level_success=True))
reject('individual attempt produced an APK', lambda p: denied(p)['attempts'][0].update(output_apk_created=True))
reject('individual attempt applied feature', lambda p: denied(p)['attempts'][0].update(applied_patches=[pub.ONEBACK_NAME]))
reject('individual attempt rejected a different feature', lambda p: denied(p)['attempts'][0].update(rejected_patches=['wrong feature']))
reject('individual attempt cause differs from summary', lambda p: denied(p)['attempts'][0].update(failure_message='different cause'))
reject('unsafe evidence path', lambda p: denied(p).update(evidence_directory='validation/../../outside'))
reject('missing upstream integration', lambda p: p.pop('integration'))
reject('integration for wrong APK', lambda p: p['integration'].update(input_sha256=pub.ORIGINAL_4753_APK_SHA256))
reject('integration for stale MPP', lambda p: p['integration'].update(artifact_sha256='0'*64))
reject('integration used a different upstream', lambda p: p['integration'].update(upstream_sha256='0'*64))
reject('integration selected fewer upstream patches', lambda p: p['integration']['selected_patches'].pop())
reject('integration did not apply one patch', lambda p: p['integration']['applied_patches'].pop())
reject('integration swallowed one failure', lambda p: p['integration'].update(failed_patches=['Settings']))
reject('integration never rebuilt APK', lambda p: p['integration'].update(steps_all_success=False))
reject('integration lacks native Back audit', lambda p: p['integration']['audits'].pop('oneback'))
reject('integration lacks Settings/Japanese audit', lambda p: p['integration']['audits'].pop('settings_japanese'))
reject('integration used extra heap', lambda p: p['integration'].update(max_heap='4g'))
reject('integration forced compatibility', lambda p: p['integration'].update(force_compatibility=True))
reject('integration changed user options', lambda p: p['integration'].update(default_options_used=False))
reject('integration claims unknown exact user selection', lambda p: p['integration'].update(user_exact_selection_reproduced=True))
reject('integration claims Android device operation', lambda p: p['integration'].update(physical_android_tested=True))
reject_qa('build supports removed version', lambda q: q.update(supported_versions=['47.4.3','47.5.3']))
reject_qa('build omits removed-version scope', lambda q: q.pop('removed_versions'))
reject_qa('native Back regression missing', lambda q: q['host'].pop('Back4743NativeBackTest'))
reject_qa('old API-only check replaces native Back regression', lambda q: q['host'].update(Back4743NativeBackTest='PASS API names only'))
reject_qa('retired ABI runtime rejection missing', lambda q: q['host'].pop('Back4753RejectedTest'))
reject_qa('production admission SDK missing', lambda q: q['target_admission'].pop())
reject_qa('admission only checked metadata', lambda q: q['target_admission'][0].update(result='PASS metadata only'))
reject_qa('loader did not inspect dependencies', lambda q: q['metadata'][0].update(result='PASS official loader: native=6; OneBack=1; Japanese=1;'))
reject_qa('unrestricted Japanese loader', lambda q: q.update(japanese_loader_version_restricted=False))
reject_qa('unrelated runtime changed', lambda q: q.update(oneback_runtime_preservation='PASS OneBack runtime: preserved=84; changed=[Lapp/hiro/oneback/runtime/TwitterBack;]; added=[]'))
reject_qa('unrelated runtime added', lambda q: q.update(oneback_runtime_preservation='PASS OneBack runtime: preserved=84; changed=[Lapp/hiro/oneback/runtime/TikTokHome;]; added=[Lapp/hiro/oneback/runtime/TwitterUnexpected;]'))
print('PASS publication evidence rejection tests: '+str(count)+'; baseline is this release recorded proof, never synthetic success')
