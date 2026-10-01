#!/usr/bin/env python3
"""Independent D1 snapshot/parser and exact authored-script scope review. No native execution."""
import argparse,ctypes,ctypes.util,hashlib,importlib.util,json,os,subprocess,tempfile,zipfile
from pathlib import Path
HERE=Path(__file__).resolve().parent
MODULE=HERE.parent
CORE=MODULE.parent
EFFECT='d40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e'
ARCHIVES={'natural':'5b50343dcedc7e9aadd218626e621ad683cd67372d225024f118b47f493546f0','purity':'cd5da20fefe1f9bd594e4e0d055d6320392fa54fb5df9d34b8f159c791c5b117'}
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def run(args):
    out=subprocess.run(list(map(str,args)),capture_output=True,text=True)
    if out.returncode:raise RuntimeError(out.stdout+'\n'+out.stderr)
    return out.stdout.strip()
def lua_run(code):
    lib=ctypes.CDLL(ctypes.util.find_library('lua5.4'));lib.luaL_newstate.restype=ctypes.c_void_p
    lib.luaL_openlibs.argtypes=[ctypes.c_void_p];lib.luaL_loadstring.argtypes=[ctypes.c_void_p,ctypes.c_char_p]
    lib.lua_pcallk.argtypes=[ctypes.c_void_p,ctypes.c_int,ctypes.c_int,ctypes.c_int,ctypes.c_longlong,ctypes.c_void_p]
    lib.lua_tolstring.argtypes=[ctypes.c_void_p,ctypes.c_int,ctypes.c_void_p];lib.lua_tolstring.restype=ctypes.c_char_p;lib.lua_close.argtypes=[ctypes.c_void_p]
    state=lib.luaL_newstate();lib.luaL_openlibs(state)
    try:
        status=lib.luaL_loadstring(state,code.encode())
        if not status:status=lib.lua_pcallk(state,0,0,0,0,None)
        if status:raise AssertionError(lib.lua_tolstring(state,-1,None).decode())
    finally:lib.lua_close(state)
LUA='''
local vertices=8192;local drawn=65536;local stored=65536;local components=4;local dim=2
local completed=false;local emitted=false;local calls=0;local packets={};local faceCount=2
local EffectFaceMakeupSystemScript={onLateUpdate=function()completed=true;calls=calls+1 end}
local result={getFaceCount=function()return faceCount end}
local pos={size=function()return vertices*dim end,get=function(self,i)assert(completed and not emitted);local v=math.floor(i/dim);if i%dim==0 then return v/16 elseif i%dim==1 then return -v/32 else return v/64 end end}
local uv={size=function()return vertices*2 end,get=function(self,i)assert(not emitted);if i%2==0 then return math.floor(i/2)%2 else return .5 end end}
local indices={size=function()return stored end,get=function(self,i)assert(not emitted and i<drawn);return i%vertices end}
local sub={useIndices32=true,indices16=indices,indices32=indices,indicesCount=drawn,primitive=4}
local mesh={getAttributeData=function(self,semantic,start,count)assert(completed and start==0 and count==8193);if semantic==6 then return uv end;assert(semantic==(dim==3 and 0 or 13));return pos end,submeshes={size=function()return 1 end},getSubMesh=function(self,i)assert(i==0);return sub end}
local renderer={sharedMaterials={size=function()return 2 end}}
local comp={templateMesh=mesh,entity={name="face"},faceIds={size=function()return 2 end,get=function(self,i)return 7-4*i end},makeupEntity={getComponent=function(self,name)assert(name=="Renderer");return renderer end}}
local comps={comp,comp,comp,comp}
Amaz={VertexAttribType={POSITION=0,TEXCOORD0=6,TEXCOORD7=13},Algorithm={getAEAlgorithmResult=function()return result end},MessageCenter={sendMessage=function(id,nonce,seq,text)
 assert(id==1431061505 and nonce==901 and seq==#packets+1);packets[#packets+1]=text;emitted=true
end}}
'''
def appendix(feature):
    return (MODULE/'draw_geometry.lua.in').read_text().replace('@NONCE@','901').replace('@FEATURE@',feature).replace('@ATTRIBUTE@','POSITION' if feature=='AmazingFeature6' else 'TEXCOORD7').replace('@COMPONENTS@','3' if feature=='AmazingFeature6' else '2')
def lua_review(packet):
    footer='''
local instance={comps=comps};EffectFaceMakeupSystemScript.onLateUpdate(instance,nil,0)
assert(calls==1 and #packets==3082 and packets[#packets]:find("|END|-1|3081",1,true))
local f=assert(io.open(@FILE@,"w"));for _,p in ipairs(packets)do f:write(p,"\\n")end;f:close()
EffectFaceMakeupSystemScript.onLateUpdate(instance,nil,0);assert(calls==2 and #packets==3082)
'''.replace('@FILE@',json.dumps(str(packet)))
    lua_run(LUA+appendix('AmazingFeature1')+footer)
    invalid=['vertices=8193','drawn=65537;sub.indicesCount=drawn','sub.indicesCount=stored+1','sub.useIndices32=0',
             'mesh.getSubMesh=function()return nil end','comp.makeupEntity.getComponent=function()return nil end',
             'renderer.sharedMaterials.size=function()return 65 end','uv.size=function()return vertices*2-1 end',
             'pos.get=function()return math.huge end','indices.get=function()return vertices end',
             'indices.get=function()return -.5 end','faceCount=11',
             'pos.get=function()faceCount=3;return 0 end','comps[5]=comp']
    for fault in invalid:
        lua_run(LUA+fault+'\n'+appendix('AmazingFeature1')+'''
EffectFaceMakeupSystemScript.onLateUpdate({comps=comps},nil,0)
assert(#packets==1 and packets[1]=="D1|AmazingFeature1|ERROR|-1|draw_state_unavailable")''')
    for feature in ['AmazingFeature1','AmazingFeature3','AmazingFeature5','AmazingFeature6','AmazingFeature7']:
        setup='vertices=3;drawn=3;stored=65536;sub.indicesCount=drawn;comps={comp};dim='+('3' if feature=='AmazingFeature6' else '2')+';'
        lua_run(LUA+setup+appendix(feature)+'''
EffectFaceMakeupSystemScript.onLateUpdate({comps=comps},nil,0)
assert(#packets==6 and packets[4]:find("0,1,4,3,65536",1,true));assert(packets[5]:find(";0,1,2",1,true))
''')
    # Missing generated entity is explicitly unknown. The authored entity must never substitute for it.
    lua_run(LUA+'vertices=3;drawn=3;sub.indicesCount=3;comps={comp};comp.makeupEntity=nil;comp.entity.getComponent=function()error("wrong authored renderer")end;'+appendix('AmazingFeature1')+'''
EffectFaceMakeupSystemScript.onLateUpdate({comps=comps},nil,0)
assert(#packets==6 and packets[2]:find(",3,1,-1,",1,true))
''')
    return {'mock_cases':21,'max_vertices':32768,'max_requested_indices':262144,'max_packets':3082,'native_execution':False,
            'checks':['all scalar snapshotting precedes first external message','both attribute widths','index prefix excludes unused storage','generated renderer absence never substitutes authored renderer','previous late hook runs first and on subsequent calls']}
def native_review(path):
    from elftools.elf.elffile import ELFFile
    from capstone import Cs,CS_ARCH_ARM64,CS_MODE_ARM
    assert sha(path)==EFFECT
    data=path.read_bytes()
    with path.open('rb') as f:segments=list(ELFFile(f).iter_segments())
    def off(address):
        for s in segments:
            if s['p_type']=='PT_LOAD' and s['p_vaddr']<=address<s['p_vaddr']+s['p_filesz']:return address-s['p_vaddr']+s['p_offset']
        raise AssertionError('unmapped static address')
    md=Cs(CS_ARCH_ARM64,CS_MODE_ARM)
    # Independently checks both branches of indicesCount and the generated renderer call chain.
    expected=[(0xa43404,'tbnz','w1, #0x1f, #0xa43440'),(0xa4340c,'cbz','x8, #0xa43418'),
        (0xa43418,'mov','w9, wzr'),(0xa4341c,'cmp','w9, w1'),(0xa43420,'b.le','#0xa43440'),
        (0xa43424,'ldr','x8, [x8, #0x10]'),(0xa43428,'add','x9, x8, w1, sxtw #4'),
        (0xa3fe30,'ldrb','w0, [x0, #0x14b]'),(0xa3fe34,'ret',''),
        (0xa400e8,'tbz','w0, #0, #0xa400f8'),(0xa400ec,'ldr','x8, [x19, #0x78]'),(0xa400f0,'add','x8, x8, #0xa0'),
        (0xa400f4,'b','#0xa400fc'),(0xa400f8,'add','x8, x19, #0x150'),(0xa400fc,'ldr','w0, [x8]'),
        (0x861b68,'mov','x1, x0'),(0x861b6c,'mov','x0, x20'),(0x861b70,'bl','#0x857e70'),
        (0x861b74,'ldr','x0, [x19, #0xf0]'),(0x861b78,'cbz','x0, #0x861ae0'),
        (0x861b7c,'ldr','x8, [x0]'),(0x861b80,'ldr','x1, [x19, #0x90]'),
        (0x861b84,'ldr','x8, [x8, #0xe0]'),(0x861b88,'blr','x8')]
    for address,mnemonic,operands in expected:
        instructions=list(md.disasm(data[off(address):off(address)+4],address));assert len(instructions)==1
        assert (instructions[0].mnemonic,instructions[0].op_str)==(mnemonic,operands),hex(address)
    strings={0x125bb01:'useIndices32',0x1263fa3:'sharedMaterials',0x125bd47:'getSubMesh',0x125bb2e:'indicesCount'}
    for address,value in strings.items():assert data[off(address):off(address)+len(value)+1]==value.encode()+b'\0'
    return {'effect_sha256':EFFECT,'independent_instruction_checks':len(expected),'independent_string_checks':len(strings),
            'scope':'Exact bounded accessor/getter and generated-renderer static slices, not execution, frame attribution, or final GPU state.','native_addresses_invoked':False}
def authored_review(natural,purity,work):
    spec=importlib.util.spec_from_file_location('independent_d1_installer',MODULE/'instrument_geometry.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    pins=json.loads((CORE/'android_style_binding/SCRIPT_OBSERVER_PINS.json').read_text())
    checked=[]
    for style,archive in [('natural',natural),('purity',purity)]:
        assert sha(archive)==ARCHIVES[style]
        out=work/style;manifest=module.instrument(archive,style,out,901,observe_draw=True)
        expected=[p for p in pins[style] if p['kind']=='uniform']
        assert manifest['expected_features']==[p['path'].split('/')[0] for p in expected]
        assert manifest['draw_observer_enabled'] and not manifest['production_geometry_verified'] and not manifest['per_face_ranges_known']
        with zipfile.ZipFile(archive) as z:
            for item in pins[style]:
                original=z.read('materials/016/'+item['path']);assert hashlib.sha256(original).hexdigest()==item['sha256']
                actual=(out/item['path']).read_text();has_d1='local __hd_nonce=901' in actual
                assert has_d1==(item['kind']=='uniform')
                if item['kind']=='uniform':
                    feature=item['path'].split('/')[0]
                    assert actual.count(appendix(feature))==1
                    assert actual.index('local __hd_previous=')<actual.index('exports.EffectFaceMakeupSystemScript = EffectFaceMakeupSystemScript')
                    assert ('getAttributeData(Amaz.VertexAttribType.POSITION,0,8193)' in actual)==(feature=='AmazingFeature6')
                    lua_run('local result=(function()\n'+actual+'\nend)();assert(type(result.EffectFaceMakeupSystemScript.onLateUpdate)=="function")')
                    checked.append({'style':style,'feature':feature,'original_script_sha256':item['sha256'],'patched_script_sha256':sha(out/item['path'])})
        baseout=work/(style+'-default');default=module.instrument(archive,style,baseout,901)
        assert not default['draw_observer_enabled']
        for item in expected:assert 'local __hd_nonce=' not in (baseout/item['path']).read_text()
    assert len(checked)==6
    return {'loaded_actual_private_scripts':6,'selected_scripts':checked,'unexpected_d1_targets':0,'default_draw_observer_disabled':True,
            'script_load_only':True,'native_script_execution':False}
def main():
    p=argparse.ArgumentParser()
    for name in ('jdk-bin','android-jar','effect','natural-zip','purity-zip'):p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--report',type=Path,default=HERE/'INDEPENDENT_REVIEW.json');a=p.parse_args()
    files=[MODULE/'src/com/hiro/ulike/geometry/NativeDrawObservation.java',MODULE/'src/com/hiro/ulike/geometry/NativeMeshObservation.java',
           MODULE/'draw_geometry.lua.in',MODULE/'late_geometry.lua.in',MODULE/'instrument_geometry.py',MODULE/'verify_native.py',
           CORE/'android_style_binding/instrument_style.py',CORE/'android_style_binding/SCRIPT_OBSERVER_PINS.json',HERE/'DrawReview.java',Path(__file__).resolve()]
    before={str(f.relative_to(CORE)):sha(f) for f in files};native=native_review(a.effect)
    with tempfile.TemporaryDirectory(prefix='ulike-independent-d1-') as temporary:
        work=Path(temporary);packet=work/'packets.txt';lua=lua_review(packet);assets=authored_review(a.natural_zip,a.purity_zip,work)
        classes=work/'classes';classes.mkdir()
        run([a.jdk_bin/'javac','--release','8','-cp',a.android_jar,'-d',classes,*files[:2],HERE/'DrawReview.java'])
        parser=json.loads(run([a.jdk_bin/'java','-Xmx64m','-cp',classes,'com.hiro.ulike.geometry.DrawReview',packet]))
    assert before=={str(f.relative_to(CORE)):sha(f) for f in files},'reviewed source changed'
    report={'status':'PASS_INDEPENDENT_D1_TOPOLOGY_DIAGNOSTIC_REVIEW','parser':parser,'lua_mock':lua,'actual_pinned_script_scope':assets,
            'native_static':native,'reviewed_file_sha256':before,'blocking_findings':[],
            'limitations':['D1 captures CPU vertex/UV/topology snapshots, not final GPU draw state or matrices.','Declared face IDs and generated-renderer material counts do not establish active face routing or submesh/material mapping.','G1/D1 equality tests confirm snapshots match; they do not establish source-frame attribution or callback timing.','Purity2 legacy attOpacity is not observed by D1.','Real phone/native callback execution, production binding and complete beauty engine remain unverified.'],
            'android_device_execution':False,'actual_native_callbacks':False,'production_geometry_verified':False}
    a.report.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'status':report['status'],'parser':parser,'lua_cases':lua['mock_cases'],'actual_scripts':assets['loaded_actual_private_scripts']}))
if __name__=='__main__':main()
