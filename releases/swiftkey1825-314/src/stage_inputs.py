#!/usr/bin/env python3
"""Snapshot two reviewed public JMdict source files to a new inputs branch only.

Does not build, publish a release, alter main/dev, or update any Manager feed.
The public source bytes must match the locally reviewed exact pins.
"""
import hashlib,json,os,re,subprocess,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
CONFIG=Path(__file__).resolve().parents[1]/'stage-inputs.json'
REPO='hiro191u3n2/hiro-morphe-patches';BRANCH='inputs/swiftkey1825'
ALLOW={
 'inputs/swiftkey1825/JMdict_e.gz':('https://www.edrdg.org/pub/Nihongo/JMdict_e.gz',10589583,'739ce5a15038e063f6db819fe61a70c0f60c2039569b651f0a0f5601fdbc0b39'),
 'inputs/swiftkey1825/JMDICT_LICENSE.html':('https://www.edrdg.org/edrdg/licence.html',13587,'52f60ea9ca68170a2f0663d7dba381ebf1bd57c17a3347dfe21153865c156692')}
def run(args):return subprocess.check_output(list(map(str,args)),cwd=ROOT,text=True).strip()
def git(*args):return run(['git','-c','credential.helper=','-c','credential.helper=!gh auth git-credential',*args])
def main():
 cfg=json.loads(CONFIG.read_text());assert cfg['schema']=='swiftkey1825-public-dictionary-snapshots-v1'
 assert cfg['repository']==REPO and cfg['branch']==BRANCH
 assert {f['path']:(f['url'],f['bytes'],f['sha256']) for f in cfg['files']}==ALLOW
 assert len(cfg['files'])==len(ALLOW)
 source=os.environ['GITHUB_SHA'];assert re.fullmatch('[0-9a-f]{40}',source)
 assert git('rev-parse','HEAD')==source
 assert run(['gh','api',f'repos/{REPO}/git/ref/heads/main','--jq','.object.sha'])==source,'main advanced; re-stage deliberately'
 feed=json.loads((ROOT/'patches-bundle.json').read_text())
 assert feed['version']==cfg['baseline_version'] and feed['download_url']==cfg['baseline_download_url'],'Baseline advanced'
 assert not git('ls-remote','--heads','origin',f'refs/heads/{BRANCH}'),'Input branch already exists; inspect rather than overwrite'
 for f in cfg['files']:
  with urllib.request.urlopen(f['url'],timeout=60) as response:data=response.read(f['bytes']+1)
  assert len(data)==f['bytes'] and hashlib.sha256(data).hexdigest()==f['sha256'],'Source changed: '+f['path']
  path=ROOT/f['path'];path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
 paths=sorted(ALLOW)
 git('add','--sparse','--',*paths)
 assert sorted(git('diff','--cached','--name-only').splitlines())==paths,'Unexpected staged files'
 git('-c','user.name=github-actions[bot]','-c','user.email=41898282+github-actions[bot]@users.noreply.github.com',
     'commit','-m','inputs: pin reviewed JMdict 2026-10-28 dictionary and license for SwiftKey1825')
 commit=git('rev-parse','HEAD');assert git('show','-s','--format=%P',commit)==source
 assert sorted(git('diff-tree','--no-commit-id','--name-only','-r',commit).splitlines())==paths
 git('push','origin',f'{commit}:refs/heads/{BRANCH}')
 assert run(['gh','api',f'repos/{REPO}/git/ref/heads/{BRANCH}','--jq','.object.sha'])==commit
 receipt={'schema':cfg['schema'],'status':'snapshots_committed','source_commit':source,'snapshot_commit':commit,
  'main_dev_feeds_not_modified':True,'files':[{**f,'immutable_url':f'https://raw.githubusercontent.com/{REPO}/{commit}/{f["path"]}'} for f in cfg['files']]}
 dest=ROOT/'.stage-swiftkey1825';dest.mkdir(exist_ok=True)
 (dest/'snapshot-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
 print(json.dumps(receipt,indent=2))
if __name__=='__main__':main()
