#!/usr/bin/env python3
"""Reproducible supplemental dictionary union; no network except explicit --download.

Preserve all baseline readings/candidate order and append compatible vocabulary.
Duplicate key: runtime-normalized reading + NFC surface. Surface text itself is
retained, never NFKC-folded, except U+00AD soft hyphens removed from new source
surfaces before duplicate checks. Separate English table is a duplicate exclusion set.
"""
from __future__ import annotations
import argparse,bz2,collections,csv,gzip,hashlib,io,json,re,unicodedata,urllib.request,zipfile
from pathlib import Path
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parent
READING=re.compile(r'[\u3041-\u3096ー]{2,32}\Z')
CONTROL=re.compile(r'[\x00-\x1f\x7f]')
BASELINE_SHA='1f15766663f5cbf1de8bce2df6a2b8f5e80a488166950979b92ecac8f9e27ca0'
ENGLISH_SHA='895eb9d40c12bda31b77d9992adc38add46cd06819857e73ebe3785d34e9b074'
def sha(b):return hashlib.sha256(b).hexdigest()
def norm_read(s):
 s=unicodedata.normalize('NFKC',s).lower()
 return ''.join(chr(ord(c)-0x60) if '\u30a1'<=c<='\u30f6' else c for c in s)
def clean_new_surface(s):
 # Soft hyphen is a discretionary line-break artifact, not a word character.
 # Preserve all other format characters (e.g. Persian ZWNJ) and baseline bytes.
 return s.replace('\u00ad','')
def unescape(s):return re.sub(r'\\u(?:([0-9a-fA-F]{4})|\{([0-9a-fA-F]+)\})',lambda m:chr(int(m[1] or m[2],16)),s)
def tsv_stream(stream,read_col=0,surface_col=1):
 for cols in csv.reader(stream,delimiter='\t',quoting=csv.QUOTE_NONE):
  if not cols or cols[0].startswith('#'):continue
  if len(cols)<=max(read_col,surface_col):raise ValueError('Short TSV row')
  yield cols[read_col],cols[surface_col],None

def pairs(source,inputs):
 kind=source['format'];path=inputs/source['file']
 if kind=='sudachi':
  with zipfile.ZipFile(path) as z:
   members=[n for n in z.namelist() if n.endswith('.csv')];assert len(members)==1
   with io.TextIOWrapper(z.open(members[0]),encoding='utf-8') as f:
    for row in csv.reader(f):
     if len(row)!=19:raise ValueError('Unexpected Sudachi schema')
     yield unescape(row[11]),unescape(row[4]),'nonlexical_pos' if row[5] in ('空白','補助記号','記号') else None
 elif kind=='ut':
  with bz2.open(path,'rt',encoding='utf-8') as f:yield from tsv_stream(f,0,4)
 elif kind=='unidic':
  with zipfile.ZipFile(path) as z:
   for name in sorted(n for n in z.namelist() if n.endswith('.tsv')):
    with io.TextIOWrapper(z.open(name),encoding='utf-8-sig') as f:yield from tsv_stream(f)
 elif kind=='mecab':
  with path.open(encoding='utf-8',newline='') as f:
   # Upstream emits literal comma joins (no CSV quoting); preserve literal quotes.
   for row in csv.reader(f,quoting=csv.QUOTE_NONE):
    if len(row)!=13:raise ValueError('Unexpected MeCab schema')
    yield row[11],row[0],None
 elif kind=='jmdict':
  with gzip.open(path,'rb') as f:
   for _,entry in ET.iterparse(f,events=('end',)):
    if entry.tag!='entry':continue
    spellings=[e.text for e in entry.findall('k_ele/keb')]
    for reading in entry.findall('r_ele'):
     r=reading.findtext('reb');restrictions=[n.text for n in reading.findall('re_restr')]
     if reading.find('re_nokanji') is not None or not spellings:surfaces=[r]
     elif restrictions:
      if not set(restrictions)<=set(spellings):raise ValueError('Invalid JMdict re_restr')
      surfaces=[s for s in spellings if s in restrictions]
     else:surfaces=spellings
     for surface in surfaces:yield r,surface,None
    entry.clear()
 else:raise ValueError('Unknown format '+kind)

def baseline_data(path,expected,strict=True):
 data=path.read_bytes();assert sha(data)==expected, 'Baseline hash differs'
 rows={}
 for line in data.decode().splitlines():
  cells=line.split('\t');r=cells[0];assert not strict or (READING.fullmatch(r) and r==norm_read(r))
  assert r not in rows and len(cells)>1
  values={}
  for value in cells[1:]:
   key=unicodedata.normalize('NFC',value)
   assert key not in values and value and not CONTROL.search(value),'Baseline invalid/normalized duplicate'
   values[key]=value
  rows[r]=values
 assert list(rows)==sorted(rows),'Baseline unsorted'
 return rows

def main():
 p=argparse.ArgumentParser();p.add_argument('--inputs',type=Path,required=True);p.add_argument('--baseline',type=Path,required=True);p.add_argument('--english',type=Path,required=True);p.add_argument('--baseline-license',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--manifest',type=Path,default=ROOT/'source_manifest.json');p.add_argument('--download',action='store_true');a=p.parse_args()
 m=json.loads(a.manifest.read_text());a.output.mkdir(parents=True,exist_ok=True)
 for item in m['files']:
  path=a.inputs/item['file']
  if not path.exists() and a.download:
   if item.get('snapshot'):raise RuntimeError('Pinned mutable-source snapshot must be supplied: '+item['file'])
   path.parent.mkdir(parents=True,exist_ok=True)
   with urllib.request.urlopen(item['url'],timeout=180) as r:raw=r.read()
   assert sha(raw)==item['sha256'],'Downloaded input hash differs';path.write_bytes(raw)
  assert path.is_file() and sha(path.read_bytes())==item['sha256'],'Pinned input differs: '+str(path)
 words=baseline_data(a.baseline,BASELINE_SHA);english=baseline_data(a.english,ENGLISH_SHA,False)
 baseline_count=sum(map(len,words.values()));baseline_readings=len(words)
 report={'baseline_readings':baseline_readings,'baseline_pairs':baseline_count,'baseline_sha256':BASELINE_SHA,'english_sha256':ENGLISH_SHA,'sources':[],'policy':m['policy']}
 for source in m['sources']:
  stats=collections.Counter();stats['rows']=0;added_sample=[]
  for reading,surface,reason in pairs(source,a.inputs):
   stats['rows']+=1
   if reason:stats['rejected_'+reason]+=1;continue
   r=norm_read(reading)
   if not READING.fullmatch(r):stats['rejected_reading_outside_runtime_2_to_32_hiragana_or_long_mark']+=1;continue
   if '\u00ad' in surface:
    stats['soft_hyphen_cleaned_source_rows']+=1
    stats['soft_hyphen_removed_codepoints']+=surface.count('\u00ad')
    surface=clean_new_surface(surface)
   if not surface or surface.isspace() or CONTROL.search(surface):stats['rejected_empty_or_control_surface']+=1;continue
   if any(0xd800<=ord(c)<=0xdfff for c in surface):stats['rejected_surrogate_surface']+=1;continue
   key=unicodedata.normalize('NFC',surface)
   if key in words.get(r,{}):stats['duplicate_japanese_or_prior_source']+=1;continue
   if key in english.get(r,{}):stats['duplicate_existing_english']+=1;continue
   words.setdefault(r,{})[key]=surface;stats['added_pairs']+=1
   if len(added_sample)<15:added_sample.append([r,surface])
  result={'source':source['id'],**dict(stats),'sample_added':added_sample};report['sources'].append(result);print(json.dumps(result,ensure_ascii=False),flush=True)
 # Whole-output structural check is performed while serializing, not spot tests.
 target=a.output/'words_ja.tsv';total=0;max_count=0;max_keys=[]
 with target.open('wb') as out:
  for reading in sorted(words):
   vals=list(words[reading].values());count=len(vals);assert count and len(set(map(lambda x:unicodedata.normalize('NFC',x),vals)))==count
   if count>max_count:max_count=count;max_keys=[reading]
   elif count==max_count:max_keys.append(reading)
   out.write(('\t'.join([reading]+vals)+'\n').encode('utf-8'));total+=count
 # Baseline prefix/order proof; no old term is deleted or reordered.
 for line in a.baseline.read_text().splitlines():
  cells=line.split('\t');assert list(words[cells[0]].values())[:len(cells)-1]==cells[1:]
 newpairs=sum(s.get('added_pairs',0) for s in report['sources']);assert baseline_count+newpairs==total
 report.update(status='PASSED',readings=len(words),pairs=total,added_pairs=newpairs,bytes=target.stat().st_size,sha256=sha(target.read_bytes()),max_candidates_per_reading=max_count,max_candidate_readings=max_keys[:10],baseline_order_preserved=True,duplicate_pairs=0,existing_english_new_duplicates=0,soft_hyphen_cleaned_source_rows=sum(s.get('soft_hyphen_cleaned_source_rows',0) for s in report['sources']),soft_hyphen_removed_codepoints=sum(s.get('soft_hyphen_removed_codepoints',0) for s in report['sources']),normalization='reading NFKC/lowercase/kata-to-hira; remove U+00AD from new surfaces; surface NFC comparison only; baseline spelling retained')
 report['examples']={r:list(words.get(r,{}).values()) for r in ['しるかつ','ちいぽけ','でぃーぴん','しぶやらぶず','あさのしゅんや','じんこうちのう','こーひー','とうきょう']}
 (a.output/'merge_report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
 # Existing complete notices are retained; append provenance and exact upstream notices.
 notice=a.baseline_license.read_text()+'\n\n===== SwiftKey Japanese vocabulary update 2026-10-29 =====\n'
 notice+='Derived data: readings normalized, incompatible nonlexical/control rows rejected, U+00AD soft hyphens removed from new source surfaces before duplicate checks, duplicate reading/surface pairs removed; all other surface format characters and existing candidate order preserved. Source and license details follow.\n'
 for source in m['sources']:notice+='\n'+source['id']+'\n'+source['attribution']+'\n'
 for item in m['files']:
  if item.get('notice'):notice+='\n----- '+item['file']+' -----\n'+(a.inputs/item['file']).read_text(encoding='utf-8')+'\n'
 notice+='\nSource data and reproducible extraction/merge script are distributed with this release. Wikipedia-derived portions: attribution to Japanese Wikipedia contributors; see upstream project links and article revision histories. CC BY-SA conditions apply to applicable portions; no exclusive license is asserted over the entire aggregate.\n'
 (a.output/'words_LICENSE.txt').write_text(notice)
 print(json.dumps({k:v for k,v in report.items() if k not in ('sources','examples','policy')},ensure_ascii=False),flush=True)
if __name__=='__main__':main()
