#!/usr/bin/env python3
import ctypes,ctypes.util,hashlib,importlib.util,json,pathlib,tempfile,sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('instrument_geo',ROOT/'instrument_geometry.py');mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
lua=ctypes.CDLL(ctypes.util.find_library('lua5.4'))
lua.luaL_newstate.restype=ctypes.c_void_p
lua.luaL_openlibs.argtypes=[ctypes.c_void_p];lua.luaL_loadstring.argtypes=[ctypes.c_void_p,ctypes.c_char_p]
lua.lua_pcallk.argtypes=[ctypes.c_void_p,ctypes.c_int,ctypes.c_int,ctypes.c_int,ctypes.c_longlong,ctypes.c_void_p]
lua.lua_tolstring.argtypes=[ctypes.c_void_p,ctypes.c_int,ctypes.c_void_p];lua.lua_tolstring.restype=ctypes.c_char_p;lua.lua_close.argtypes=[ctypes.c_void_p]
def run(code):
 s=lua.luaL_newstate();lua.luaL_openlibs(s)
 try:
  result=lua.luaL_loadstring(s,code.encode())
  if not result:result=lua.lua_pcallk(s,0,0,0,0,None)
  if result:raise AssertionError(lua.lua_tolstring(s,-1,None).decode())
 finally:lua.lua_close(s)
BASE='''
local dim=2;local packets={};local read=0;local completed=false;local declared=1;local faces=1;local source={}
for i=0,3999 do source[i]=i/16 end
local values={size=function()return 1240*dim end,get=function(self,i)read=read+1;return source[i]end}
local mesh={getAttributeData=function(self,semantic,start,count) assert(completed and semantic==(dim==3 and 0 or 13) and start==0 and count==8193);return values end}
local ids={size=function()return 5 end,get=function(self,i)return i end}
local comp={templateMesh=mesh,faceIds=ids,entity={name="face"}}
local result={getFaceCount=function()return faces end}
Amaz={VertexAttribType={TEXCOORD7=13,POSITION=0},Algorithm={getAEAlgorithmResult=function()return result end},MessageCenter={sendMessage=function(id,nonce,ordinal,text)
 assert(id==1431062273 and nonce==314 and ordinal==#packets+1 and #text<=8192);table.insert(packets,text)
 -- Transport callback may mutate SDK-owned buffers: observer must already own its copy.
 for i=0,3999 do source[i]=999 end
 end}}
'''
checks=0
with tempfile.TemporaryDirectory(prefix='ulike-geometry-diagnostic-') as temp:
 for style,archive in [('natural',sys.argv[1]),('purity',sys.argv[2])]:
  out=pathlib.Path(temp)/style;manifest=mod.instrument(archive,style,out,314);checks+=1
  assert len(manifest['expected_features'])==(1 if style=='natural' else 5)
  for item in manifest['patched']:
   src=(out/item['path']).read_text();feature=item['path'].split('/')[0]
   # Actual supplied script still loads; no original proprietary script is embedded here.
   run('local e=(function()\n'+src+'\nend)();assert(e.EffectFaceMakeupSystemScript)');checks+=1
   featureBase=BASE+('dim=3;' if feature=='AmazingFeature6' else '')
   stub='local EffectFaceMakeupSystemScript={onLateUpdate=function()completed=true end};'+mod.appendix(feature,314)
   post='local s=setmetatable({comps={comp}},{__index=EffectFaceMakeupSystemScript});s:onLateUpdate(nil,0);assert(read==1240*dim);assert(#packets==23);assert(packets[3]:find("0,0.0625,0.125",1,true));assert(packets[23]:find("|END|-1|22",1,true));s:onLateUpdate(nil,0);assert(#packets==23)'
   run(featureBase+stub+post);checks+=1
   for injection in ['comp.templateMesh=nil','values.size=function()return 32769 end','values.size=function()return 0 end','values.get=function()return 0/0 end','ids.get=function()return 256 end','comp.entity.name=""','faces=11','values.size=function()return 2479 end']:
    run(featureBase+injection+';'+stub+';local s=setmetatable({comps={comp}},{__index=EffectFaceMakeupSystemScript});s:onLateUpdate(nil,0);assert(#packets==1 and packets[1]:find("|ERROR|-1|",1,true))');checks+=1
   run(featureBase+stub+';faces=0;local s=setmetatable({comps={}},{__index=EffectFaceMakeupSystemScript});s:onLateUpdate(nil,0);assert(#packets==2 and packets[1]:find("|BEGIN|-1|0,0",1,true))');checks+=1
   if style=='natural':
    target=str(pathlib.Path(sys.argv[3]).resolve());run(BASE+stub+post+';local f=assert(io.open('+json.dumps(target)+',"w"));for _,s in ipairs(packets)do f:write(s,"\\n")end;f:close()');checks+=1
 for nonce in [0,-1,2147483648,True,1.5]:
  try:mod.appendix('AmazingFeature1',nonce);raise AssertionError('invalid nonce accepted')
  except ValueError:checks+=1
 print(json.dumps({'checks':checks,'actual_supplied_scripts_loaded':6,'host_lua':'5.4 explicit SDK mocks','phone_execution':False}))
