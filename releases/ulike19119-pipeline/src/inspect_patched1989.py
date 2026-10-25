#!/usr/bin/env python3
"""Inspect an unchanged private .89 MPP application checkpoint; never repatch it.

The source, original input, MPP and private checkpoint are verified by the same
application checker used by apply_original1989.py. Only the sanitized report is
public evidence. The original APK and logs remain in the selected work folder.
"""
from pathlib import Path
import argparse
from apply_original1989 import main

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('input','mpp','work','jdk','tools'):
        parser.add_argument('--'+name,type=Path,required=True)
    parser.add_argument('--stage',choices=('provisional','final'),default='final')
    args = parser.parse_args()
    main(args.input,args.mpp,args.work,args.jdk,args.tools,args.stage,False,True)
