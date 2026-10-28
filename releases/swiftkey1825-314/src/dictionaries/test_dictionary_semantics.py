#!/usr/bin/env python3
"""Small adversarial fixtures for source relation and literal-field fidelity."""
import gzip,importlib.util,json,tempfile
from pathlib import Path
spec=importlib.util.spec_from_file_location('merge_dictionaries',Path(__file__).with_name('merge_dictionaries.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
xml='''<JMdict><entry><k_ele><keb>甲</keb></k_ele><k_ele><keb>乙</keb></k_ele><r_ele><reb>こう</reb><re_restr>甲</re_restr></r_ele><r_ele><reb>おつ</reb><re_restr>乙</re_restr></r_ele><r_ele><reb>かな</reb><re_nokanji/></r_ele></entry><entry><r_ele><reb>かなだけ</reb></r_ele></entry><entry><k_ele><keb>両</keb></k_ele><k_ele><keb>兩</keb></k_ele><r_ele><reb>りょう</reb></r_ele></entry></JMdict>'''
with tempfile.TemporaryDirectory() as t:
 r=Path(t);(r/'x.gz').write_bytes(gzip.compress(xml.encode()))
 actual=list(m.pairs({'format':'jmdict','file':'x.gz'},r))
 assert actual==[('こう','甲',None),('おつ','乙',None),('かな','かな',None),('かなだけ','かなだけ',None),('りょう','両',None),('りょう','兩',None)]
 # Upstream jawiki format literally joins fields without CSV quoting.
 (r/'mecab.csv').write_text('"gusto,1288,1288,4569,名詞,固有名詞,一般,*,*,*,"gusto,ぐすと,ぐすと\n')
 assert list(m.pairs({'format':'mecab','file':'mecab.csv'},r))==[('ぐすと','"gusto',None)]
assert m.unescape(r'A\u002CB\u{1F600}')=='A,B😀'
assert m.norm_read('ｺｰﾋｰ')=='こーひー'
assert m.norm_read('ガ')=='が'
assert not m.READING.fullmatch('aこーひー')
assert m.READING.fullmatch('こーひー')
assert m.CONTROL.search('bad\tword')
assert m.clean_new_surface('複素\u00ad八元数')=='複素八元数'
assert m.clean_new_surface('فارسی\u200cزبان')=='فارسی\u200cزبان'
assert m.clean_new_surface('A-B')=='A-B'
assert m.clean_new_surface('x\u00ady\u00adz')=='xyz'
print(json.dumps({'status':'PASSED','jmdict_restrictions_and_no_kanji':True,'literal_quote_surface_preserved':True,'both_sudachi_escape_formats':True,'runtime_reading_normalization':True,'only_new_surface_soft_hyphen_removed':True,'persian_zwnj_preserved':True}))
