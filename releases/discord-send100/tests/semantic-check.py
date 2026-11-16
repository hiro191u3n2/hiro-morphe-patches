"""Execute exact affected HBC function instructions with mocked React/RN imports.
This is a host semantic harness, NOT a real Hermes VM or Android/UI test.
"""
import argparse, hashlib, itertools, json, pathlib, struct, sys, tempfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--bundle", required=True, type=pathlib.Path,
                    help="Original index.android.bundle from the verified Venus APK")
parser.add_argument("--hbc-dir", required=True, type=pathlib.Path,
                    help="scripts/hbc directory from Venus-Patches commit 987b7a22d96d41a0ba80c060b485291e1c7bcc4b")
parser.add_argument("--output", type=pathlib.Path, help="Optional JSON result file; stdout always prints the result")
args = parser.parse_args()
ROOT = pathlib.Path(__file__).resolve().parent
SPEC = json.loads((ROOT / "patch-spec.json").read_text())
for dependency in ("hbc.py", "ops.txt"):
    if not (args.hbc_dir / dependency).is_file():
        parser.error("Missing external Venus inspection dependency: " + str(args.hbc_dir / dependency))
digest = hashlib.sha256()
with args.bundle.open("rb") as source:
    for block in iter(lambda: source.read(1024 * 1024), b""):
        digest.update(block)
if digest.hexdigest() != SPEC["inputBundleSha256"]:
    parser.error("Unsupported input bundle SHA-256; this test requires the exact original Venus 1.21.1 input")
sys.path.insert(0, str(args.hbc_dir.resolve()))
from hbc import Bundle, OPINFO
# The external reader caches its analysis. Keep that cache temporary, never beside source/APK files.
CACHE = tempfile.TemporaryDirectory(prefix="hiro-discord-hbc-")
B = Bundle(str(args.bundle.resolve()), str(pathlib.Path(CACHE.name) / "index.pkl"))
H = B.h
PATCHES={p['functionId']:p for p in SPEC['patches']}
UNDEF=object(); SENTINEL=object()
def truth(x): return x is not UNDEF and x is not None and x is not False and x != '' and x != 0
def same(a,b):
 if a is b: return True
 if isinstance(a,(dict,list)) or callable(a) or isinstance(b,(dict,list)) or callable(b): return False
 return type(a) is type(b) and a==b
class Env:
 def __init__(self,parent,slots=None): self.parent=parent;self.slots=slots or {}
class Closure:
 def __init__(self,vm,fid,env):self.vm=vm;self.fid=fid;self.env=env
 def __call__(self,*args):return self.vm.run(self.fid,self.env,args)
class VM:
 def __init__(self,patched=False):self.patched=patched;self.trace=[];self.imperatives=[]
 def run(self,fid,env,args=()):
  f=H.func(fid); body=bytearray(H.b[f['offset']:f['offset']+f['size']])
  if self.patched and fid in PATCHES:
   p=PATCHES[fid]; at=p['relativeOffset']; old=bytes.fromhex(p['oldHex']); new=bytes.fromhex(p['newHex']); assert body[at:at+len(old)]==old; body[at:at+len(old)]=new
  il=[]; at=0
  while at<len(body):
   name,fields,size=OPINFO[body[at]]; il.append((at,name,[struct.unpack_from(fmt,body,at+offset)[0] for offset,fmt in fields])); at+=size
  assert at==len(body)
  ins={p:(n,a) for p,n,a in il}; nxt={p:il[i+1][0] if i+1<len(il) else f['size'] for i,(p,_,_) in enumerate(il)}
  r={};pc=0;steps=0
  while True:
   steps+=1
   if steps>4000:raise RuntimeError('instruction budget exceeded')
   name,a=ins[pc];n=nxt[pc];self.trace.append((fid,pc))
   if name=='GetParentEnvironment':
    e=env
    for _ in range(a[1]):e=e.parent
    r[a[0]]=e
   elif name=='CreateFunctionEnvironment':r[a[0]]=Env(env)
   elif name=='LoadParam':r[a[0]]=args[a[1]-1] if len(args)>=a[1] else UNDEF
   elif name in ('StoreToEnvironment','StoreNPToEnvironment'):r[a[0]].slots[a[1]]=r[a[2]]
   elif name=='LoadFromEnvironment':r[a[0]]=r[a[1]].slots[a[2]]
   elif name=='GetGlobalObject':r[a[0]]={'Symbol':{'for':lambda s:SENTINEL}}
   elif name.startswith(('GetById','TryGetById')):
    ob=r[a[1]];key=H.string(a[-1]);r[a[0]]=len(ob) if key=='length' and isinstance(ob,(list,str)) else ob.get(key,UNDEF)
   elif name=='GetByIndex':r[a[0]]=r[a[1]][a[2]]
   elif name.startswith(('LoadConstString','LoadConstStringLongIndex')):r[a[0]]=H.string(a[1])
   elif name in ('LoadConstUInt8','LoadConstInt','LoadConstDouble'):r[a[0]]=a[1]
   elif name=='LoadConstUndefined':r[a[0]]=UNDEF
   elif name=='LoadConstZero':r[a[0]]=0
   elif name=='LoadConstTrue':r[a[0]]=True
   elif name=='LoadConstFalse':r[a[0]]=False
   elif name=='LoadConstNull':r[a[0]]=None
   elif name=='NewObject':r[a[0]]={}
   elif name.startswith('NewObjectWithBuffer'):
    keys=H.keys(a[1]);vals=H._literal(H.litOff+a[2],len(keys),H.string);r[a[0]]=dict(zip(keys,vals))
   elif name=='PutOwnBySlotIdx':
    ob=r[a[0]];key=list(ob.keys())[a[2]];ob[key]=r[a[1]]
   elif name=='NewArray':r[a[0]]=[UNDEF]*a[1]
   elif name=='DefineOwnInDenseArray':r[a[0]][a[2]]=r[a[1]]
   elif name=='PutByValStrict':r[a[0]][r[a[1]]]=r[a[2]]
   elif name in ('CreateClosure','CreateClosureLongIndex'):r[a[0]]=Closure(self,a[2],r[a[1]])
   elif name in ('Call1','Call2','Call3','Call4'):
    fn=r[a[1]]
    if not callable(fn):raise RuntimeError(('not callable',fid,pc,a,fn))
    r[a[0]]=fn(*(r[x] for x in a[3:]))
   elif name=='Mov':r[a[0]]=r[a[1]]
   elif name=='Not':r[a[0]]=not truth(r[a[1]])
   elif name=='Greater':r[a[0]]=r[a[1]]>r[a[2]]
   elif name=='Mul':r[a[0]]=r[a[1]]*r[a[2]]
   elif name=='Add':r[a[0]]=r[a[1]]+r[a[2]]
   elif name=='StrictNeq':r[a[0]]=not same(r[a[1]],r[a[2]])
   elif name=='StrictEq':r[a[0]]=same(r[a[1]],r[a[2]])
   elif name in ('Jmp','JmpLong'):n=pc+a[0]
   elif name in ('JmpTrue','JmpTrueLong') and truth(r[a[1]]):n=pc+a[0]
   elif name in ('JmpFalse','JmpFalseLong') and not truth(r[a[1]]):n=pc+a[0]
   elif name in ('JStrictNotEqual','JStrictNotEqualLong') and not same(r[a[1]],r[a[2]]):n=pc+a[0]
   elif name in ('JStrictEqual','JStrictEqualLong') and same(r[a[1]],r[a[2]]):n=pc+a[0]
   elif name.startswith(('JmpTrue','JmpFalse','JStrictNotEqual','JStrictEqual')):pass
   elif name=='Ret':return r[a[0]]
   else:raise RuntimeError(('unsupported',fid,pc,name,a))
   pc=n

def scenario(fid,patched,reduced,canVoice,hasText,attachments,edit,require,cooldown):
 vm=VM(patched)
 calls={'send':0}
 def send():calls['send']+=1
 setter=lambda value:None
 imperatives=[]
 react={'useState':lambda value:[value,setter], 'useMemo':lambda fn,deps:fn(), 'useImperativeHandle':lambda ref,fn:imperatives.append(fn())}
 tokens={'CHAT_INPUT_SEND_BUTTON_WIDTH':44,'CHAT_INPUT_SEND_BUTTON_HEIGHT':40,'CHAT_INPUT_ACTION_BUTTON_MARGIN':4}
 deps=B.deps[12908]
 imports={576:{'c':lambda size:[SENTINEL]*size},4913:{'useToken':lambda token:token},587:{'modules':{'mobile':tokens}},504:{'useStateFromStores':lambda stores,fn,*rest:fn()},12330:'ChatInputActionButton',5139:{'SendMessageIcon':'SendMessageIcon'},1128:{'intl':{'string':lambda s:s},'t':{'TXNS7S':'Send message'}},12909:'VoiceMessageButton',4922:{'TransitionGroup':'TransitionGroup'}}
 def importer(mid):
  if mid not in imports:raise RuntimeError(('unmocked import',mid))
  return imports[mid]
 def jsx(component,props,*key):return {'component':component,'props':props}
 env=Env(None,{0:importer,1:importer,2:deps,3:lambda arr,count:arr,4:react,5:'View',6:{'useReducedMotion':reduced},7:{'isChannelOnCooldown':lambda ch:cooldown},8:20,9:jsx,10:{'BUTTON_SEND':'send','BUTTON_SEND_DISABLED':'disabled','BUTTON_SEND_VOICE_MESSAGE':'voice','BUTTON_SEND_VOICE_MESSAGE_DISABLED':'voice-disabled'},11:lambda w,h:{'button':{'width':w,'height':h},'buttonActive':'active-style','iconActive':'icon-style'},12:'Inner',13:'renderItem',14:'getItemKey',15:'FloatingSlot'})
 props={'channel':{'id':'channel-1'},'defaultValue':'text' if hasText else '', 'canSendVoiceMessage':canVoice,'hasPendingAttachments':attachments,'hasPendingEdit':edit,'onSendMessage':send,'requireTextContent':require,'ref':'input-ref'}
 out=vm.run(fid,env,[props]);assert calls['send']==0,'render unexpectedly sends'
 assert len(imperatives)==1 and imperatives[0].get('setHasText') is setter,imperatives
 if reduced or patched:
  assert out['component']=='View',out
  child=out['props']['children'];expected= (not cooldown or edit) and (hasText or attachments or not require)
  voice=canVoice and not expected
  assert out['props']['style']=={'width':(40 if voice else 44)+8,'height':40,'alignItems':'center','justifyContent':'center'},out['props']['style']
  assert child['component']==('VoiceMessageButton' if voice else 'ChatInputActionButton'),child
  if not voice:
   assert child['props']['disabled']==(not expected),(child,expected)
   assert child['props']['onPress'] is send
   assert child['props']['IconComponent']=='SendMessageIcon'
   # Sending remains exclusively an explicit callback, with no additional transport.
   child['props']['onPress']();assert calls['send']==1
  else:assert child['props']=={'disabled':cooldown,'channelId':'channel-1'},child
 else:
  assert out['component']=='FloatingSlot',out
  assert out['props']['children']['component']=='TransitionGroup',out
 return out,vm.trace

def normalize(x):
 if callable(x):return '<original-callback>'
 if isinstance(x,dict):return {k:normalize(v) for k,v in x.items()}
 if isinstance(x,list):return [normalize(v) for v in x]
 return x
cases=0
for flags in itertools.product([False,True],repeat=6):
 for fid in [62650,62651]:
  original,trace=scenario(fid,False,True,*flags)
  for reduced in [False,True]:
   patched,ptrace=scenario(fid,True,reduced,*flags)
   assert normalize(original)==normalize(patched),(fid,flags,reduced)
   static_start=1001 if fid==62650 else 598
   assert (fid,static_start) in ptrace
   assert not any(f==fid and (778<=p<1001 if fid==62650 else 505<=p<598) for f,p in ptrace)
   cases+=1
  # Confirm the non-reduced source actually exercises the removed animated rendering path.
  scenario(fid,False,False,*flags)
result={'test':'exact_hbc_function_host_interpreter','casesCompared':cases,'compilerVariants':[62650,62651],'stateDimensions':['canSendVoiceMessage','hasText','hasPendingAttachments','hasPendingEdit','requireTextContent','cooldown'],'patchedReducedMotionSettings':[False,True],'assertions':['Output equals original ReducedMotion=true','send callback identity retained','render never sends','disabled and cooldown state preserved','voice button branch preserved','explicit dimensions preserved','imperative input setter preserved','animation path skipped'],'result':'PASS','limitations':'Mocks React/RN imports; not Hermes VM, Android runtime, or device/UI reproduction.'}
result['inputBundleSha256'] = SPEC['inputBundleSha256']
result['dependencyCommit'] = '987b7a22d96d41a0ba80c060b485291e1c7bcc4b'
if args.output:
 args.output.parent.mkdir(parents=True, exist_ok=True)
 args.output.write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))
H.b.close()
H.f.close()
CACHE.cleanup()
