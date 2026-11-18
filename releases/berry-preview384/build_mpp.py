"""Build the combined 1.0.384 and Berry 1.4.7 MPPs from published baselines."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory

MANIFEST = 'META-INF/MANIFEST.MF'
FAMILY = 'app/hiro/berry/patches/ForegroundTabsPatch'
BASES = {
    'Hiro_Morphe_Patches_v1.0.383.mpp': ('1.0.384','59f4d5c8b2769d681eeb1177d6713f01465113405a2422003ff22c4cf75dfa25'),
    'Berry_Browser_v1.4.6.mpp': ('1.4.7','7b38a23801b082bbd668399e2a76e4de07321c296414d87faeba2b78ca1c14d0'),
}
PINS = {
    'ecj.jar':'74c93172475fee0cd4e6eefb44233516dd40525642da35d009146d97ea598229',
    'd8.jar':'38a9ffaaebbd17d1514e3ecbff5a85b989bb13c551b9e224c7409514eea84ff5',
    'classpath.jar':'fe4bc92815bb14976150e4ec83e4985932b46d1d47edec75d37604481572e864',
}
def sha(raw):
    return hashlib.sha256(raw).hexdigest()
def belongs(name):
    return name==FAMILY+'.class' or name.startswith(FAMILY+'$') and name.endswith('.class')
def run(*cmd):
    subprocess.run(list(map(str,cmd)),check=True)
def build(args):
    spec=importlib.util.spec_from_file_location('berry_release_zip',args.zip_helper)
    writer=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(writer)
    args.output.mkdir(parents=True,exist_ok=True)
    report={'artifacts':{},'result':'PASS','provider':FAMILY,'source_sha256':sha(args.src.read_bytes()),'tools':PINS}
    for name,expected in PINS.items():
        assert sha((args.tools/name).read_bytes())==expected,name
    with TemporaryDirectory() as tmpdir:
        tmp=Path(tmpdir)
        classes=tmp/'classes';classes.mkdir()
        dex=tmp/'dex';dex.mkdir()
        javac=tmp/'helper';javac.mkdir()
        mods={}
        for name,(_,checksum) in BASES.items():
            p=args.previous/name
            assert sha(p.read_bytes())==checksum
            mods[name]=writer.archive(p)
        previous=next(iter(mods.values()))
        provider_jar=tmp/'original.jar'
        writer.write_zip(provider_jar,{n:b for n,b in previous.items() if n.endswith('.class')})
        cp=args.tools/'classpath.jar'
        ecj=['java','-jar',args.tools/'ecj.jar','-encoding','UTF-8']
        run(*ecj,'-source','8','-target','8','-g:source,lines','-cp',str(cp)+':'+str(provider_jar),'-d',classes,args.src)
        compiled={p.relative_to(classes).as_posix():p.read_bytes() for p in classes.rglob('*.class')}
        assert compiled and all(belongs(n) for n in compiled)
        writer.write_zip(tmp/'provider.jar',compiled)
        run('java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--classpath',cp,'--classpath',provider_jar,'--output',dex,tmp/'provider.jar')
        merger=Path(__file__).with_name('MergeBerryPreview.java')
        run(*ecj,'-source','11','-target','11','-cp',cp,'-d',javac,merger)
        (tmp/'base.dex').write_bytes(previous['classes.dex'])
        run('java','-cp',str(javac)+':'+str(cp),'MergeBerryPreview',tmp/'base.dex',dex/'classes.dex',tmp/'combined.dex',tmp/'single.dex',tmp/'dex.json')
        report['dex_preservation']=json.loads((tmp/'dex.json').read_bytes())
        for idx,(name,(version,_)) in enumerate(BASES.items()):
            base=args.previous/name
            entries=mods[name]
            updated={n:b for n,b in entries.items() if not belongs(n)}
            updated.update(compiled)
            mf=writer.headers(entries[MANIFEST])
            old_version=mf['Version']
            mf.update(Version=version,Timestamp='2026-11-18T15:00:00',
                      Description='Berry Browser 1.4.7: stop scheduling automatic foreground preview capture when page loading finishes.')
            updated[MANIFEST]=writer.manifest(mf)
            updated['classes.dex']=(tmp/('single.dex' if idx else 'combined.dex')).read_bytes()
            changed=sorted(n for n in set(entries)|set(updated) if entries.get(n)!=updated.get(n))
            assert all(belongs(n) or n in {MANIFEST,'classes.dex'} for n in changed)
            assert FAMILY+'.class' in changed, 'Foreground provider was not modified'
            report.setdefault('changed_entries',changed)
            assert report['changed_entries']==changed
            target=args.output/name.replace(old_version,version)
            writer.write_combined_zip(target,updated,base,entries,tmp/'changed.zip')
            report['artifacts'][target.name]={'bytes':target.stat().st_size,'sha256':sha(target.read_bytes())}
    (args.output/'build-evidence.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report))
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    for key in ['previous','src','output','zip-helper','tools']:
        parser.add_argument('--'+key,required=True,type=Path)
    build(parser.parse_args())
