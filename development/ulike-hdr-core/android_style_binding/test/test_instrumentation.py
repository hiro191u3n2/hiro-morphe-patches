#!/usr/bin/env python3
"""Exercises our appendices and supplied original script updates using liblua5.4.
This validates syntax/control/data flow under explicit SDK mocks, not device delivery.
Vendor scripts are read privately from the user's archive and never embedded here.
"""
import ctypes,ctypes.util,hashlib,importlib.util,json,pathlib,tempfile,sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('instrument',ROOT/'instrument_style.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
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
  if result:
   pathlib.Path("/tmp/binding-lua-failure.lua").write_text(code)
   raise AssertionError(lua.lua_tolstring(s,-1,None).decode())
 finally:lua.lua_close(s)
BASE='''
local packets={};local writes={};local faces=1;local gender=2;local age=25
local props={value=0.625,getFloat=function(self,key)return self.value end,setFloat=function(self,key,v)self.value=v end}
local material={properties=props,setFloat=function(self,k,v)props:setFloat(k,v)end}
local materials={get=function(self,i)return material end,size=function()return 1 end}
local renderer={sharedMaterials=materials,material=material}
local entity={name="ObservedComponent",getComponent=function(self,k)return renderer end}
local comp={entity=entity,femaleOpacity=0.75,maleOpacity=0.25,type=0,setFaceUniform=function(self,k,f,v)table.insert(writes,{k,f,v})end}
local result={getFaceCount=function()return faces end,getFaceAttributeInfo=function()return {age=age,gender=gender}end,getFaceExtraInfo=function()return {}end}
Amaz={EffectFaceMakeupType={FACEU_PUPIL=8},MessageCenter={sendMessage=function(id,nonce,ordinal,text)
 assert(id==1431065345 and nonce==314 and ordinal==#packets+1);assert(#text<=8192);table.insert(packets,text)
 end},Algorithm={getAEAlgorithmResult=function()return result end,setAlgorithmParamInt=function()end},
 BuiltinObject={getAgeMakeupState=function()return true end,getMaleMakeupState=function()return false end}}
'''
checks=0
with tempfile.TemporaryDirectory(prefix='ulike-style-instrument-test-') as temp:
 for style,archive in [('natural',sys.argv[1]),('purity',sys.argv[2])]:
  out=pathlib.Path(temp)/style;manifest=module.instrument(archive,style,out,314)
  specs=json.loads((ROOT/'SCRIPT_OBSERVER_PINS.json').read_text())[style]
  assert manifest['files_copied']==(45 if style=='natural' else 143);checks+=1
  for item in specs:
   source=(out/item['path']).read_text();klass=item['class'];kind=item['kind'];feature=item['path'].split('/')[0]
   run('local e=(function()\n'+source+'\nend)();assert(e.'+klass+' ~= nil)');checks+=1
   if kind=='uniform':
    for gen,years in [(2,25),(1,25),(2,6),(0,25)]:
     suffix=f'''gender={gen};age={years};local e=(function()\n{source}\nend)();local s=e.{klass}.new();s.comps={{comp}};s.intensity={{0.8}}
     s:onUpdate(nil,0);assert(string.find(packets[#packets],"|END|-1|",1,true));local n=#packets
     s:onUpdate(nil,0);assert(#packets==n)
     if #writes>0 then local expected="ObservedComponent,intensity,"..string.format("%.17g",writes[1][3]);assert(string.sub(packets[1],-#expected)==expected) else assert(n==1) end
     '''
     run(BASE+suffix);checks+=1
    run(BASE+'faces=0;local e=(function()\n'+source+'\nend)();local s=e.'+klass+'.new();s.comps={comp};s.intensity={0.8};s:onUpdate(nil,0);assert(#packets==1 and string.find(packets[1],"|END|-1|0",1,true))');checks+=1
   elif kind=='lut':
    suffix='local e=(function()\n'+source+'\nend)();local s=e.'+klass+'.new();s.filterComponent={{entity=entity,sharedMaterials=materials}};s:onUpdate(nil,0);assert(#packets==2);assert(string.find(packets[1],"|uniform|-1|ObservedComponent,uniAlpha,0.625",1,true));s:onUpdate(nil,0);assert(#packets==2)'
    run(BASE+suffix);checks+=1
   elif kind=='natural_neural':
    run(BASE+'local e=(function()\n'+source+'\nend)();local s=e.'+klass+'.new();s.pass4Material=material;s.pass4table={get=function()return 0.3125 end};s:onUpdate(nil,0);assert(#packets==2);assert(string.find(packets[1],"|uniform|-1|Entity,intensity,0.3125",1,true))');checks+=1
   elif kind=='mesh3d':
    suffix='''
    local matrix={GetRow=function(self,i)return {x=i*4+1,y=i*4+2,z=i*4+3,w=i*4+4}end}
    local vertices={size=function()return 1427 end,get=function(self,i)return {x=i/16,y=i/32,z=i/64}end}
    result.getFaceMeshInfo=function()return {mvp=matrix,modelMatrix=matrix,vertexes=vertices,normals=vertices}end
    local mesh={setVertexArray=function(self,x)assert(x==vertices)end,setNormalArray=function()end}
    local facecomp={mesh=mesh,sharedMaterials=materials,props={setMatrix=function()end}}
    local e=(function()\n'''+source+'''\nend)();local s=e.Face3DSystem.new();s.faceEntity={{name="face0"}};s.faceComp={facecomp};s:onUpdate(nil,0)
    assert(#packets==26);assert(string.find(packets[1],"|mvp|0|1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16",1,true));assert(string.find(packets[24],"|vertices|0|1408,19,1427;",1,true));assert(string.find(packets[25],"|uniform|0|face0,intensity,0.625",1,true));assert(string.find(packets[26],"|END|-1|25",1,true));s:onUpdate(nil,0);assert(#packets==26)
    '''
    run(BASE+suffix);checks+=1
   else:
    # The original neural shader update needs native GAN mesh. Exercise the
    # exact observer appendix separately without pretending a GAN SDK mock.
    fixture='local '+klass+'={onUpdate=function(self)self.updated=true end};'+module.appendix(item,314)
    run(BASE+fixture+'local s=setmetatable({MeshRenderer=renderer},{__index='+klass+'});s:onUpdate(nil,0);assert(s.updated);assert(#packets==2);assert(string.find(packets[1],"|uniform|-1|LaughGan_main,intensity,0.625",1,true))');checks+=1
 print(json.dumps({'host_lua':'5.4 mock SDK','checks':checks,'actual_scripts_syntax':12,'actual_device_delivery':False}))
