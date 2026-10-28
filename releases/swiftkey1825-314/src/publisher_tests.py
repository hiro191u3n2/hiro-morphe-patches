#!/usr/bin/env python3
"""Offline publication boundary tests, with corrupt inputs and advanced feeds."""
import copy, importlib.util, io, json, struct, tempfile, unittest, zipfile
from pathlib import Path
from unittest import mock
spec=importlib.util.spec_from_file_location('publisher',Path(__file__).with_name('publish1825.py'))
p=importlib.util.module_from_spec(spec);spec.loader.exec_module(p)

def expected():
 return {'schema':'swiftkey1825-publication-v1','swiftkey_version':p.VERSION,'bundle_version':p.BUNDLE,
  'baseline_bundle_version':p.PREVIOUS,'baseline_bundle_sha256':p.BASE_SHA256,
  'baseline_swiftkey_version':p.SINGLE_PREVIOUS,'baseline_swiftkey_sha256':p.SINGLE_BASE_SHA256,
  'change_plan_reviewed':True,'qa_contract_reviewed':True,'android_device_tested':False,'original_apk_apply_tested':False,
  'artifacts':{n:{'bytes':1,'sha256':'1'*64} for n in p.ASSETS},
  'allowed_changed_bundle_entries':sorted(p.REQUIRED_CHANGED),'allowed_changed_standalone_entries':sorted(p.REQUIRED_CHANGED),
  'allowed_added_bundle_entries':[],'allowed_added_standalone_entries':[],'qa_required_values':copy.deepcopy(p.REQUIRED_QA)}

def pack(version,variant):
 runtime=bytearray(b'dex\n035\0'+b'\0'*120);runtime[8]=variant;struct.pack_into('<I',runtime,96,91)
 return {p.MF:('Manifest-Version: 1.0\r\nVersion: '+version+'\r\nTimestamp: fixed\r\nDescription: test\r\n\r\n').encode(),
  p.EXTENSION:bytes(runtime),'classes.dex':b'dex\n035\0loader'+bytes([variant]),p.LOADER_CLASS:b'class'+bytes([variant]),p.WORDS:('あ\t亜'+('\t阿' if variant==2 else '')+'\n').encode(),
  p.LICENSE:b'old license'+(b' new licenses' if variant==2 else b''),'other-app/payload':b'preserved'}

class BoundaryTests(unittest.TestCase):
 def test_complete_manifest_and_weakened_contract(self):
  with tempfile.TemporaryDirectory() as d:
   path=Path(d)/'manifest.json';value=expected();path.write_text(json.dumps(value));self.assertEqual(p.load_expected(path)['bundle_version'],p.BUNDLE)
   mutations=[lambda x:x.update(baseline_bundle_sha256='0'*64),lambda x:x.update(change_plan_reviewed=False),
    lambda x:x.update(allowed_added_bundle_entries=['other-app/payload']),
    lambda x:x['qa_required_values'].update({'dictionary.duplicates_absent':False}),
    lambda x:x['qa_required_values'].update(device_tested=0),lambda x:x['artifacts'].pop(p.QA_NAME)]
   for mutate in mutations:
    value=expected();mutate(value);path.write_text(json.dumps(value))
    with self.assertRaises(RuntimeError):p.load_expected(path)
 def test_dictionary_real_preservation_and_rejections(self):
  old='あ\t亜\t阿\nい\t伊\n'.encode();new='あ\t亜\t阿\t唖\nい\t伊\nう\t宇\n'.encode()
  r=p.validate_dictionary(old,new);self.assertEqual((r['candidates'],r['added_candidates']),(5,2))
  for text in ['あ\t阿\t亜\t唖\nい\t伊\n','あ\t亜\t阿\t亜\nい\t伊\n',
   'あ\t亜\t阿\t唖\n','い\t伊\nあ\t亜\t阿\n','あ\t亜\t阿\nあ\t唖\nい\t伊\n','あ\t亜\t阿\nい\t伊\t\n']:
   with self.subTest(text=text),self.assertRaises(RuntimeError):p.validate_dictionary(old,text.encode())
  with self.assertRaises(RuntimeError):p.validate_dictionary(old,old)
 def test_archive_change_allowlist(self):
  old,new=pack(p.PREVIOUS,1),pack(p.BUNDLE,2)
  self.assertEqual(p.validate_archive_delta(old,new,p.PREVIOUS,p.BUNDLE,'bundle'),sorted(p.REQUIRED_CHANGED))
  for key in ['other-app/payload']:
   bad=copy.deepcopy(new);bad[key]+=b'changed'
   with self.assertRaises(RuntimeError):p.validate_archive_delta(old,bad,p.PREVIOUS,p.BUNDLE,'bundle')
  bad=copy.deepcopy(new);bad['new/file']=b'x'
  with self.assertRaises(RuntimeError):p.validate_archive_delta(old,bad,p.PREVIOUS,p.BUNDLE,'bundle')
 def test_feed_scopes_preserve_other_apps_and_stop_advancement(self):
  state={'manifest':{'version':p.PREVIOUS,'download_url':p.BASE_URL,'created_at':'2026-10-28T15:23:53',
   'description':'SwiftKey Beta：v1.8.24（9.13.16.4）\nULike：v1.9.119\n','unknown_key':{'keep':[1,2]}},
   'log':'# 1.0.313 (2026-10-28)\n\n* **SwiftKey Beta:** prior\n'}
  feed,log=p.metadata(state,'https://example.invalid/fixed.mpp','2026-10-29T00:00:00','Notes')
  self.assertEqual(feed['unknown_key'],state['manifest']['unknown_key']);self.assertIn('SwiftKey Beta：v1.8.25',feed['description'])
  self.assertTrue(log.endswith(state['log']))
  for app in p.APP_NAMES:self.assertTrue(p.has_changes_for(log,p.PREVIOUS,[app]))
  for app in p.OTHER_APPS:self.assertFalse(p.has_changes_for(log,p.PREVIOUS,[app]))
  self.assertFalse(p.has_changes_for(log,p.BUNDLE,list(p.APP_NAMES)))
  for key,value in [('version','1.0.315'),('download_url','https://example.invalid/other')]:
   bad=copy.deepcopy(state);bad['manifest'][key]=value
   with self.assertRaises(RuntimeError):p.metadata(bad,'url','2026-10-29T00:00:00','Notes')
 def test_source_extra_or_changed_inputs_rejected(self):
  names=['build1825.py','publish1825.py','publisher_tests.py','workflow.yml','runtime/tests/AuditDictionaryAbi.java']
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);pins={}
   for name in names:
    f=root/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_bytes(b'x');pins[name]={'bytes':1,'sha256':p.sha(b'x')}
   self.assertEqual(p.verify_source_files(root,{'source_files':pins}),5)
   (root/'unexpected.py').write_text('raise RuntimeError()')
   with self.assertRaises(RuntimeError):p.verify_source_files(root,{'source_files':pins})
 def test_main_dev_push_is_atomic_and_lease_guarded(self):
  helper=mock.Mock();states={'main':{'head':'a'*40},'dev':{'head':'b'*40}}
  with mock.patch.object(p,'prepare_commit',side_effect=lambda repo,state,files,msg:{**state,'head':state['head'][0].upper()*40}),mock.patch.object(p,'git',return_value='') as git:
   p.commit_feeds_atomically(helper,Path('/tmp'),states,{'main':{'a':b'x'},'dev':{'a':b'y'}},'reviewed update')
   pushes=[x for x in git.call_args_list if len(x.args)>1 and x.args[1]=='push'];self.assertEqual(len(pushes),1)
   args=pushes[0].args;self.assertIn('--atomic',args);self.assertIn('--force-with-lease=refs/heads/main:'+'a'*40,args)
   self.assertIn('--force-with-lease=refs/heads/dev:'+'b'*40,args)
   self.assertEqual(helper.check_heads.call_count,3)

if __name__=='__main__':unittest.main(verbosity=2)
