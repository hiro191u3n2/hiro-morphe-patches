#!/usr/bin/env python3
"""Add Japanese provider/resources, retaining every unchanged compressed377 ZIP record."""
from pathlib import Path
import argparse,copy,hashlib,io,json,os,subprocess,zipfile

BASE_SHA='1f2e4d227d1260ce484950cdd4651e94cd0cfd529df20c9dcec5a48763c6d5c8'
MF='META-INF/MANIFEST.MF'

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def headers(raw):
 rows=[]
 for r in raw.replace(b'\r\n',b'\n').splitlines():
  if r.startswith(b' '):rows[-1]+=r[1:]
  elif r:rows.append(r)
 return dict(r.decode().split(': ',1)for r in rows)
def manifest(fields):
 out=bytearray()
 for k,v in fields.items():
  assert not any(c in v for c in '\r\n\0')
  line=(k+': '+v).encode()
  while len(line)>70:
   end=70
   while line[end]&0xc0==0x80:end-=1
   out+=line[:end]+b'\r\n';line=b' '+line[end:]
  out+=line+b'\r\n'
 out+=b'\r\n';assert headers(bytes(out))==fields;return bytes(out)
def local_records(path):
 data=Path(path).read_bytes()
 with zipfile.ZipFile(io.BytesIO(data))as z:
  infos=sorted(z.infolist(),key=lambda x:x.header_offset)
  return {i.filename:(copy.copy(i),data[i.header_offset:infos[k+1].header_offset if k+1<len(infos)else z.start_dir]) for k,i in enumerate(infos)}
def append_raw(z,info,raw):
 # Standard ZipFile.close emits the central directory with relocated offsets.
 info=copy.copy(info);info.header_offset=z.fp.tell();z.fp.write(raw)
 z.filelist.append(info);z.NameToInfo[info.filename]=info;z.start_dir=z.fp.tell();z._didModify=True
def encoded(name,data):
 f=io.BytesIO()
 with zipfile.ZipFile(f,'w')as z:
  i=zipfile.ZipInfo(name,(2026,11,17,0,0,0));i.create_system=3;i.external_attr=0o100644<<16
  z.writestr(i,data,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
 with zipfile.ZipFile(io.BytesIO(f.getvalue()))as z:return copy.copy(z.infolist()[0]),f.getvalue()[:z.start_dir]
def main():
 p=argparse.ArgumentParser()
 for n in ['base','component','tools','patcher','out']:p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();assert sha(a.base)==BASE_SHA
 a.out.mkdir(parents=True,exist_ok=True);work=a.out/'work';work.mkdir(exist_ok=True)
 with zipfile.ZipFile(a.base)as z:base={n:z.read(n)for n in z.namelist()}
 with zipfile.ZipFile(a.component)as z:part={n:z.read(n)for n in z.namelist()}
 assert headers(base[MF])['Version']=='1.0.377'
 for name,src in [('base',base),('addon',part)]: (work/(name+'.dex')).write_bytes(src['classes.dex'])
 merge_cp=a.tools/'classpath.jar'
 subprocess.run(['java','-XX:-UsePerfData','-jar',str(a.tools/'ecj.jar'),'-source','11','-target','11','-cp',str(merge_cp),'-d',str(work),str(Path(__file__).with_name('MergeJapanese.java'))],check=True)
 subprocess.run(['java','-XX:-UsePerfData','-cp',os.pathsep.join([str(work),str(merge_cp)]),'MergeJapanese',str(work/'base.dex'),str(work/'addon.dex'),str(work/'merged.dex'),str(work/'preservation.json')],check=True)
 fields=headers(base[MF]);fields.update(Version='1.0.378',Timestamp='2026-11-17T00:00:00',Description='Japanese Piko NewX settings and menus for X 12.43. Existing patches retained.')
 changes={MF:manifest(fields),'classes.dex':(work/'merged.dex').read_bytes()}
 additions={n:b for n,b in part.items()if n not in [MF,'classes.dex']}
 assert not(base.keys()&additions.keys())
 output=a.out/'Hiro_Morphe_Patches_v1.0.378.mpp';records=local_records(a.base)
 with zipfile.ZipFile(output,'w')as z:
  for n,(info,raw)in records.items():
   if n in changes:info,raw=encoded(n,changes[n])
   append_raw(z,info,raw)
  for n,b in sorted(additions.items()):append_raw(z,*encoded(n,b))
 with zipfile.ZipFile(output)as z:
  assert z.testzip()is None;actual={n:z.read(n)for n in z.namelist()};assert actual==dict(base,**changes,**additions)
 preserved=[n for n in base if n not in changes];new_records=local_records(output)
 assert all(records[n][1]==new_records[n][1] for n in preserved)
 standalone=a.out/'Piko_NewX_Japanese_v1.0.0.mpp';standalone.write_bytes(a.component.read_bytes())
 report={'schema':'hiro-piko-newx-japanese-build-v1','result':'PASS','baseline_sha256':BASE_SHA,'component_sha256':sha(a.component),'artifacts':{f.name:{'bytes':f.stat().st_size,'sha256':sha(f)}for f in [output,standalone]},'preserved_existing_zip_entries':len(preserved),'all_existing_extensions_identical':True,'preserved_raw_zip_records':len(preserved),'dex_preservation':json.loads((work/'preservation.json').read_text()),'physical_device_tested':False,'emulator_tested':False}
 (a.out/'build-evidence.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False))
if __name__=='__main__':main()
