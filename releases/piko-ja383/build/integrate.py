#!/usr/bin/env python3
"""Update only the Japanese/UI compatibility providers in immutable Hiro 1.0.382."""
from pathlib import Path
import argparse, copy, hashlib, io, json, os, re, subprocess, zipfile

BASE_SHA='85ed82ced41d546bd1b6a77a0e84fe12812e798fda674138257ba5791ff0887c'
BASE_BYTES=48325466
MF='META-INF/MANIFEST.MF'
VERSION='1.0.383'
TIMESTAMP='2026-11-18T00:00:00'

def require(ok,message):
    if not ok: raise ValueError(message)
def sha(path): return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def headers(raw):
    rows=[]
    for row in raw.replace(b'\r\n',b'\n').splitlines():
        if row.startswith(b' '): rows[-1]+=row[1:]
        elif row: rows.append(row)
    return dict(row.decode().split(': ',1)for row in rows)
def manifest(fields):
    out=bytearray()
    for key,value in fields.items():
        require(not any(c in value for c in '\r\n\0'),'Invalid manifest field')
        line=(key+': '+value).encode()
        while len(line)>70:
            end=70
            while line[end]&0xc0==0x80:end-=1
            out+=line[:end]+b'\r\n';line=b' '+line[end:]
        out+=line+b'\r\n'
    out+=b'\r\n';require(headers(bytes(out))==fields,'Manifest roundtrip differs')
    return bytes(out)
def local_records(path):
    data=Path(path).read_bytes()
    with zipfile.ZipFile(io.BytesIO(data))as z:
        infos=sorted(z.infolist(),key=lambda i:i.header_offset)
        require(len({i.filename for i in infos})==len(infos),'Duplicate ZIP entry')
        return {i.filename:(copy.copy(i),data[i.header_offset:infos[k+1].header_offset if k+1<len(infos)else z.start_dir])for k,i in enumerate(infos)}
def append_raw(z,info,raw):
    info=copy.copy(info);info.header_offset=z.fp.tell();z.fp.write(raw)
    z.filelist.append(info);z.NameToInfo[info.filename]=info;z.start_dir=z.fp.tell();z._didModify=True
def encoded(name,data):
    stream=io.BytesIO()
    with zipfile.ZipFile(stream,'w')as z:
        info=zipfile.ZipInfo(name,(2026,11,18,0,0,0));info.create_system=3;info.external_attr=0o100644<<16
        z.writestr(info,data,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
    with zipfile.ZipFile(io.BytesIO(stream.getvalue()))as z:return copy.copy(z.infolist()[0]),stream.getvalue()[:z.start_dir]
def provider_class(name):
    return bool(re.fullmatch(r'app/hiro/newx/patches/(?:NewXJapaneseData|NewXJapanesePatch(?:\$[A-Za-z0-9_$]+)?)\.class',name)
        or re.fullmatch(r'app/hiro/newxui/patches/UiOptionsPatch(?:\$[A-Za-z0-9_$]+)?\.class',name))
def read_archive(path):
    with zipfile.ZipFile(path)as z:
        require(z.testzip()is None,'ZIP CRC error')
        require(len(set(z.namelist()))==len(z.namelist()),'Duplicate ZIP entry')
        return {n:z.read(n)for n in z.namelist()}
def main():
    p=argparse.ArgumentParser(description=__doc__)
    for n in ['base','component','ui-component','tools','patcher','out']:p.add_argument('--'+n,type=Path,required=True)
    a=p.parse_args()
    require(a.base.stat().st_size==BASE_BYTES and sha(a.base)==BASE_SHA,'Baseline382 identity differs')
    a.out.mkdir(parents=True,exist_ok=True);work=a.out/'work';work.mkdir(exist_ok=True)
    base,part,ui=read_archive(a.base),read_archive(a.component),read_archive(a.ui_component)
    require(headers(base[MF])['Version']=='1.0.382','Wrong baseline version')
    require(headers(part[MF])['Version']=='1.0.1','Wrong Japanese version')
    require(headers(ui[MF])['Version']=='1.1.3','Wrong UI version')
    combined={}
    for source in [part,ui]:
        require(set(source)<=set(base),'Unexpected new archive entry')
        for n,raw in source.items():
            if n in [MF,'classes.dex']:continue
            require(n not in combined,'Duplicate component entry')
            combined[n]=raw
            if not provider_class(n):require(base[n]==raw,'Unrelated class/resource/runtime changed: '+n)
    require({n for n in base if provider_class(n)}=={n for n in combined if provider_class(n)},'Provider .class set differs')
    for n,src in [('base',base),('japanese',part),('ui',ui)]:(work/(n+'.dex')).write_bytes(src['classes.dex'])
    cp=a.tools/'classpath.jar'
    subprocess.run(['java','-XX:-UsePerfData','-jar',str(a.tools/'ecj.jar'),'-source','11','-target','11','-cp',str(cp),'-d',str(work),str(Path(__file__).with_name('MergeJapanese.java'))],check=True)
    subprocess.run(['java','-XX:-UsePerfData','-cp',os.pathsep.join([str(work),str(cp)]),'MergeJapanese',str(work/'base.dex'),str(work/'japanese.dex'),str(work/'ui.dex'),str(work/'merged.dex'),str(work/'preservation.json')],check=True)
    fields=headers(base[MF]);fields.update(Version=VERSION,Timestamp=TIMESTAMP,Description='Piko 3.12.0-dev.3 NewX Japanese and display settings compatibility. Existing runtime retained.')
    changes={MF:manifest(fields),'classes.dex':(work/'merged.dex').read_bytes()}
    changes.update({n:raw for n,raw in combined.items()if provider_class(n)and raw!=base[n]})
    output=a.out/('Hiro_Morphe_Patches_v'+VERSION+'.mpp');records=local_records(a.base)
    with zipfile.ZipFile(output,'w')as z:
        for n,(info,raw)in records.items():
            if n in changes:info,raw=encoded(n,changes[n])
            append_raw(z,info,raw)
    require(read_archive(output)==dict(base,**changes),'Output content differs')
    preserved=[n for n in base if n not in changes];new_records=local_records(output)
    require(all(records[n][1]==new_records[n][1]for n in preserved),'Unchanged raw ZIP record differs')
    extensions=[n for n in base if n.startswith('extensions/')]
    resources=[n for n in base if n.startswith(('newx-ja/','newx-ui/'))]
    require(all(n in preserved for n in extensions+resources),'Extension/resource changes prohibited')
    standalone=a.out/'Piko_NewX_Japanese_v1.0.1.mpp';standalone.write_bytes(a.component.read_bytes())
    ui_single=a.out/'NewX_UI_Options_v1.1.3.mpp';ui_single.write_bytes(a.ui_component.read_bytes())
    report={'schema':'hiro-piko-newx-japanese-build-v2','result':'PASS','version':VERSION,'component_version':'1.0.1','ui_component_version':'1.1.3','timestamp':TIMESTAMP,'baseline_version':'1.0.382','baseline_sha256':BASE_SHA,
        'component_sha256':sha(a.component),'ui_component_sha256':sha(a.ui_component),
        'artifacts':{f.name:{'bytes':f.stat().st_size,'sha256':sha(f)}for f in [output,standalone,ui_single]},
        'changed_archive_entries':sorted(changes),'preserved_existing_zip_entries':len(preserved),'preserved_raw_zip_records':len(preserved),
        'preserved_runtime_extensions':len(extensions),'preserved_japanese_and_ui_resource_entries':len(resources),'all_existing_extensions_identical':True,'all_translation_and_ui_assets_identical':True,'no_added_or_removed_zip_entries':True,
        'dex_preservation':json.loads((work/'preservation.json').read_text()),'tool_sha256':{n:sha(a.tools/n)for n in ['ecj.jar','classpath.jar']},
        'patcher_sha256':sha(a.patcher),'physical_device_tested':False,'emulator_tested':False}
    raw=json.dumps(report,ensure_ascii=False,indent=2)+'\n'
    for n in ['build-evidence.json','QA_build383.json']:(a.out/n).write_text(raw)
    print(raw,end='')
if __name__=='__main__':main()
