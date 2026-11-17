#!/usr/bin/env python3
"""Audit the hook against user-supplied X DEX files or baseline APKs."""
import argparse
from pathlib import Path
import subprocess

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
parser = argparse.ArgumentParser()
parser.add_argument('inputs', nargs='+', help='Original classes6.dex or supported Piko baseline APK')
parser.add_argument('--morphe', type=Path, default=ROOT/'tools/morphe.jar')
parser.add_argument('--ecj', type=Path, default=ROOT/'tools/ecj.jar')
args = parser.parse_args()
classes = HERE/'test-classes'
classes.mkdir(exist_ok=True)
subprocess.run(['java', '-jar', str(args.ecj), '-8', '-nowarn', '-cp', str(args.morphe), '-d', str(classes),
                str(ROOT/'chat/ChatRecommendationsHook.java'), str(HERE/'ViewCountHook.java'), str(HERE/'ViewCountHookTest.java')], check=True)
subprocess.run(['java', '-Xmx2g', '-cp', str(args.morphe)+':'+str(classes), 'ViewCountHookTest', *args.inputs], check=True)
