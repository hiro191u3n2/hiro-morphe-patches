#!/usr/bin/env python3
"""Run actual display diagnostic storage/export recovery regressions."""
from pathlib import Path
import importlib.util

def test(source,work,jdk=None):
    source=Path(source)
    location=source/'host_camera_export1967.py'
    spec=importlib.util.spec_from_file_location('host_camera_export1967',location)
    module=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    result=module.test(source,Path(work),jdk=jdk)
    required=('trace_storage_regressions_passed','trace_export_regressions_passed',
        'multiprocessing_writer_isolation_passed','bounded_oversize_recovery_passed')
    if not (result.get('status')=='passed' and type(result.get('assertions')) is int and result['assertions']>0
            and result.get('physical_android_tested') is False and all(result.get(k) is True for k in required)):
        raise RuntimeError('Fresh display logger recovery/export evidence incomplete')
    return {**result,'logging_regressions_passed':True}
