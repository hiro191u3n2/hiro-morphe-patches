#!/usr/bin/env python3
"""Current H25 suffix reads and H27 owned-image leases with real production JNI.

Bitmap counters and camera/primary-NR surfaces are explicit host fixtures. The
lease test executes the same disposal helper that four pinned final-save DEX
calls use; the transformer separately proves those hooks by exact inverse hash.
No physical Android encode/camera timing is represented by this suite.
"""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess
from host_common1954 import sources1954

def test(root,work,android=None,gpu_library=None):
    root=Path(root).resolve();out=Path(work).resolve()/'host-pipelinelease1954'
    out.mkdir(parents=True,exist_ok=True);classes=out/'classes';classes.mkdir(exist_ok=True)
    if not gpu_library or not Path(gpu_library).is_file():
        raise AssertionError('Current actual production GPU JNI library required for H25/H27')
    fixtures=[p for p in (root/'tests/pipeline1942-fixtures').rglob('*.java')
              if p.relative_to(root/'tests/pipeline1942-fixtures').as_posix()!='android/graphics/Bitmap.java']
    fixtures+=list((root/'tests/timing1947-fixtures').rglob('*.java'))
    fixtures+=[root/'tests/h25h27-fixtures/android/graphics/Bitmap.java']
    sources=fixtures+[root/n for n in (
        'QualityPipeline1932.java','SpeedWorkers1935.java','Scheduling1944.java','NoiseCache1944.java',
        'NativeSpeed1944.java','GpuInteger1949.java','QualityPixels1932.java','NativeMoire1951.java',
        'PolicyCache1945.java','QualityShadow1932.java','SpatialNoise1934.java','LongMoire1934.java',
        'ProcessingTiming1947.java','tests/PipelineQuality1942Test.java',
        'tests/PipelineGpu1953Test.java','tests/PipelineLease1954Test.java')]
    sources=sources1954(root,sources)
    pins={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
    javac=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
    java=os.environ.get('ULIKE_JAVA') or (str(Path(javac).with_name('java')) if javac else shutil.which('java'))
    compiler=[javac] if javac else [java,'com.sun.tools.javac.Main']
    commands=[('compile',compiler+['-encoding','UTF-8','-source','8','-target','8','-Xlint:-options',
                                  '-d',str(classes),*map(str,sources)]),
              ('test',[java,'-XX:ActiveProcessorCount=4','-Xmx768m','-Xcheck:jni',
                       '-Djava.library.path='+str(Path(gpu_library).resolve().parent),
                       '-cp',str(classes),'com.hiro.ulike.PipelineLease1954Test'])]
    environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    for label,command in commands:
        process=subprocess.run(command,capture_output=True,text=True,timeout=240,env=environment)
        (out/(label+'.log')).write_text(process.stdout+process.stderr)
        if process.returncode:raise RuntimeError(process.stdout[-4000:]+process.stderr[-10000:])
    result=json.loads(process.stdout)
    if pins!={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}:
        raise AssertionError('Sources changed while current H25/H27 assertions executed')
    if result.get('status')!='passed' or result.get('h25_assertions',0)<100000 or result.get('h27_assertions',0)<25:
        raise AssertionError('Actual H25/H27 suffix/owned-lease assertions missing')
    result.update(physical_android_tested=False,device_speed_measured=False,
                  actual_production_gpu_jni_executed=True,
                  reference_snapshot_copies=0,cpu_destination_copies_per_probe=1,
                  source_snapshot_copies=0,
                  source_pins=pins,
                  gpu_library_sha256=hashlib.sha256(Path(gpu_library).read_bytes()).hexdigest(),
                  fixture_scope='Actual .54 pipeline/lease logic and production JNI/GLES execute. Bitmap counter/metadata, camera, primary NR and unavailable Android services are explicit fixtures. Final-save disposal executes its exact lease helper; four emitted DEX hooks have independent inverse verification. Physical encoding and capture are not performed.')
    (out/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    return result

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--out',required=True);p.add_argument('--gpu-library',required=True)
    a=p.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,a.out,gpu_library=a.gpu_library),indent=2))
