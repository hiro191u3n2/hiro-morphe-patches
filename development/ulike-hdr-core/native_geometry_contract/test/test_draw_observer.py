#!/usr/bin/env python3
import ctypes,ctypes.util,importlib.util,json,pathlib,tempfile,sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('instrument_geo',ROOT/'instrument_geometry.py');mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
lua=ctypes.CDLL(ctypes.util.find_library('lua5.4'));lua.luaL_newstate.restype=ctypes.c_void_p
lua.luaL_openlibs.argtypes=[ctypes.c_void_p];lua.luaL_loadstring.argtypes=[ctypes.c_void_p,ctypes.c_char_p];lua.lua_pcallk.argtypes=[ctypes.c_void_p,ctypes.c_int,ctypes.c_int,ctypes.c_int,ctypes.c_longlong,ctypes.c_void_p];lua.lua_tolstring.argtypes=[ctypes.c_void_p,ctypes.c_int,ctypes.c_void_p];lua.lua_tolstring.restype=ctypes.c_char_p;lua.lua_close.argtypes=[ctypes.c_void_p]
def run(code):
 state=lua.luaL_newstate();lua.luaL_openlibs(state)
 try:
  status=lua.luaL_loadstring(state,code.encode())
  if not status:status=lua.lua_pcallk(state,0,0,0,0,None)
  if status:raise AssertionError(lua.lua_tolstring(state,-1,None).decode())
 finally:lua.lua_close(state)
BASE='''
local dim=2;local packets={};local completed=false;local changed=false;local count=1240;local faces=1
local pos={size=function()return count*dim end,get=function(self,i) assert(not changed);return math.floor(i/dim)/16+(i%dim)/16 end}
local uv={size=function()return count*2 end,get=function(self,i) assert(not changed);return math.floor(i/2)/16+(i%2)/16 end}
local indices={size=function()return 1242 end,get=function(self,i)assert(not changed and i<1239);return i end}
local sub={useIndices32=false,indices16=indices,indices32=indices,indicesCount=1239,primitive=4}
local empty={useIndices32=true,indices16=indices,indices32=indices,indicesCount=0,primitive=5}
local mesh={getAttributeData=function(self,semantic,start,amount)assert(completed and start==0 and amount==8193);if semantic==6 then return uv end;assert(semantic==(dim==2 and 13 or 0));return pos end,submeshes={size=function()return 2 end},getSubMesh=function(self,i)if i==0 then return sub else return empty end end}
local renderer={sharedMaterials={size=function()return 2 end}}
local comp={templateMesh=mesh,entity={name="face"},faceIds={size=function()return 5 end,get=function(self,i)return i end},makeupEntity={getComponent=function(self,kind)assert(kind=="Renderer");return renderer end}}
Amaz={VertexAttribType={TEXCOORD0=6,TEXCOORD7=13,POSITION=0},Algorithm={getAEAlgorithmResult=function()return {getFaceCount=function()return faces end}end},MessageCenter={sendMessage=function(id,nonce,n,text)assert(id==1431061505 and nonce==314 and n==#packets+1);packets[#packets+1]=text;changed=true end}}
'''
checks=0
for feature in ['AmazingFeature1','AmazingFeature3','AmazingFeature5','AmazingFeature6','AmazingFeature7']:
 base=BASE+('dim=3;' if feature=='AmazingFeature6' else '')
 stub='local EffectFaceMakeupSystemScript={onLateUpdate=function()completed=true end};'+mod.appendix(feature,314,'draw_geometry.lua.in')
 tail='local s=setmetatable({comps={comp}},{__index=EffectFaceMakeupSystemScript});s:onLateUpdate(nil,0);assert(#packets==54 and packets[54]:find("|END|-1|53",1,true));assert(packets[3]:find("0,0.0625",1,true));s:onLateUpdate(nil,0);assert(#packets==54)'
 run(base+stub+tail);checks+=1
 for change in ['uv.size=function()return 2 end','sub.indicesCount=1243','sub.indicesCount=-1','sub.useIndices32=1','sub.primitive=256','indices.get=function()return count end','mesh.getSubMesh=function()return nil end','renderer.sharedMaterials.size=function()return 65 end','comp.makeupEntity.getComponent=function()return nil end','count=8193','faces=11','comp.entity.name=""','pos.get=function()return 0/0 end']:
  run(base+change+';'+stub+';local s=setmetatable({comps={comp}},{__index=EffectFaceMakeupSystemScript});s:onLateUpdate(nil,0);assert(#packets==1 and packets[1]:find("|ERROR|-1|",1,true))');checks+=1
 run(base+'comp.makeupEntity=nil;'+stub+tail+';assert(packets[2]:find(",2,-1,",1,true))');checks+=1
 run(base+stub+';faces=0;local s=setmetatable({comps={}},{__index=EffectFaceMakeupSystemScript});s:onLateUpdate(nil,0);assert(#packets==2)');checks+=1
 if feature=='AmazingFeature1':
  run(base+stub+tail+';local f=assert(io.open('+json.dumps(str(pathlib.Path(sys.argv[3]).resolve()))+',"w"));for _,p in ipairs(packets)do f:write(p,"\\n")end;f:close()');checks+=1
with tempfile.TemporaryDirectory(prefix='ulike-draw-private-') as tmp:
 for style,archive in [('natural',sys.argv[1]),('purity',sys.argv[2])]:
  out=pathlib.Path(tmp)/style;manifest=mod.instrument(archive,style,out,314,observe_draw=True);assert manifest['draw_observer_enabled'];checks+=1
  for item in manifest['patched']:
   src=(out/item['path']).read_text();run('local e=(function()\n'+src+'\nend)();assert(e.EffectFaceMakeupSystemScript)');checks+=1
print(json.dumps({'checks':checks,'actual_supplied_scripts_loaded':6,'host_lua':'5.4 explicit SDK mocks','device_execution':False}))
