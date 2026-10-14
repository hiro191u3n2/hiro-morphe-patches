#!/usr/bin/env python3
"""Run frozen historical assertions on current production through local adapters.

Historical files are SHA-pinned and never edited. Import adapters are private to
one runner module; neither shared importlib nor shared subprocess is patched.
Compiler closures gain only the additive diagnostic declaration. Tiny historical
peers receive an explicitly identified no-op telemetry declaration, while the
new .88 integration suites compile and execute the actual diagnostic classes.
Source inverses apply only to preservation checks, never executable production.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import inspect
import json
import os
import re
import types

ROOT = Path(__file__).resolve().parent
REFERENCE_SHA = 'a2c82ad9367212a283fb4755e3d3e3ba887c66523654ba539e7782f2371122f6'
SHIM = 'tests1988/pipeline_detail/compile_only/PipelineDetail1988.java'
SHIM_SHA = '7aa6245751c4dd7b51f44c4cfc2c992b6f712c9145f95c43224199b6baa836fd'


def sha(value):
    return hashlib.sha256(value if isinstance(value, bytes) else Path(value).read_bytes()).hexdigest()


class Proxy:
    def __init__(self, original, **overrides):
        self.original = original
        self.overrides = overrides

    def __getattr__(self, name):
        return self.overrides[name] if name in self.overrides else getattr(self.original, name)


class TextInput:
    """Read-only preservation input, intentionally not usable as a javac path."""
    def __init__(self, text):
        self.text = text

    def read_text(self, *args, **kwargs):
        return self.text

    def read_bytes(self):
        return self.text.encode()


class Adapter:
    def __init__(self, source, work):
        self.source, self.work = Path(source).resolve(), Path(work).resolve()
        self.work.mkdir(parents=True, exist_ok=True)
        reference = self.source / 'tests1988/source-scope87.json'
        if sha(reference) != REFERENCE_SHA or sha(self.source / SHIM) != SHIM_SHA:
            raise AssertionError('Frozen adapter reference or compiler-only declaration differs')
        self.reference = json.loads(reference.read_text())
        self.loaded = {}
        self.events = []
        self.helpers = {}

    def helper(self, name):
        if name not in self.helpers:
            spec = importlib.util.spec_from_file_location(name + '_review88', self.source / (name + '.py'))
            value = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(value)
            self.helpers[name] = value
        return self.helpers[name]

    def restore_frontend(self, name, text):
        baseline = self.reference[name]
        if sha(text.encode()) == baseline:
            return text
        result = self.helper('host_pipeline_routes1988_detail').reviewed_source87(name, text)
        if sha(result.encode()) != baseline:
            raise AssertionError('Reviewed .88 inverse did not restore published .87: ' + name)
        self.events.append(dict(kind='preservation_only_inverse', source=name,
                                current_sha256=sha(text.encode()), restored_sha256=baseline))
        return result

    def restore_finish(self, current):
        raw = current if isinstance(current, bytes) else current.encode()
        if sha(raw) == self.reference['GpuChain1961.java']:
            return raw
        value = self.helper('host_finish_failures1988')
        result = value.reviewed_chain87_source1988(raw)
        if sha(result) != self.reference['GpuChain1961.java']:
            raise AssertionError('Finish inverse did not restore immutable .87')
        self.events.append(dict(kind='preservation_only_inverse', source='GpuChain1961.java',
                                current_sha256=sha(raw), restored_sha256=sha(result)))
        return result

    def spec(self, name, location, *args, **kwargs):
        spec = importlib.util.spec_from_file_location(name, location, *args, **kwargs)
        path = Path(location).resolve()
        if path.parent == self.source and path.name in self.reference and path.suffix == '.py':
            expected = self.reference[path.name]
            if sha(path) != expected:
                raise AssertionError('A frozen historical runner changed: ' + path.name)
            original = spec.loader
            adapter = self

            class Loader:
                def create_module(self, requested):
                    return original.create_module(requested)

                def exec_module(self, module):
                    original.exec_module(module)
                    adapter.install(module, path)

            spec.loader = Loader()
        return spec

    def load(self, name):
        spec = self.spec(name + '_retained88_' + str(len(self.loaded)), self.source / (name + '.py'))
        result = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(result)
        return result

    def compile_command(self, command):
        values = list(map(str, command))
        if not values or not (Path(values[0]).name == 'javac' or 'com.sun.tools.javac.Main' in values):
            return command
        inputs = [Path(value).resolve() for value in values if value.endswith('.java') and Path(value).is_file()]
        # Optional stage APIs did not exist in the old tiny queue peers. Add
        # methods only to those controlled peers, never a production or old
        # published Qualification implementation. Existing assertions and
        # existing peer methods remain byte-for-byte in the generated copy.
        for path in inputs:
            body = path.read_text()
            marker = re.search(r'\bclass GpuQualification1961\s*\{', body)
            if not marker or 'PERSIST_LOCK' in body or 'finishBegin1988(' in body:
                continue
            original_body = body
            metadata_edits = []
            if path.name == 'AnalysisPeers1987.java':
                peer_key = 'tests1987/analysis/AnalysisPeers1987.java'
                if sha(body.encode()) != self.reference[peer_key]:
                    raise AssertionError('The frozen analysis reservation peer changed')
                old_commit = 'Object commit(Probe probe){commits++;if(!begun||!current()){probe.close();close();return null;}pending=probe;held=false;return null;}'
                edits = (
                    ('    static final class Reservation1984 implements AutoCloseable {',
                     '    static final class QueueDecision1983 {final boolean accepted;final long requestedBytes;QueueDecision1983(boolean a,long b){accepted=a;requestedBytes=b;}}\n'
                     '    static final class Reservation1984 implements AutoCloseable {\n        final QueueDecision1983 decision;'),
                    ('Reservation1984(boolean accepted,long bytes){this.accepted=accepted;this.bytes=bytes;held=accepted;if(held)retained+=bytes;}',
                     'Reservation1984(boolean accepted,long bytes){this.accepted=accepted;this.bytes=bytes;held=accepted;if(held)retained+=bytes;decision=new QueueDecision1983(accepted,bytes);}'),
                    (old_commit, old_commit.replace('Object commit', 'QueueDecision1983 commit').replace('close();return null;', 'close();return new QueueDecision1983(false,bytes);').replace('held=false;return null;', 'held=false;return new QueueDecision1983(true,bytes);')),
                )
                for old, new in edits:
                    if body.count(old) != 1:
                        raise AssertionError('Unexpected analysis metadata adapter anchor')
                    body = body.replace(old, new)
                    metadata_edits.append((old, new))
                inverse = body
                for old, new in reversed(metadata_edits):
                    inverse = inverse.replace(new, old, 1)
                if inverse != original_body:
                    raise AssertionError('Analysis peer control changed beyond result metadata')
                marker = re.search(r'\bclass GpuQualification1961\s*\{', body)
            rejection = ('FinishControl1986.proofs.remove(key);' if 'FinishControl1986.' in body else
                         'throw new AssertionError("Unexpected typed Finish failure in unrelated historical peer");')
            addition = ('\n    static void rejectNonSpeed1988(String key,String reason){' + rejection + '}\n'
                        '    static void finishCpu1988(int stage,int mask){}\n'
                        '    static void finishBegin1988(int candidate,int phase){}\n'
                        '    static void finishStage1988(int stage){}\n'
                        '    static void finishFault1988(int fault,int mask){}\n'
                        '    static void finishEnd1988(){}\n')
            updated = body[:marker.end()] + addition + body[marker.end():]
            if updated.replace(addition, '', 1) != body:
                raise AssertionError('A historical peer method would change')
            destination = self.work / 'additive-peers' / sha(original_body.encode()) / path.name
            destination.parent.mkdir(parents=True, exist_ok=True)
            if destination.exists() and destination.read_text() != updated:
                raise AssertionError('Conflicting additive peer declaration')
            destination.write_text(updated)
            values = [str(destination) if v.endswith('.java') and Path(v).resolve() == path else v for v in values]
            self.events.append(dict(kind='additive_qualification_peer', original=str(path),
                                    original_sha256=sha(original_body.encode()), adapted_sha256=sha(updated.encode()),
                                    analysis_result_metadata_hunks=len(metadata_edits),
                                    typed_rejection_peer='remove_controlled_proof' if 'FinishControl1986.' in body else 'fail_if_called'))
        inputs = [Path(value).resolve() for value in values if value.endswith('.java') and Path(value).is_file()]
        if any(path.name == 'PipelineDetail1988.java' for path in inputs):
            return values
        # When the current production closure is already on the classpath it
        # owns the real new helper; avoid introducing a second declaration.
        classpath = []
        for option in ('-cp', '-classpath', '--class-path'):
            if option in values:
                classpath += [Path(p) for p in values[values.index(option) + 1].split(os.pathsep)]
        if any((path / 'com/hiro/ulike/PipelineDetail1988.class').is_file() for path in classpath):
            return values
        using = [p for p in inputs if 'PipelineDetail1988.' in p.read_text()]
        if not using:
            return values
        actual_timing = any(p.name == 'ProcessingTiming1947.java'
                            and p.read_bytes() == (self.source / 'ProcessingTiming1947.java').read_bytes()
                            for p in inputs)
        helper = self.source / ('PipelineDetail1988.java' if actual_timing else SHIM)
        self.events.append(dict(kind='javac_additive_declaration', actual=actual_timing,
                                source=str(helper.relative_to(self.source)), sha256=sha(helper),
                                referenced_by=[str(p) for p in using]))
        return values + [str(helper)]

    def preservation(self, original, tree):
        tree = Path(tree).resolve()
        declaration_name = 'tests1982/pipeline/preserved81.json'
        declaration = json.loads((tree / declaration_name).read_text())
        destination = self.work / ('preservation-only-' + str(len(self.events)))
        for name in set(declaration['complete_files']) | set(declaration['methods']) | {declaration_name}:
            target = destination / name
            target.parent.mkdir(parents=True, exist_ok=True)
            if target.exists() or target.is_symlink():
                raise AssertionError('Preservation-only destination already exists')
            if name in ('StrongNoise1958.java', 'QualityPipeline1932.java', 'GpuProtection1961.java', 'FastResize1933.java'):
                target.write_text(self.restore_frontend(name, (tree / name).read_text()))
            else:
                target.symlink_to(tree / name)
        return original(destination)

    def install(self, module, path):
        if getattr(module, '_retained1988_installed', False):
            return
        module._retained1988_installed = True
        self.loaded[path.name] = sha(path)
        if hasattr(module, 'importlib'):
            module.importlib = Proxy(module.importlib, util=Proxy(module.importlib.util,
                                                                   spec_from_file_location=self.spec))
        if hasattr(module, 'subprocess'):
            original = module.subprocess
            def run(command, *args, **kwargs):
                return original.run(self.compile_command(command), *args, **kwargs)
            module.subprocess = Proxy(original, run=run)
        if path.name.startswith('build') and hasattr(module, 'compile_inputs'):
            previous = module.compile_inputs
            def compile_inputs():
                values = dict(previous())
                key = 'com/hiro/ulike/PipelineDetail1988.java'
                if key in values:
                    raise AssertionError('Historical closure unexpectedly declares .88 helper')
                values[key] = self.source / 'PipelineDetail1988.java'
                self.events.append(dict(kind='actual_production_closure_addition', runner=path.name,
                                        source=key, sha256=sha(values[key])))
                return values
            module.compile_inputs = compile_inputs
        if path.name == 'host_analysis1987.py':
            previous = module.reviewed_analysis86_source1987
            module.reviewed_analysis86_source1987 = lambda text: previous(self.restore_frontend('GpuAnalysis1961.java', text))
        if path.name == 'host_protection1987.py':
            previous = module.inverse
            module.inverse = lambda current, baseline: previous(TextInput(self.restore_frontend('GpuProtection1961.java', current.read_text())), baseline)
        if path.name == 'host_finish_reservation1987.py':
            previous = module.inverse_chain1987
            module.inverse_chain1987 = lambda current, baseline: previous(self.restore_finish(current), baseline)
        if path.name == 'host_pipeline_diagnostics1982.py':
            previous = module.preservation
            module.preservation = lambda tree: self.preservation(previous, tree)
        if path.name == 'host_finish_baseline1986.py':
            self.helper('host_finish_failures1988').install_legacy88(module, self.source, self.work, self.events)


def execute(name, source, work, jdk=None, ndk=None):
    adapter = Adapter(source, Path(work) / 'compatibility88')
    runner = adapter.load(name)
    parameters = inspect.signature(runner.test).parameters
    arguments = {key: value for key, value in (('jdk', jdk), ('ndk', ndk)) if key in parameters}
    result = runner.test(source, Path(work) / 'executed', **arguments)
    if not isinstance(result, dict) or result.get('status') != 'passed' or result.get('assertions', 0) <= 0:
        raise AssertionError('Frozen assertions did not execute successfully on current production')
    for filename, digest in adapter.loaded.items():
        if sha(Path(source) / filename) != digest:
            raise AssertionError('Frozen runner changed during execution: ' + filename)
    result['retained_compatibility1988'] = dict(
        scope='Unchanged historical assertions execute current production. Inverses are preservation-only; optional compiler peers record nothing.',
        loaded_frozen_runner_sha256=adapter.loaded, events=adapter.events,
        compiler_only_detail_sha256=SHIM_SHA, adapter_sha256=sha(__file__))
    Path(work).mkdir(parents=True, exist_ok=True)
    (Path(work) / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--runner', required=True)
    parser.add_argument('--source', type=Path, default=ROOT)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    parser.add_argument('--ndk', type=Path)
    args = parser.parse_args()
    result = execute(args.runner, args.source, args.work, args.jdk, args.ndk)
    print(json.dumps(dict(status=result['status'], assertions=result['assertions'],
                          adapter_events=len(result['retained_compatibility1988']['events']))))
