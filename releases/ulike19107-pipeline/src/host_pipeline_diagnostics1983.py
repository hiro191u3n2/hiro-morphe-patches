#!/usr/bin/env python3
"""Run the complete .82 backend suite with additive Resident fixture APIs.

The historical runner, its 18 backend cases and all numeric/source preservation
contracts execute unchanged. The controlled Resident peer compile input receives
an additive .83 adapter. For preservation only, the exact reviewed capacity API
edit is inverted and must reproduce the entire pinned .82 GpuChain file. Actual
backend execution always compiles the current production source.
"""
from pathlib import Path
import hashlib
import importlib.util
import json

CHAIN_BASELINE82_SHA256 = '15326a93fab9363941830859e4a6340fc92d1258bf618c854bd5ef79cfdea6a9'


def inverse_chain_capacity1983(current, baseline):
    """Exact reviewed inverse, not method exclusion or an updated expected hash."""
    if hashlib.sha256(baseline).hexdigest() != CHAIN_BASELINE82_SHA256:
        raise AssertionError('Frozen .82 capacity baseline does not match its reviewed complete-file pin')
    edits = [
        ('final GpuPolicy1960.GeometryData geometry;final int[] slots;final long[] capacities,uploadBytes1983;',
         'final GpuPolicy1960.GeometryData geometry;final int[] slots;final long[] capacities;'),
        ('''        Preflight1981(Bitmap source,int rotation,int width,int height,int rows,GpuPolicy1960.GeometryData data,
                int[] slots,long[] capacities,long javaPeak){
            this(source,rotation,width,height,rows,data,slots,capacities,capacities,javaPeak);
        }
        Preflight1981(Bitmap source,int rotation,int width,int height,int rows,GpuPolicy1960.GeometryData data,
                int[] slots,long[] capacities,long[] uploadBytes,long javaPeak){''',
         '''        Preflight1981(Bitmap source,int rotation,int width,int height,int rows,GpuPolicy1960.GeometryData data,
                int[] slots,long[] capacities,long javaPeak){'''),
        ('geometry=data;this.slots=slots;this.capacities=capacities;uploadBytes1983=uploadBytes;this.javaPeak=javaPeak;',
         'geometry=data;this.slots=slots;this.capacities=capacities;this.javaPeak=javaPeak;'),
        ('GpuNoise1960.Lease1971 admitted=value.reserveCapacity1983(slots,capacities,uploadBytes1983,javaPeak);',
         'GpuNoise1960.Lease1971 admitted=value.reserveCapacity1971(slots,capacities,javaPeak);'),
        ('''                // slot24 can receive unchanged CPU-fallback rows during Strong.
                // Moire/geometry and final ARGB banks are written by the GPU;
                // only policy words and geometry tables are CPU uploads later.
                long[] uploads=data==null?new long[]{4L*n,0,16L*capacity,16L*capacity,0,0,0}:
                    new long[]{4L*n,0,0,16L*capacity,16L*capacity,0,0,0,0,4L*data.weights.length,4L*data.tables.length};
                if(!GpuNoise1960.planFits1983(capacities,uploads,javaPeak))continue;
                Preflight1981 result=new Preflight1981(source,rotation,width,height,rows,data,slots,capacities,uploads,javaPeak);''',
         '''                if(!GpuNoise1960.planFits1981(capacities,javaPeak))continue;
                Preflight1981 result=new Preflight1981(source,rotation,width,height,rows,data,slots,capacities,javaPeak);'''),
        ('''            long[] uploads=data==null?new long[]{carried==null?4L*n:0,0,0,0}:
                new long[]{carried==null?4L*n:0,0,0,0,0,4L*data.weights.length,4L*data.tables.length};
            imageLease=session.reserveCapacity1983(slots,capacities,uploads,0);''',
         '            imageLease=session.reserveCapacity1971(slots,capacities,0);'),
        ('GpuNoise1960.Lease1971 geometryLease=session.reserveCapacity1983(new int[]{4},new long[]{data.exactCrop?4:12L*width*(hi-lo)},new long[]{0},0);',
         'GpuNoise1960.Lease1971 geometryLease=session.reserveCapacity1971(new int[]{4},new long[]{data.exactCrop?4:12L*width*(hi-lo)},0);'),
        ('''        GpuNoise1960.Lease1971 lease=session.reserveCapacity1983(new int[]{3,8,17,18},
            new long[]{16L*capacity,16L*capacity,4L*capacity,4L*capacity},new long[]{16L*capacity,16L*capacity,0,0},8L*capacity+512L);''',
         '''        GpuNoise1960.Lease1971 lease=session.reserveCapacity1971(new int[]{3,8,17,18},
            new long[]{16L*capacity,16L*capacity,4L*capacity,4L*capacity},8L*capacity+512L);'''),
    ]
    restored = current.decode('utf-8')
    for new, old in edits:
        if restored.count(new) != 1:
            raise AssertionError('Reviewed capacity inverse span changed: ' + new.splitlines()[0])
        restored = restored.replace(new, old, 1)
    restored = restored.encode('utf-8')
    if hashlib.sha256(restored).hexdigest() != CHAIN_BASELINE82_SHA256 or restored != baseline:
        raise AssertionError('Capacity inverse did not reproduce the entire reviewed .82 source; unreviewed change remains')
    return restored


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    original = source / 'host_pipeline_diagnostics1982.py'
    retained = module('pipeline83_retained82', original)
    adapter = module('pipeline83_resident_adapter', source / 'host_resident_diagnostics1983.py')
    old_peer = source / 'tests1982/pipeline/ResidentPeers1982.java'
    copied_peer = work / 'adapted/ResidentPeers1982.java'
    copied_peer.parent.mkdir(parents=True, exist_ok=True)
    copied_peer.write_text(adapter.adapt_resident_peers1983(old_peer.read_text()))
    execute = retained.run
    original_preservation = retained.preservation
    replaced = []
    inverse_observed = []

    def preserved_with_exact_inverse(current_source):
        if Path(current_source).resolve() != source:
            raise AssertionError('Unexpected production source during preservation')
        baseline = (source / 'tests1983/memory/baseline82/GpuChain1961.java').read_bytes()
        current = (source / 'GpuChain1961.java').read_bytes()
        restored = inverse_chain_capacity1983(current, baseline)
        declaration_name = 'tests1982/pipeline/preserved81.json'
        declaration = json.loads((source / declaration_name).read_text())
        tree = work / 'preservation-only'
        for name in set(declaration['complete_files']) | set(declaration['methods']) | {declaration_name}:
            path = tree / name
            path.parent.mkdir(parents=True, exist_ok=True)
            if name == 'GpuChain1961.java':
                path.write_bytes(restored)
            else:
                if path.is_symlink():
                    path.unlink()
                elif path.exists():
                    raise AssertionError('Preservation adapter refuses to replace unexpected regular file: ' + str(path))
                path.symlink_to(source / name)
        inverse_observed.append(hashlib.sha256(current).hexdigest())
        return original_preservation(tree)

    def adapted_run(command, log, timeout=180):
        selected = []
        for argument in command:
            if str(argument) == str(old_peer):
                if not any('javac' in str(value) for value in command):
                    raise AssertionError('Resident peer substitution is restricted to compilation')
                selected.append(copied_peer)
                replaced.append(str(log))
            else:
                selected.append(argument)
        return execute(selected, log, timeout)

    retained.run = adapted_run
    retained.preservation = preserved_with_exact_inverse
    original_sha = hashlib.sha256(original.read_bytes()).hexdigest()
    peer_sha = hashlib.sha256(old_peer.read_bytes()).hexdigest()
    result = retained.test(source, work, jdk=jdk, ndk=ndk)
    if len(replaced) != 2 or len(inverse_observed) != 1:
        raise AssertionError('Both current and frozen Resident compile cases must execute')
    expected = {'single-gpu', 'single-null', 'single-link', 'residual-gpu', 'residual-null', 'residual-link',
                'worker', 'worker-background', 'worker-oracle', 'worker-write-failure',
                'geometry-cpu', 'geometry-gpu', 'geometry-failure',
                'regions-cpu', 'regions-gpu', 'regions-failure', 'model', 'defaults'}
    if not expected.issubset(result['cases']):
        raise AssertionError('An inherited backend case did not execute')
    if hashlib.sha256(original.read_bytes()).hexdigest() != original_sha or hashlib.sha256(old_peer.read_bytes()).hexdigest() != peer_sha:
        raise AssertionError('Historical runner or fixture changed during adapter execution')
    result.update(pipeline_retained_backend_suite1983=True,
                  pipeline_resident_fixture_adapter1983=True,
                  pipeline_reviewed_capacity_inverse1983=True,
                  reviewed_chain_baseline82_sha256=CHAIN_BASELINE82_SHA256,
                  current_chain_preservation_input_sha256=inverse_observed[0],
                  retained_runner_source_sha256=original_sha,
                  retained_resident_peer_sha256=peer_sha,
                  adapter_resident_peer_sha256=hashlib.sha256(copied_peer.read_bytes()).hexdigest(),
                  runner_source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest())
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
