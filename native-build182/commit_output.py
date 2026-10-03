import base64,hashlib,json,os,urllib.request
from pathlib import Path
repo='hiro191u3n2/hiro-morphe-patches'
branch='work/ulike-native-readback182'
commit=os.environ['GITHUB_SHA']
def api(path,data=None,method=None):
    request=urllib.request.Request('https://api.github.com/repos/'+repo+path,headers={'Authorization':'Bearer '+os.environ['GH_TOKEN'],'Accept':'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28'},data=None if data is None else json.dumps(data).encode(),method=method)
    with urllib.request.urlopen(request,timeout=45) as response:return json.load(response)
head=api('/git/ref/heads/'+branch)['object']['sha']
assert head==commit,'Source branch advanced; output not committed'
raw=Path('/tmp/libhiro_draw_readback.so').read_bytes()
assert raw[:6]==b'\x7fELF\x02\x01' and int.from_bytes(raw[18:20],'little')==183
assert 1024<len(raw)<1024*1024
receipt={'source_commit':commit,'ndk':'27.2.12479018','api':26,'abi':'arm64-v8a','repeat_build_identical':True,'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest(),'sources':json.loads(Path('native-build182/sources.json').read_text())}
files={'native-build182/output/libhiro_draw_readback.so.b64':base64.b64encode(raw).decode()+'\n','native-build182/output/BUILD_RECEIPT.json':json.dumps(receipt,indent=2)+'\n'}
base=api('/git/commits/'+commit)['tree']['sha']
tree=api('/git/trees',{'base_tree':base,'tree':[{'path':name,'mode':'100644','type':'blob','content':data} for name,data in files.items()]})
new=api('/git/commits',{'message':'Build original arm64 native draw capture module 182','tree':tree['sha'],'parents':[commit]})
assert api('/git/ref/heads/'+branch)['object']['sha']==commit
api('/git/refs/heads/'+branch,{'sha':new['sha'],'force':False},'PATCH')
print(json.dumps({'commit':new['sha'],'bytes':len(raw),'sha256':receipt['sha256']}))
