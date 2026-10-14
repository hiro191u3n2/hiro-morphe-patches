"""Add actual optional .52 helpers and explicit Android unavailable-service fixtures.

Inherited suites keep their original assertions and compare exact baseline pixels.
No physical Android GPU or OS scheduling behavior is modeled by these fixtures.
"""
from pathlib import Path

def sources1952(root, sources):
    root=Path(root);sources=list(sources)
    if (root/"host_common1953.py").is_file():
        from host_common1953 import sources1953
        return sources1953(root,sources)
    paths={str(Path(p)) for p in sources}
    if str(root/'QualityPipeline1932.java') in paths:
        sources += [root/'GpuFinish1952.java',root/'WholeRoute1952.java']
    if str(root/'ProcessingTiming1947.java') in paths:
        for name in ('PerformanceHints1952.java','SpeedWorkers1935.java'):
            if str(root/name) not in paths:sources.append(root/name)
        for name in ('Build.java','Process.java','SystemClock.java'):
            if not any(str(p).endswith('/android/os/'+name) for p in sources):
                sources.append(root/'tests/compat1952-fixtures/android/os'/name)
    return sources
