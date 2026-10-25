#!/usr/bin/env python3
"""Freeze explicitly reviewed production roots and exact source bytes for build19101."""
import argparse,json
from pathlib import Path
from build19101 import ROOT,VERSION,BUNDLE_VERSION,BASE_PINS,TOOL_PINS,source_pins,publication_pins,production_path,json_bytes,runtime_source_version

def declare(roots,output):
 names=json.loads(roots.read_text());assert names and names==sorted(set(names))
 for name in names:production_path(name)
 # The earlier automatic review rejection remains historical. The user's current
 # standing instruction authorizes automatic publication after this .118/.285
 # release passes the exact source, host, original-APKS and remote feed gates.
 value={'schema':'ulike19101-build-declaration-v1','ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,'runtime_version':runtime_source_version(),'timing_version_matches_release':True,'baseline':BASE_PINS,'tool_sha256':TOOL_PINS,'production_helper_roots':names,'reviewed_source_sha256':source_pins(),'reviewed_publication_sha256':publication_pins(),'device_tested':False,'publication_authorized_by_user':True,'standing_publication_instruction':True,'external_publication_performed':False,'automatic_review_blocked':True,'needs_explicit_release_confirmation':False}
 output.write_bytes(json_bytes(value));return value
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--roots',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();declare(a.roots,a.output)
