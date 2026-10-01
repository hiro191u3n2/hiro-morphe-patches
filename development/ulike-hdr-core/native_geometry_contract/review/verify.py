#!/usr/bin/env python3
"""Independent bounded diagnostic observer/parser and pinned native control-flow review."""
import argparse,ctypes,ctypes.util,hashlib,importlib.util,json,os,struct,subprocess,tempfile,zipfile
from pathlib import Path
HERE=Path(__file__).resolve().parent
MODULE=HERE.parent
CORE=MODULE.parent
EFFECT='d40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(args):
    p=subprocess.run(list(map(str,args)),capture_output=True,text=True)
    if p.returncode:raise RuntimeError(p.stdout+'\n'+p.stderr)
    return p.stdout.strip()
def lua_run(code):
    lib=ctypes.CDLL(ctypes.util.find_library('lua5.4'))
    lib.luaL_newstate.restype=ctypes.c_void_p;lib.luaL_openlibs.argtypes=[ctypes.c_void_p]
    lib.luaL_loadstring.argtypes=[ctypes.c_void_p,ctypes.c_char_p]
    lib.lua_pcallk.argtypes=[ctypes.c_void_p,ctypes.c_int,ctypes.c_int,ctypes.c_int,ctypes.c_longlong,ctypes.c_void_p]
    lib.lua_tolstring.argtypes=[ctypes.c_void_p,ctypes.c_int,ctypes.c_void_p];lib.lua_tolstring.restype=ctypes.c_char_p;lib.lua_close.argtypes=[ctypes.c_void_p]
    state=lib.luaL_newstate();lib.luaL_openlibs(state)
    try:
        status=lib.luaL_loadstring(state,code.encode())
        if not status:status=lib.lua_pcallk(state,0,0,0,0,None)
        if status:raise AssertionError(lib.lua_tolstring(state,-1,None).decode())
    finally:lib.lua_close(state)
LUA_BASE='''
local packets={};local faceCount=2;local live={0,0};local scalarCount=16384;local called=0;local gets=0
EffectFaceMakeupSystemScript={onLateUpdate=function(self,sys,dt)called=called+1;live[1]=1.25;live[2]=-2.5 end}
local vector={size=function()return scalarCount end,get=function(self,i)gets=gets+1;return live[i%2+1] end}
local function newmesh()return {getAttributeData=function(self,semantic,start,count)
 assert(semantic==13 and start==0 and count==8193);return vector end}end
local shared=newmesh();local meshes={shared,shared,newmesh(),newmesh()}
local comps={};for i=1,4 do comps[i]={templateMesh=meshes[i],faceIds={size=function()return 2 end,get=function(self,i)return i end},entity={name="component"..i}}end
local result={getFaceCount=function()return faceCount end}
Amaz={VertexAttribType={TEXCOORD7=13},Algorithm={getAEAlgorithmResult=function()return result end},
 MessageCenter={sendMessage=function(id,nonce,seq,text)
 assert(id==1431062273 and nonce==17 and seq==#packets+1);assert(#text<=8192)
 packets[#packets+1]={id,nonce,seq,text};live[1]=999;live[2]=999
 end}}
'''
def lua_review(path):
    template=(MODULE/'late_geometry.lua.in').read_text()
    appendix=template.replace('@NONCE@','17').replace('@FEATURE@','AmazingFeature1').replace('@ATTRIBUTE@','TEXCOORD7').replace('@COMPONENTS@','2')
    footer='''
local instance={comps=comps};EffectFaceMakeupSystemScript.onLateUpdate(instance,nil,0)
assert(called==1 and #packets==518 and gets==65536)
assert(packets[1][4]=="G1|AmazingFeature1|BEGIN|-1|2,4")
assert(string.find(packets[2][4],",0,8192,0:1",1,true))
assert(string.find(packets[131][4],",0,8192,0:1",1,true))
assert(string.find(packets[260][4],",1,8192,0:1",1,true))
assert(string.find(packets[389][4],",2,8192,0:1",1,true))
local file=assert(io.open(@PATH@,"w"));for _,p in ipairs(packets)do file:write(p[1],"\\t",p[2],"\\t",p[3],"\\t",p[4],"\\n")end;file:close()
EffectFaceMakeupSystemScript.onLateUpdate(instance,nil,0);assert(called==2 and #packets==518)
'''.replace('@PATH@',json.dumps(str(path)))
    lua_run(LUA_BASE+appendix+footer)
    invalid=[
      'scalarCount=16386',
      'scalarCount=16383',
      'scalarCount=0',
      'comps[1].templateMesh=nil',
      'comps[1].faceIds.size=function()return 33 end',
      'comps[1].faceIds.get=function()return -1 end',
      'comps[1].entity.name=string.rep("n",129)',
      'for i=5,33 do comps[i]=comps[1] end',
      'for i=5,5 do comps[i]=comps[1] end',
      'vector.get=function()return 0/0 end',
      'vector.get=function(self,i)faceCount=3;return live[i%2+1] end',
    ]
    for fault in invalid:
        lua_run(LUA_BASE+fault+'\n'+appendix+'''\nEffectFaceMakeupSystemScript.onLateUpdate({comps=comps},nil,0)
assert(#packets==1 and packets[1][4]=="G1|AmazingFeature1|ERROR|-1|geometry_unavailable")''')
    three=LUA_BASE.replace('local live={0,0};','local live={0,0,0};').replace('local scalarCount=16384;','local scalarCount=6;').replace('live[2]=-2.5 end','live[2]=-2.5;live[3]=3.75 end').replace('live[i%2+1]','live[i%3+1]').replace('semantic==13','semantic==0').replace('VertexAttribType={TEXCOORD7=13}','VertexAttribType={POSITION=0}')
    third=template.replace('@NONCE@','17').replace('@FEATURE@','AmazingFeature6').replace('@ATTRIBUTE@','POSITION').replace('@COMPONENTS@','3')
    lua_run(three+'\ncomps={comps[1]}\n'+third+'''
EffectFaceMakeupSystemScript.onLateUpdate({comps=comps},nil,0)
assert(#packets==4 and packets[3][4]=="G1|AmazingFeature6|POS|0|0,2;1.25,-2.5,3.75,1.25,-2.5,3.75")
local f=assert(io.open(@PATH@,"w"));for _,p in ipairs(packets)do f:write(p[1],"\\t",p[2],"\\t",p[3],"\\t",p[4],"\\n")end;f:close()
'''.replace('@PATH@',json.dumps(str(path.with_name('three.tsv')))))
    return {'mock_cases':len(invalid)+2,'max_vertices_copied':32768,'max_packets':518,'width_three_eyelash_verified_in_mock':True,'native_execution':False}
def native_review(path):
    from elftools.elf.elffile import ELFFile
    from capstone import Cs,CS_ARCH_ARM64,CS_MODE_ARM
    assert sha(path)==EFFECT,'effect library differs'
    binary=path.read_bytes()
    with path.open('rb') as file:segments=list(ELFFile(file).iter_segments())
    def offset(address):
        for segment in segments:
            if segment['p_type']=='PT_LOAD' and segment['p_vaddr']<=address<segment['p_vaddr']+segment['p_filesz']:return address-segment['p_vaddr']+segment['p_offset']
        raise AssertionError('not mapped')
    md=Cs(CS_ARCH_ARM64,CS_MODE_ARM)
    def instruction(address,mnemonic,operands):
        items=list(md.disasm(binary[offset(address):offset(address)+4],address));assert len(items)==1
        assert (items[0].mnemonic,items[0].op_str)==(mnemonic,operands),hex(address)
    # Independent check of width dispatch: one-based width -> byte table -> scaled PC target.
    table=binary[offset(0x13278bc):offset(0x13278bc)+4]
    assert list(table)==[0,97,26,60]
    assert 0xa436d0+table[1]*4==0xa43854,'width two dispatch'
    assert 0xa436d0+table[2]*4==0xa43738,'width three dispatch'
    selected=[(0xa436ac,'sub','w8, w0, #1'),(0xa436c4,'ldrb','w11, [x9, x8]'),(0xa436c8,'add','x10, x10, x11, lsl #2'),
      (0xa43854,'bl','#0xe4c704'),(0xa43858,'bl','#0xe7a1e8'),(0xe7a1e8,'sub','w8, w0, w19'),(0xe7a1ec,'cmp','w8, w20'),(0xe7a1f0,'mov','w8, w20'),
      (0xa4385c,'b.gt','#0xa43868'),(0xa43864,'sub','w8, w0, w19'),(0xa43868,'lsl','w1, w8, #1'),
      (0xa438a0,'cmp','x9, x12'),(0xa438a4,'ccmp','w14, w0, #0, lt'),(0xa438a8,'b.ge','#0xa438dc'),
      (0xa438bc,'add','x15, x15, x8'),(0xa438c0,'add','x8, x8, #8'),(0xa438c4,'str','w16, [x15]'),(0xa438c8,'ldr','w16, [x14, #4]'),(0xa438d0,'str','w16, [x15, #4]'),
      (0xa43738,'bl','#0xe4c704'),(0xa4373c,'bl','#0xe7a1e8'),(0xa43740,'b.gt','#0xa4374c'),(0xa43748,'sub','w8, w0, w19'),(0xa4374c,'add','w1, w8, w8, lsl #1'),
      (0xa43798,'str','w16, [x15, w17, uxtw #2]'),(0xa437a4,'str','w16, [x15, w17, uxtw #2]'),(0xa437a8,'ldr','w16, [x14, #8]'),(0xa437b0,'str','w16, [x15, w13, uxtw #2]'),(0xa437b8,'add','w13, w13, #3'),
      (0xac8ca8,'cmp','x9, x26'),(0xac8cac,'b.ls','#0xac8d60'),(0xac8d0c,'bl','#0xe72590'),(0xac8d50,'bl','#0xe72590'),
      (0xac8d54,'add','x26, x26, #1'),(0xac8d5c,'b','#0xac8c9c'),(0xac8d64,'cbnz','w8, #0xac8d78'),(0xac8d6c,'mov','w1, #0x44e'),(0xac8d74,'bl','#0xa766a0'),
      (0xb14a48,'add','x20, x20, #0xf6c'),(0xb14b10,'bl','#0xb12be4')]
    for item in selected:instruction(*item)
    name=offset(0x126bf6c);assert binary[name:name+13]==b'onLateUpdate\0'
    # Positive count caller uses start 0; clamped output cannot exceed 8193 vertices.
    for total in (0,1,8192,8193,8194,32768,2147483647):
        remaining=total;count=8193;allocated=(count if remaining>count else remaining)*2
        assert 0<=allocated<=16386
    return {'effect_sha256':EFFECT,'independent_instruction_checks':len(selected),'width2_table_target':'0xa43854','width3_table_target':'0xa43738',
            'late_update_timing':'Scene system loop precedes conditional 0x44e dispatch, not proof of a picture callback or same-still attribution',
            'count_scope':'only nonnegative vertexCount with observer start=0 and positive count=8193',
            'native_addresses_invoked':False}
def asset_review(natural,purity):
    pins=json.loads((CORE/'android_style_binding/ASSET_PINS.json').read_text())['assets']
    known={(p['style'],p['path']):p['sha256'] for p in pins}
    summary=[]
    for style,archive,features,expected in [('natural',natural,[1],'5b50343dcedc7e9aadd218626e621ad683cd67372d225024f118b47f493546f0'),('purity',purity,[1,3,5,6,7],'cd5da20fefe1f9bd594e4e0d055d6320392fa54fb5df9d34b8f159c791c5b117')]:
        assert sha(archive)==expected
        with zipfile.ZipFile(archive) as z:
            for feature in features:
                prefix=f'materials/016/AmazingFeature{feature}/'
                shaders=[n for n in z.namelist() if n.startswith(prefix) and n.endswith('.xshader')]
                assert shaders
                for name in shaders:
                    semantic=0 if style=='purity' and feature==6 else 13
                    data=z.read(name);pattern=b'\x0b\x00attPosition'+struct.pack('<III',0x90580a2b,1,semantic)
                    assert pattern in data,'selected authored shader position semantic mismatch'
                    summary.append({'style':style,'path':name.removeprefix('materials/016/'),'sha256':hashlib.sha256(data).hexdigest(),'position_semantic':semantic})
    return summary
def main():
    p=argparse.ArgumentParser()
    for n in ('effect','natural-zip','purity-zip','jdk-bin','android-jar'):p.add_argument('--'+n,type=Path,required=True)
    p.add_argument('--report',type=Path,default=MODULE/'INDEPENDENT_REVIEW.json');a=p.parse_args()
    files=[MODULE/'late_geometry.lua.in',MODULE/'instrument_geometry.py',MODULE/'verify_native.py',MODULE/'src/com/hiro/ulike/geometry/NativeMeshObservation.java',HERE/'ParserReview.java',Path(__file__).resolve(),CORE/'android_style_binding/ASSET_PINS.json']
    before={str(f.relative_to(CORE)):sha(f) for f in files}
    native=native_review(a.effect);assets=asset_review(a.natural_zip,a.purity_zip)
    with tempfile.TemporaryDirectory(prefix='ulike-native-geometry-review-') as work:
        work=Path(work);packet=work/'synthetic_packets.tsv';lua=lua_review(packet);classes=work/'classes';classes.mkdir()
        run([a.jdk_bin/'javac','--release','8','-cp',a.android_jar,'-d',classes,MODULE/'src/com/hiro/ulike/geometry/NativeMeshObservation.java',HERE/'ParserReview.java'])
        parser=json.loads(run([a.jdk_bin/'java','-Xmx128m','-cp',classes,'com.hiro.ulike.geometry.ParserReview',packet]))
    assert before=={str(f.relative_to(CORE)):sha(f) for f in files},'source changed during review'
    report={'status':'PASS_INDEPENDENT_NATIVE_GEOMETRY_DIAGNOSTIC_REVIEW','native_static':native,'selected_authored_shader_bindings':assets,'lua_observer_mocks':lua,'parser':parser,
      'reviewed_file_sha256':before,'blocking_findings':[],
      'scope':['Lua userdata alias ordinal is object-key identity only, never a native pointer or verified shared GPU buffer.','Declared eligible face IDs are not final compacted active face/submesh routing.','Template mesh samples do not prove final render projection, UV, draw order, source-still attribution, or no later overwrite.','Normal SDK script appendix remains diagnostic-only; no native address invoked or production geometry enabled.'],
      'android_device_execution':False,'actual_native_callback_delivery':False,'per_face_routing_verified':False,'production_geometry_verified':False}
    a.report.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'status':report['status'],'native_instruction_checks':native['independent_instruction_checks'],'lua':lua,'parser':parser}))
if __name__=='__main__':main()
