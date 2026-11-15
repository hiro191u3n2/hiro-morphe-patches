from pathlib import Path
from urllib.request import Request,urlopen
from urllib.error import HTTPError
import hashlib,json
root=Path('/workspace/scratch/193242214db5');pub=root/'site-publication/.sites-checkout/public'
origin='https://hiro-morphe-patches.otoha10.chatgpt.site/'
report={}
def get(name,method='GET',headers=None):
 req=Request(origin+name,method=method,headers={'User-Agent':'Hiro-Morphe-Release-Check',**(headers or {})})
 try:
  with urlopen(req,timeout=60) as r:return r.status,{k.lower():v for k,v in r.headers.items()},r.read()
 except HTTPError as e:return e.code,{k.lower():v for k,v in e.headers.items()},e.read()
for name in ['patches-bundle.json','source','CHANGELOG.md','Maps_Hiro_Addons_v1.1.0.mpp','Maps_Hiro_Addons_v1.1.0_sources_and_QA.zip','Hiro_Morphe_Patches_v1.0.374.mpp']:
 code,headers,data=get(name);expected=(pub/name).read_bytes();assert code==200,(name,code);assert data==expected,name
 report[name]={'status':code,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()};print('PASS live',name,len(data),flush=True)
name='Hiro_Morphe_Patches_v1.0.374.mpp';raw=(pub/name).read_bytes();sha=hashlib.sha256(raw).hexdigest()
code,h,b=get(name,'HEAD');assert code==200 and not b and int(h.get('content-length','0'))==len(raw),(code,h)
for text,start,end in [('bytes=0-4095',0,4095),('bytes=-33',len(raw)-33,len(raw)-1),('bytes=10000-10099',10000,10099)]:
 code,h,b=get(name,headers={'Range':text});assert code==206 and b==raw[start:end+1],(text,code,h)
 print('PASS live range',text,flush=True)
report['manager_ranges']={'HEAD':True,'prefix':True,'suffix':True,'resumed':True}
(root/'maps2644/qa/live-verification.json').write_text(json.dumps(report,indent=2)+'\n')
