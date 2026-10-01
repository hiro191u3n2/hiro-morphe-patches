#!/usr/bin/env python3
"""Create a private, single-still copy of the caller's pinned effect.

No original effect source or assets are distributed with this patcher. The
appendices observe actual selected script setters and available 3D algorithm
data. They do not infer 2D native makeup vertices or read arbitrary GL state.
The caller must load this copy in a NEW isolated SDK instance for exactly one
owned still image; never apply it to the active preview or reuse a nonce.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import shutil
import zipfile

ROOT = Path(__file__).resolve().parent
ARCHIVES = {"natural": "5b50343dcedc7e9aadd218626e621ad683cd67372d225024f118b47f493546f0",
            "purity": "cd5da20fefe1f9bd594e4e0d055d6320392fa54fb5df9d34b8f159c791c5b117"}


def appendix(spec, nonce):
    feature = spec["path"].split("/")[0]
    cls = spec["class"]
    base = '''
-- BEGIN Hiro same-still observation appendix; this is not vendor code.
local __hiro_nonce = @NONCE@
local __hiro_feature = "@FEATURE@"
local __hiro_sequence = 0
local __hiro_done = false
local __hiro_active = false
local function __hiro_number(value)
    if type(value) ~= "number" or value ~= value or value == math.huge or value == -math.huge then
        error("nonfinite observation")
    end
    return string.format("%.17g", value)
end
local function __hiro_safe(value)
    if type(value) ~= "string" or #value > 128 or string.find(value, "[^%w_%-]") then
        error("invalid component identifier")
    end
    return value
end
local function __hiro_emit(kind, face, payload)
    if #payload > 7000 then error("observation packet too large") end
    __hiro_sequence = __hiro_sequence + 1
    Amaz.MessageCenter.sendMessage(1431065345, __hiro_nonce, __hiro_sequence,
        "S1|" .. __hiro_feature .. "|" .. kind .. "|" .. tostring(face) .. "|" .. payload)
end
local function __hiro_uniform(component, face, key, value)
    __hiro_emit("uniform", face, __hiro_safe(component) .. "," .. key .. "," .. __hiro_number(value))
end
local function __hiro_matrix(face, m)
    local numbers = {}
    for row=0,3 do
        local v = m:GetRow(row)
        table.insert(numbers,__hiro_number(v.x));table.insert(numbers,__hiro_number(v.y))
        table.insert(numbers,__hiro_number(v.z));table.insert(numbers,__hiro_number(v.w))
    end
    __hiro_emit("mvp",face,table.concat(numbers,","))
end
local __hiro_original_update = @CLASS@.onUpdate
'''.replace("@NONCE@", str(nonce)).replace("@FEATURE@", feature).replace("@CLASS@", cls)
    kind = spec["kind"]
    if kind == "uniform":
        base += '''
local __hiro_original_opacity = @CLASS@._setOpacity
function @CLASS@:_setOpacity(component,face,opacity)
    __hiro_original_opacity(self,component,face,opacity)
    if __hiro_active then
        __hiro_uniform(component.entity.name,face,"intensity",opacity)
    end
end
'''.replace("@CLASS@", cls)
        collect = '''
        -- These are post-setFaceUniform values. They are not a GL readback.
        -- A 2D native FaceMakeup vertex/segmentation observer is still required.
'''
    elif kind == "mesh3d":
        collect = '''
        local result = Amaz.Algorithm.getAEAlgorithmResult()
        local count = math.min(#self.faceEntity,result:getFaceCount())
        for slot=1,count do
            if self.faceEntity[slot].visible then
                local info = result:getFaceMeshInfo(slot-1)
                if info == nil then error("missing same-still face mesh") end
                local vertices = info.vertexes
                local total = vertices:size()
                if total ~= 1427 then error("unexpected pinned 3D mesh vertex count") end
                __hiro_matrix(slot-1,info.mvp)
                for first=0,total-1,64 do
                    local numbers = {};local amount=math.min(64,total-first)
                    for i=first,first+amount-1 do
                        local v=vertices:get(i)
                        table.insert(numbers,__hiro_number(v.x));table.insert(numbers,__hiro_number(v.y));table.insert(numbers,__hiro_number(v.z))
                    end
                    __hiro_emit("vertices",slot-1,tostring(first)..","..tostring(amount)..","..tostring(total)..";"..table.concat(numbers,","))
                end
                local material=self.faceComp[slot].sharedMaterials:get(0)
                __hiro_uniform(self.faceEntity[slot].name,slot-1,"intensity",material.properties:getFloat("intensity"))
            end
        end
'''
    elif kind == "natural_neural":
        collect = '__hiro_uniform("Entity",-1,"intensity",self.pass4Material.properties:getFloat("intensity"))\n'
    elif kind == "purity_neural":
        collect = '__hiro_uniform("LaughGan_main",-1,"intensity",self.MeshRenderer.material.properties:getFloat("intensity"))\n'
    elif kind == "lut":
        collect = '''
        for i=1,#self.filterComponent do
            local component=self.filterComponent[i]
            __hiro_uniform(component.entity.name,-1,"uniAlpha",component.sharedMaterials:get(0).properties:getFloat("uniAlpha"))
        end
'''
    else:
        raise ValueError("unsupported observer kind")
    base += '''
function @CLASS@:onUpdate(context,deltaTime)
    if __hiro_done then
        if __hiro_original_update then __hiro_original_update(self,context,deltaTime) end
        return
    end
    __hiro_active=true
    if __hiro_original_update then __hiro_original_update(self,context,deltaTime) end
    __hiro_active=false
    __hiro_done=true
    local ok = pcall(function()
@COLLECT@
    end)
    if ok then
        __hiro_emit("END",-1,tostring(__hiro_sequence))
    else
        __hiro_emit("ERROR",-1,"observation_failed")
    end
end
-- END Hiro same-still observation appendix.
'''.replace("@CLASS@", cls).replace("@COLLECT@", collect)
    return base


def instrument(archive, style, output, nonce):
    archive, output = Path(archive), Path(output).resolve()
    if style not in ARCHIVES or not 1 <= nonce <= 2147483647:
        raise ValueError("supported style and positive int nonce required")
    if output == ROOT or ROOT in output.parents or output.exists():
        raise ValueError("output must be a new PRIVATE directory outside distributable source")
    with archive.open("rb") as source:
        archive_snapshot = source.read(32*1024*1024+1)
    if len(archive_snapshot) > 32*1024*1024 or hashlib.sha256(archive_snapshot).hexdigest() != ARCHIVES[style]:
        raise ValueError("style archive SHA-256 mismatch")
    specs = json.loads((ROOT/"SCRIPT_OBSERVER_PINS.json").read_text())[style]
    copied, patched = 0, []
    output.mkdir(parents=True, exist_ok=False)
    try:
        with zipfile.ZipFile(io.BytesIO(archive_snapshot)) as z:
            names = z.namelist()
            if len(set(names)) != len(names): raise ValueError("duplicate archive members")
            for entry in z.infolist():
                if not entry.filename.startswith("materials/016/"): continue
                relative = Path(entry.filename[len("materials/016/"):])
                if not relative.parts or relative.is_absolute() or ".." in relative.parts or entry.is_dir():
                    raise ValueError("unexpected selected-style entry")
                if entry.file_size > 4*1024*1024: raise ValueError("entry budget")
                destination=output/relative;destination.parent.mkdir(parents=True,exist_ok=True)
                destination.write_bytes(z.read(entry));copied+=1
            for spec in specs:
                path=output/spec["path"];source=path.read_bytes()
                if hashlib.sha256(source).hexdigest()!=spec["sha256"]:raise ValueError("script SHA-256 mismatch")
                marker="exports."+spec["class"]+" = "+spec["class"]
                text=source.decode("utf-8")
                if text.count(marker)!=1:raise ValueError("script insertion boundary mismatch")
                result=text.replace(marker,appendix(spec,nonce)+"\n"+marker)
                path.write_text(result,encoding="utf-8")
                patched.append({"path":spec["path"],"source_sha256":spec["sha256"],"patched_sha256":hashlib.sha256(path.read_bytes()).hexdigest()})
        manifest={"schema":"ulike-private-same-still-observer-1","style":style,"nonce":nonce,
                  "message_id":0x554c5301,"per_feature_ordinal_starts_at":1,
                  "expected_features":[s["path"].split("/")[0] for s in specs],"files_copied":copied,"patched":patched,
                  "required_usage":"new isolated SDK instance, one owned still submission, never preview/reused nonce",
                  "actual_device_delivery_verified":False,"native_2d_makeup_geometry_observed":False,
                  "skin_mask_pixels_observed":False,"original_shader_execution_proven":False}
        (output/"hiro-observer-manifest.json").write_text(json.dumps(manifest,indent=2)+"\n")
        return manifest
    except BaseException:
        shutil.rmtree(output)
        raise


if __name__ == "__main__":
    p=argparse.ArgumentParser();p.add_argument("archive");p.add_argument("style",choices=ARCHIVES)
    p.add_argument("output");p.add_argument("--nonce",required=True,type=int)
    a=p.parse_args();print(json.dumps(instrument(a.archive,a.style,a.output,a.nonce),indent=2))
