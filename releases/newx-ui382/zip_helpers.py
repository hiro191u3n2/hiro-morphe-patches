"""Deterministic ZIP append utilities that preserve previous compressed payloads."""
from pathlib import Path
import copy,hashlib,io,zipfile
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
  return{i.filename:(copy.copy(i),data[i.header_offset:infos[k+1].header_offset if k+1<len(infos)else z.start_dir])for k,i in enumerate(infos)}
def append_raw(z,info,raw):
 info=copy.copy(info);info.header_offset=z.fp.tell();z.fp.write(raw)
 z.filelist.append(info);z.NameToInfo[info.filename]=info;z.start_dir=z.fp.tell();z._didModify=True
def encoded(name,data):
 f=io.BytesIO()
 with zipfile.ZipFile(f,'w')as z:
  i=zipfile.ZipInfo(name,(2026,11,17,0,0,0));i.create_system=3;i.external_attr=0o100644<<16
  z.writestr(i,data,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
 with zipfile.ZipFile(io.BytesIO(f.getvalue()))as z:return copy.copy(z.infolist()[0]),f.getvalue()[:z.start_dir]
