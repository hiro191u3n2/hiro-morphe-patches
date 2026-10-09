#!/usr/bin/env python3
"""Run retained .65 camera regressions alongside required GPU processing."""
from pathlib import Path
import importlib.util

def test(root,work,jdk,ndk=None):
 root=Path(root).resolve()
 spec=importlib.util.spec_from_file_location('camera1965_retained',root/'host_camera1965.py')
 mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
 return mod.test(root,Path(work)/'camera-preservation1966',jdk=jdk)
