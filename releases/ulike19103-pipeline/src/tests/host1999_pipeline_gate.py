#!/usr/bin/env python3
"""Retain .92 startup barrier/copy/ownership assertions for .99 baseline keys.

The explicit readiness peer now reports its known narrow (32-row) schedule as
scalar metadata. Certificate queries and actual finish/pixel execution still
raise; no GPU result or photograph qualification is fabricated. All historical
harness assertions remain unchanged and three narrow-metadata assertions are
added. The historical runner/fixtures and production sources are not edited.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(source,work,classes,native,jdk):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 old=source/'tests/host1992_pipeline_gate.py';before=sha(old)
 spec=importlib.util.spec_from_file_location('pipeline_gate92_for99',old);retained=importlib.util.module_from_spec(spec);spec.loader.exec_module(retained)
 name='com/hiro/ulike/GpuFinishDefault1990.java';peer=retained.PEERS[name]
 anchor='static boolean wideReady1993(){throw new AssertionError("readiness-only peer must not execute finish schedule selection");}'
 replacement='static int narrowQueries1999;static boolean wideReady1993(){narrowQueries1999++;return false;}'
 if peer.count(anchor)!=1:raise AssertionError('Historical schedule-metadata seam changed')
 peer=peer.replace(anchor,replacement)
 pixel='static android.graphics.Bitmap run(android.graphics.Bitmap b,int r,int w,int h,QualityPixels1932.Plan p,boolean f,ProcessingTiming1947.Trace t,String k){return null;}'
 if peer.count(pixel)!=1:raise AssertionError('Historical pixel runner seam changed')
 peer=peer.replace(pixel,'static android.graphics.Bitmap run(android.graphics.Bitmap b,int r,int w,int h,QualityPixels1932.Plan p,boolean f,ProcessingTiming1947.Trace t,String k){throw new AssertionError("narrow-metadata peer must not execute finish pixels");}')
 retained.PEERS=dict(retained.PEERS);retained.PEERS[name]=peer
 # .102 records the deployed startup/Strong route in a qualification key.
 # These are nonblocking scalar reads of existing controlled readiness; they
 # do not admit pixels, request preflight, fabricate a certificate or wait.
 startup_name='com/hiro/ulike/GpuStartup1990.java';startup=retained.PEERS[startup_name]
 startup_anchor=' public static boolean domainReady(){return ready;}'
 if startup.count(startup_anchor)!=1:raise AssertionError('Historical startup readiness seam changed')
 startup=startup.replace(startup_anchor,' public static boolean coreDomainReady19102(){return ready&&GpuStrongDefault1990.ready();}public static boolean modelDomainReady19102(){return ready&&GpuModelDefault1990.ready();}'+startup_anchor)
 retained.PEERS[startup_name]=startup
 strong_name='com/hiro/ulike/GpuStrongDefault1990.java';strong=retained.PEERS[strong_name]
 strong_anchor='static boolean ready(){return available;}'
 if strong.count(strong_anchor)!=1:raise AssertionError('Historical Strong readiness seam changed')
 retained.PEERS[strong_name]=strong.replace(strong_anchor,strong_anchor+'static int profile1994(){return 4;}')

 anchor='  check(value==null,"uncertified resident leaves foreground on proven standard route");'
 extra='''
  check(GpuFinishDefault1990.narrowQueries1999>0,"resident baseline observes controlled narrow schedule metadata");
  check(GpuChain1961.residentBaseline1978(source,0,65,67,plan,true).equals("standard1999:controlled-quality-standard1992:32"),"baseline key binds exact controlled32 schedule without pixel execution");'''
 if retained.HARNESS.count(anchor)!=1:raise AssertionError('Retained resident admission assertion changed')
 retained.HARNESS=retained.HARNESS.replace(anchor,anchor+extra)
 anchor='  GpuFinishDefault1990.available=false;'
 if retained.HARNESS.count(anchor)!=1:raise AssertionError('Retained missing-proof control changed')
 retained.HARNESS=retained.HARNESS.replace(anchor,'  int beforeMissingQueries1999=GpuFinishDefault1990.narrowQueries1999;\n'+anchor)
 anchor='  check(value==null,"missing independent finish proof never exposes an uncertified resident result");'
 if retained.HARNESS.count(anchor)!=1:raise AssertionError('Retained uncertified-result assertion changed')
 retained.HARNESS=retained.HARNESS.replace(anchor,anchor+'\n  check(GpuFinishDefault1990.narrowQueries1999==beforeMissingQueries1999,"missing finish readiness never queries a deployed schedule");')
 result=retained.test(source,work/'retained92',classes,native,jdk)
 if sha(old)!=before:raise AssertionError('Historical pipeline gate modified')
 if not result.get('actual_pipeline_resident_code') or not result.get('controlled_admission_peers'):raise AssertionError('Retained production gate missing')
 result.update(retained_pipeline_gate1992=True,strict_narrow_baseline_metadata1999=True,readiness_peer_pixels_forbidden1999=True,historical_runner_sha256=before,adapter_sha256=sha(Path(__file__)))
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for key in ('source','work','classes','native','jdk'):p.add_argument('--'+key,required=True)
 a=p.parse_args();print(json.dumps(test(a.source,a.work,a.classes,a.native,a.jdk),indent=2))
