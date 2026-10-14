"""Actual forward helpers plus a byte-pinned .52 source view for inherited oracles."""
from pathlib import Path
import hashlib

BASELINE_PINS={
 'QualityPipeline1932.java':'73378b9f6f3be7206525090ad8937c814de960162ead92d17681713c230b3616',
 'ProcessingTiming1947.java':'650db51a5829b473c55636e80744494ce658d0be4e3b838ae3804cd137318634',
 'AsyncSave1935.java':'3d3804d15a8de5264dc9caeec37ae8af575496086fbb18556665093823f773f5',
 'SaveQueue1935.java':'c2883e62a34af73cd77772541603da93e80f4996f50bb5d0bc14eb9854677c43',
 'ShotContext1932.java':'67d6eaf979cd235e015b082635fecc79f9c8d1c4c8caa5a76449d10e227d4266',
 'tests/ProcessingTiming1947Test.java':'0945ad97761f2d553419d0a7ebc290da1780ccd3a8fd34d387b3a2de13cd86a4',
}

def source_pins(root):
 root=Path(root)
 for name,expected in BASELINE_PINS.items():
  if hashlib.sha256((root/'tests/pipeline1953-reference'/name).read_bytes()).hexdigest()!=expected:
   raise AssertionError('Changed published .52 source oracle: '+name)
 return dict(BASELINE_PINS)

def sources1953(root,sources):
 root=Path(root);sources=list(sources)
 def add(name):
  p=root/name
  if p not in sources:sources.append(p)
 actual=[]
 for p in sources:
  p=Path(p)
  if p.name in ('QualityPipeline1932.java','ProcessingTiming1947.java','AsyncSave1935.java','SaveQueue1935.java','ShotContext1932.java'):
   actual.append(p.read_text())
 text='\n'.join(actual)
 if 'GpuFinish1952' in text:
  for n in ('GpuFinish1952.java','WholeRoute1952.java'):add(n)
 if 'GpuFinish1953' in text:
  for n in ('GpuFinish1953.java','FinishPolicy1953.java','WholeRoute1953.java'):add(n)
 if 'WholeRoute1953' in text:
  add('WholeRoute1953.java');add('WholeRoute1952.java');add('SpeedWorkers1935.java')
  if not any(p.name=='GpuFinish1953.java' for p in map(Path,sources)):
   add('tests/compat1953-fixtures/com/hiro/ulike/GpuFinish1953.java')
 if any(p.name=='ProcessingTiming1947.java' for p in map(Path,sources)):
  add('PerformanceHints1952.java');add('SpeedWorkers1935.java')
  for n in ('Build.java','Process.java','SystemClock.java'):
   if not any(str(p).endswith('/android/os/'+n) for p in sources):
    add('tests/compat1952-fixtures/android/os/'+n)
 return sources

def inherited1952_root(root,work):
 """Current unchanged core sources plus original .52 changed-family sources.

 Each file has its normal filename for javac and backward native code. The old
 oracle never runs a new helper with a missing native library and calls it .52.
 """
 root=Path(root).resolve();pins=source_pins(root)
 releases=Path(work).resolve()/'inherited-source1952/releases'
 view=releases/'ulike1952-h12-h15/src';view.mkdir(parents=True,exist_ok=True)
 for p in root.rglob('*'):
  if not p.is_file() or '__pycache__' in p.parts:continue
  name=p.relative_to(root).as_posix();target=view/name
  target.parent.mkdir(parents=True,exist_ok=True)
  # These runners resolve __file__ when recording source provenance. A
  # symlink resolves back into the forward source tree and cannot honestly
  # be made relative to this independent inherited snapshot.
  if target.is_symlink():target.unlink()
  if name in pins:target.write_bytes((root/'tests/pipeline1953-reference'/name).read_bytes())
  else:target.write_bytes(p.read_bytes())
 for name,expected in pins.items():
  if hashlib.sha256((view/name).read_bytes()).hexdigest()!=expected:raise AssertionError('Inherited .52 view differs: '+name)
 return view

PREGPU1950_PINS={'SpeedWorkers1935.java': '3a0832a24f1b1ee07984883869fa45a0d1b56c125f6acfb8dd12afcd4193f900', 'QualityPixels1932.java': '2e280f60398865cbf5efa4dfec7f9eb0ce7136ccaf3e04434f274713b4343da5', 'PolicyCache1945.java': 'eee2d4d86a6cedf251d66e2f8ed6fda96d4d09d20a7a9b8e7f7740914d3ac819', 'QualityShadow1932.java': 'ef71e6c2245c4af1f71d81077ea02ed04478b1de8586ba970ad4d6419bd3a69d', 'NoiseCache1944.java': '3c2516166b3df66548411620ca40438ef5b34e57d0362a16e1b4fb5c75c3d997', 'NativeSpeed1944.java': 'bf9493f12bfb7dcb1c59149019b0ce0d4a77325a64f1fe1137983d09c76e8357', 'GpuInteger1949.java': '834e5caf6f9644417ed1bfe6d82241e1f06068614d47bdb80f8397d79556c411', 'NativeSpeed1935.java': '14420c359a0c77dbbf5812cfde3040f045f76e477f8468e961220ec64285d8a6', 'SpatialNoise1934.java': 'e0220b9bc76d8f1f8e09ac712f048dc643b589b6c30da901502db7d0e2c297e6', 'LongMoire1934.java': '3efa95b627ec6ff160026b9b1a8d1582d7ba355c73ffc8bef575fdee1210de99', 'tests/noise-fixtures/com/hiro/ulike/QualityPipeline1932.java': 'd89c78e2671392308c5edf4a7384d7e6f00437aa3d4c3279f7bc3164296fedf0'}

def pregpu1950_root(root):
 root=Path(root)/"tests/pregpu1950-reference"
 for name,expected in PREGPU1950_PINS.items():
  if hashlib.sha256((root/name).read_bytes()).hexdigest()!=expected:raise AssertionError("Changed independent pre-GPU .50 source oracle: "+name)
 return root
