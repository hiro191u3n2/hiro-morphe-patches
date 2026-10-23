from pathlib import Path
from collections import defaultdict
import zipfile, hashlib, base64, json, io, re
apk=Path('tiktok4743-work/apk/TikTok_47.4.3_original.apk')
out=Path('tiktok4743-work/runtime-audit/dynamic-modules')
needles=defaultdict(list);subjects={};metadata={}
with zipfile.ZipFile(apk) as z:
 for module in ['live_cast','pitaya','ship']:
  name='df_'+module
  raw=z.read('assets/dynamic_feature_install/'+name+'.json')
  metadata[name]=json.loads(raw)
  (out/(name+'.json')).write_bytes(raw)
  for abi in ['arm64-v8a','armeabi-v7a']:
   path='lib/'+abi+'/libdex_df_'+module+'.so';raw=z.read(path)
   subjects[path]=raw
   with zipfile.ZipFile(io.BytesIO(raw)) as inner:
    for entry in inner.infolist():
     if entry.filename.endswith('.dex'):subjects[path+'!/'+entry.filename]=inner.read(entry)
 for path,raw in subjects.items():
  for alg in ['md5','sha1','sha256']:
   digest=hashlib.new(alg,raw).digest()
   for encoding,pattern in [('raw',digest),('hex',digest.hex().encode()),('base64',base64.b64encode(digest))]:
    needles[pattern].append({'subject':path,'algorithm':alg,'encoding':encoding})
 matcher=re.compile(b'|'.join(re.escape(x) for x in needles))
 matches=[];entries=0
 for entry in z.infolist():
  if entry.is_dir():continue
  data=z.read(entry);entries+=1
  for match in matcher.finditer(data):
   matches.append({'entry':entry.filename,'offset':match.start(),'digest_refs':needles[match.group()]})
 result={'schema':'tiktok4743-module-external-digest-search-v1','input_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'subjects':{name:{'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest()} for name,raw in subjects.items()},'unique_patterns':len(needles),'searched_decompressed_entries':entries,'matches':matches,'dynamic_module_metadata':metadata}
 (out/'digest-references.json').write_text(json.dumps(result,indent=2)+'\n')
 print(json.dumps({'subjects':len(subjects),'unique_patterns':len(needles),'entries':entries,'matches':len(matches),'matched_entries':sorted(set(m['entry'] for m in matches)),'metadata_fields':{k:sorted(v) for k,v in metadata.items()}},indent=2))
