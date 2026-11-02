#!/usr/bin/env python3
"""Make Berry Manager compatibility version-agnostic without changing executable guards.
Only BFCache <clinit> target list becomes one null-version target; four existing providers already
use null versions. Same-width JVM/DEX instruction edits preserve every other byte.
"""
from pathlib import Path
import argparse, copy, hashlib, json, struct, subprocess, tempfile, zipfile, zlib
TARGET='app/hiro/berrybfcache/patches/BfCacheRepairPatch.class'
VERSIONS={'3.83.93.1','3.83.74'}
PINS={'combined':'37401913eddad841bd9981e0374804b858ff36ae39fe9ff197aa6815e22e053b',
      'standalone':'ec31d178533b026302ff348c4e119b4242a3527c1f20c35f15ae8fbae82eae2c'}
def sha(b):return hashlib.sha256(b).hexdigest()
def u2(b,o):return struct.unpack_from('>H',b,o)[0]
def u4(b,o):return struct.unpack_from('>I',b,o)[0]
def le4(b,o):return struct.unpack_from('<I',b,o)[0]
def need(v,msg):
 if not v:raise ValueError(msg)
def changed(a,b):return [i for i,(x,y) in enumerate(zip(a,b)) if x!=y]
def patch_class(raw):
 b=bytearray(raw);need(b[:4]==b'\xca\xfe\xba\xbe','Invalid class'); cp={};o=10;i=1
 while i<u2(b,8):
  tag=b[o];o+=1
  if tag==1:n=u2(b,o);o+=2;cp[i]=(tag,bytes(b[o:o+n]).decode('utf-8'));o+=n
  elif tag in (7,8,16,19,20):cp[i]=(tag,u2(b,o));o+=2
  elif tag in (3,4,9,10,11,12,17,18):cp[i]=(tag,bytes(b[o:o+4]));o+=4
  elif tag in (5,6):cp[i]=(tag,bytes(b[o:o+8]));o+=8;i+=1
  elif tag==15:cp[i]=(tag,bytes(b[o:o+3]));o+=3
  else:raise ValueError('Unknown constant tag '+str(tag))
  i+=1
 o+=6;n=u2(b,o);o+=2+2*n
 fields=u2(b,o);o+=2
 for _ in range(fields):
  o+=6;n=u2(b,o);o+=2
  for _ in range(n):ln=u4(b,o+2);o+=6+ln
 methods=u2(b,o);o+=2;edits=[];instructions=[];literals={}
 widths={0x10:2,0x11:3,0x12:2,0x13:3,0x14:3,0x15:2,0x16:2,0x17:2,0x18:2,0x19:2,
         0x36:2,0x37:2,0x38:2,0x39:2,0x3a:2,0x84:3,0xb2:3,0xb3:3,0xb4:3,0xb5:3,
         0xb6:3,0xb7:3,0xb8:3,0xb9:5,0xba:5,0xbb:3,0xbc:2,0xbd:3,0xc0:3,0xc1:3,
         0xc5:4,0xc6:3,0xc7:3,0xc8:5,0xc9:5}
 widths.update({i:3 for i in range(0x99,0xa9)})
 for _ in range(methods):
  name=cp[u2(b,o+2)][1];o+=6;n=u2(b,o);o+=2
  for _ in range(n):
   attr=cp[u2(b,o)][1];ln=u4(b,o+2);start=o+6;o=start+ln
   if name!='<clinit>' or attr!='Code':continue
   clen=u4(b,start+4);p=start+8;end=p+clen
   while p<end:
    op=b[p];need(op not in (0xaa,0xab,0xc4),'Unexpected variable JVM instruction');w=widths.get(op,1)
    instructions.append((p,op,w))
    if op in (0x12,0x13):
     idx=b[p+1] if op==0x12 else u2(b,p+1);v=cp[idx]
     if v[0]==8 and cp[v[1]][1] in VERSIONS:
      literals[cp[v[1]][1]]=len(instructions)-1
    p+=w
   need(p==end,'JVM decode alignment')
 need(set(literals)==VERSIONS,'Exact JVM version literals required')
 first,second=literals['3.83.93.1'],literals['3.83.74']
 def edit(p,n,new,label):
  edits.append({'offset':p,'purpose':label,'before':bytes(b[p:p+n]).hex(),'after':new.hex()});b[p:p+n]=new
 pos,op,w=instructions[first];edit(pos,w,b'\x01'+b'\0'*(w-1),'one null-version target')
 arr=[i for i in range(first) if instructions[i][1]==0xbd]
 need(len(arr)==1 and instructions[arr[0]-1][1]==0x05,'Expected two-item target array')
 pos,op,w=instructions[arr[0]-1];edit(pos,1,b'\x04','target array length one')
 start=second-4
 need([i[1] for i in instructions[start:second]]==[0x59,0x04,0xbb,0x59],'Second target block differs')
 stop=next(i for i in range(second,len(instructions)) if instructions[i][1]==0x53)
 pos=instructions[start][0];n=instructions[stop][0]+1-pos
 edit(pos,n,b'\0'*n,'remove second target construction and array store')

 allowed={p for e in edits for p in range(e['offset'],e['offset']+len(bytes.fromhex(e['before'])))}
 need(len(raw)==len(b) and set(changed(raw,b))<=allowed,'Unexpected JVM changes')
 return bytes(b),edits

def uleb(b,o):
 v=s=0
 while True:
  x=b[o];o+=1;v|=(x&127)<<s
  if x<128:return v,o
  s+=7;need(s<35,'ULEB overflow')
def patch_dex(raw):
 b=bytearray(raw);need(b[:4]==b'dex\n','Invalid DEX')
 n,off=le4(b,56),le4(b,60);strings=[]
 for i in range(n):
  p=le4(b,off+i*4);_,p=uleb(b,p);end=b.index(0,p);strings.append(bytes(b[p:end]).decode('utf-8',errors='replace'))
 types=[strings[le4(b,le4(b,68)+i*4)] for i in range(le4(b,64))]
 mo=le4(b,92);co=le4(b,100);code=None
 for i in range(le4(b,96)):
  pos=co+i*32
  if types[le4(b,pos)]!='L'+TARGET[:-6]+';':continue
  p=le4(b,pos+24);sf,p=uleb(b,p);inf,p=uleb(b,p);dm,p=uleb(b,p);vm,p=uleb(b,p)
  for _ in range(sf+inf):_,p=uleb(b,p);_,p=uleb(b,p)
  for count in (dm,vm):
   idx=0
   for _ in range(count):
    d,p=uleb(b,p);idx+=d;_,p=uleb(b,p);c,p=uleb(b,p)
    if strings[le4(b,mo+idx*8+4)]=='<clinit>':need(code is None,'Duplicate initializer');code=c
 need(code is not None,'DEX target initializer missing')
 # All opcodes permitted by the frozen initializer; reject any new flow shape.
 widths={0x00:1,0x0c:1,0x0e:1,0x12:1,0x13:2,0x14:3,0x1a:2,0x1b:3,0x22:2,0x23:2,
         0x4d:2,0x62:2,0x69:2,0x70:3,0x71:3,0x76:3,0x77:3}
 p=code+16;end=p+le4(b,code+12)*2;edits=[];instructions=[];literals={}
 while p<end:
  op=b[p];need(op in widths,'Unexpected initializer DEX opcode '+hex(op));w=widths[op]*2
  instructions.append((p,op,w))
  if op in (0x1a,0x1b):
   idx=struct.unpack_from('<H',b,p+2)[0] if op==0x1a else le4(b,p+2)
   if strings[idx] in VERSIONS:
    literals[strings[idx]]=len(instructions)-1
  p+=w
 need(p==end,'DEX instruction alignment')
 need(set(literals)==VERSIONS,'Exact DEX version literals required')
 first,second=literals['3.83.93.1'],literals['3.83.74']
 def edit(p,n,new,label):
  edits.append({'offset':p,'purpose':label,'before':bytes(b[p:p+n]).hex(),'after':new.hex()});b[p:p+n]=new
 pos,op,w=instructions[first];edit(pos,w,bytes([0x13,b[pos+1],0,0])+b'\0'*(w-4),'one null-version target')
 arr=[i for i in range(first) if instructions[i][1]==0x23]
 need(len(arr)==1 and instructions[arr[0]-1][1]==0x12,'Expected array length const/4')
 pos,op,w=instructions[arr[0]-1]
 need(b[pos+1]>>4==2,'Expected two-item array')
 edit(pos,2,bytes([0x12,(b[pos+1]&15)|0x10]),'target array length one')
 start=second-1;need(instructions[start][1]==0x22,'Second target construction differs')
 stop=next(i for i in range(second,len(instructions)) if instructions[i][1]==0x4d)
 pos=instructions[start][0];n=instructions[stop][0]+instructions[stop][2]-pos
 replacement=bytearray(n)
 # The array-index constant is reused as PatchKt.bytecodePatch's default flag.
 # Preserve it at its original offset; all other second-target writes are dead.
 constants=[x for x in instructions[start:stop] if x[1]==0x12]
 need(len(constants)==1,'Expected shared default/index constant')
 keep,_,width=constants[0]
 need(b[keep:keep+width]==b'\x12\x19','Expected const/4 v9, 1 default flag')
 replacement[keep-pos:keep-pos+width]=b[keep:keep+width]
 edit(pos,n,bytes(replacement),'remove second target and preserve shared default flag')

 b[12:32]=hashlib.sha1(b[32:]).digest();struct.pack_into('<I',b,8,zlib.adler32(b[12:])&0xffffffff)
 allowed=set(range(8,32))|{p for e in edits for p in range(e['offset'],e['offset']+len(bytes.fromhex(e['before'])))}
 need(len(raw)==len(b) and set(changed(raw,b))<=allowed,'Unexpected DEX changes')
 return bytes(b),edits

def headers(raw):return dict(x.split(': ',1) for x in raw.decode().replace('\r\n ','').splitlines() if x)
def manifest(fields):
 out=bytearray()
 for k,v in fields.items():
  row=(k+': '+v).encode()
  while len(row)>70:
   n=70
   while row[n]&0xc0==0x80:n-=1
   out+=row[:n]+b'\r\n';row=b' '+row[n:]
  out+=row+b'\r\n'
 return bytes(out)+b'\r\n'
def build_one(path,kind,out):
 raw=path.read_bytes();need(sha(raw)==PINS[kind],'Frozen baseline '+kind+' differs')
 version='1.0.330' if kind=='combined' else '1.0.4'
 filename='Hiro_Morphe_Patches_v1.0.330.mpp' if kind=='combined' else 'Berry_BFCache_Repair_v1.0.4.mpp'
 with zipfile.ZipFile(path) as z:
  need(len(z.namelist())==len(set(z.namelist())) and z.testzip() is None,'Invalid baseline ZIP')
  cls,ce=patch_class(z.read(TARGET));dex,de=patch_dex(z.read('classes.dex'));f=headers(z.read('META-INF/MANIFEST.MF'))
  f.update(Version=version,Timestamp='2026-11-02T00:00:00',Description='Berry Browser: all-version Manager display for all five patches. Only BFCache compatibility version metadata changed. Existing executable guards, runtime code and other applications are preserved.')
  new={TARGET:cls,'classes.dex':dex,'META-INF/MANIFEST.MF':manifest(f)}
  dest=out/filename
  with zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as w:
   for info in z.infolist():w.writestr(copy.copy(info),new.get(info.filename,z.read(info.filename)))
  with zipfile.ZipFile(dest) as w:
   need(w.testzip() is None and w.namelist()==z.namelist(),'Output ZIP differs')
   delta=[n for n in z.namelist() if z.read(n)!=w.read(n)];need(set(delta)==set(new),'Unexpected ZIP entry change')
   return {'file':filename,'version':version,'baseline_sha256':sha(raw),'sha256':sha(dest.read_bytes()),'bytes':dest.stat().st_size,
    'entries_changed':delta,'entries_added':[],'entries_removed':[],'jvm_edits':ce,'dex_edits':de,
    'all_other_jvm_bytes_unchanged':True,'all_other_dex_bytes_unchanged_except_checksum':True,
    'all_runtime_extensions_unchanged':True,'all_other_archive_entries_unchanged':True,
    'all_profile_and_structural_guards_unchanged':True,'dex_rebuilt':False}
def main():
 p=argparse.ArgumentParser(description=__doc__)
 for k in ['combined','standalone','out','java','classpath','ecj']:p.add_argument('--'+k,type=Path,required=True)
 a=p.parse_args();a.out.mkdir(parents=True,exist_ok=True)
 report={'schema':'berry-allversions330-qa-v1','result':'PASS','device_tested':False,'outputs':{}}
 for k in ['combined','standalone']:report['outputs'][k]=build_one(getattr(a,k),k,a.out)
 with tempfile.TemporaryDirectory(prefix='berry-version-qa-') as tmp:
  cmd=[str(a.java),'-jar',str(a.ecj),'-source','11','-target','11','-encoding','UTF-8','-cp',str(a.classpath),'-d',tmp,str(Path(__file__).with_name('Verify.java'))]
  subprocess.run(cmd,check=True,capture_output=True,text=True)
  for k,row in report['outputs'].items():
   result=subprocess.run([str(a.java),'-cp',tmp+':'+str(a.classpath),'Verify',str(a.out/row['file'])],check=True,capture_output=True,text=True)
   row['loaded_metadata']=json.loads(result.stdout)
   need(row['loaded_metadata']['berry_patch_count']==5 and row['loaded_metadata']['all_versions_null'] is True,'Metadata not all-version')
 report.update(physical_device_tested=False,combined_sha256=report['outputs']['combined']['sha256'],standalone_sha256=report['outputs']['standalone']['sha256'],baseline_combined_sha256=PINS['combined'],baseline_standalone_sha256=PINS['standalone'],all_five_berry_versions_null=True,runtime_guards_preserved=True,extensions_unchanged=True,unrelated_providers_preserved=True)
 (a.out/'QA_allversions330.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({k:{t:r[t] for t in ['file','bytes','sha256']} for k,r in report['outputs'].items()},ensure_ascii=False))
if __name__=='__main__':main()
