#!/usr/bin/env python3
"""Permit only source-identity metadata changes in the pinned .94 GPU ELF.

The compute fingerprint deliberately covers Java routes as well as native code.
It must change when those routes change; retaining it would reuse stale proofs.
ELF section boundaries locate the two metadata fields. Replacing only those fields
with their fixed .94 values must restore the entire exact published native file.
"""
from pathlib import Path
import hashlib, importlib.util, json, re, struct
ROOT=Path(__file__).resolve().parent
BASE_SHA256='78371333c30d5f4d17ec1def57f10524be9d24cdcc18067529c62a56e6a1f42e'
BASE_BYTES=163272
BASE_FINGERPRINT='72782cde2dd6e3b298eee667a4c8bfe2badd47e91ada79005a25d4f0e836512f'
BASE_BUILD_ID='e57027619ac0ce0468efa2a877c60f7e949f7619'

def need(value,message):
    if not value:raise AssertionError(message)
def sha(raw):return hashlib.sha256(raw).hexdigest()

def sections(raw):
    need(len(raw)>=64 and raw[:7]==b'\x7fELF\x02\x01\x01','Expected ELF64 little-endian version1')
    need(struct.unpack_from('<H',raw,18)[0]==183,'Expected AArch64 ELF')
    offset=struct.unpack_from('<Q',raw,40)[0]
    size,count,names=struct.unpack_from('<HHH',raw,58)
    need(size==64 and 0<count<4096 and 0<names<count and offset>=64 and offset+size*count<=len(raw),'Invalid section table')
    rows=[struct.unpack_from('<IIQQQQIIQQ',raw,offset+i*size) for i in range(count)]
    names_row=rows[names];need(names_row[1]==3,'Section name table must be STRTAB')
    start,length=names_row[4:6];need(start+length<=len(raw),'Section name table bounds')
    labels=raw[start:start+length];result={}
    for row in rows:
        index=row[0];need(index<len(labels),'Section name offset')
        end=labels.find(b'\0',index);need(end>=index,'Terminated section name')
        try:name=labels[index:end].decode('ascii')
        except UnicodeDecodeError:raise AssertionError('Non-ASCII section name')
        need(name not in result,'Duplicate section name')
        need(row[1]==8 or row[4]+row[5]<=len(raw),'Section exceeds file')
        result[name]=row
    return result

def verify(raw,fingerprint,baseline=None):
    need(isinstance(raw,bytes) and len(raw)==BASE_BYTES,'Pinned GPU ELF size differs')
    need(re.fullmatch('[a-f0-9]{64}',fingerprint or '') is not None,'Invalid expected compute fingerprint')
    if baseline is not None:need(len(baseline)==BASE_BYTES and sha(baseline)==BASE_SHA256,'Wrong native baseline bytes')
    table=sections(raw)
    need('.rodata' in table and '.note.gnu.build-id' in table and '.text' in table,'Required native sections absent')
    data=table['.rodata'];note=table['.note.gnu.build-id'];code=table['.text']
    need(data[1]==1 and data[2]&2 and not data[2]&5,'Fingerprint must be read-only allocated PROGBITS')
    need(code[1]==1 and code[2]&4,'Executable text section absent')
    begin,length=data[4:6];region=raw[begin:begin+length]
    matches=list(re.finditer(rb'(?<![a-f0-9])[a-f0-9]{64}(?![a-f0-9])',region))
    need(len(matches)==1 and matches[0].group()==fingerprint.encode(),'Unique source-derived fingerprint in .rodata required')
    pos=begin+matches[0].start();need(pos>begin and raw[pos-1:pos]==b'\0' and raw[pos+64:pos+65]==b'\0','Fingerprint must be an entire NUL-terminated literal')
    need(raw.count(fingerprint.encode())==1,'Fingerprint duplicated outside .rodata')
    start,size=note[4:6]
    need(note[1]==7 and note[2]==2 and size==36 and note[8]==4,'Pinned GNU build-id NOTE shape')
    need(struct.unpack_from('<III',raw,start)==(4,20,3) and raw[start+12:start+16]==b'GNU\0','GNU SHA1 note owner/type/length')
    id_start=start+16;need(id_start+20==start+size,'GNU build-id descriptor bounds')
    need(pos+64<=start or id_start+20<=pos,'Metadata masks overlap')
    restored=bytearray(raw);restored[pos:pos+64]=BASE_FINGERPRINT.encode();restored[id_start:id_start+20]=bytes.fromhex(BASE_BUILD_ID)
    need(sha(restored)==BASE_SHA256,'GPU native changed outside exact compute fingerprint and GNU build-id descriptor')
    if baseline is not None:need(restored==baseline,'GPU native inverse differs from published baseline')
    return {'schema':'ulike1995-native-identity-delta-v1','status':'passed','baseline_sha256':BASE_SHA256,
        'baseline_bytes':BASE_BYTES,'baseline_compute_fingerprint':BASE_FINGERPRINT,'compute_fingerprint':fingerprint,
        'baseline_build_id':BASE_BUILD_ID,'build_id':raw[id_start:id_start+20].hex(),
        'allowed_metadata_ranges':[{'section':'.note.gnu.build-id','offset':id_start,'bytes':20},
                                   {'section':'.rodata','offset':pos,'bytes':64}],
        'changed_bytes':sum(x!=y for x,y in zip(raw,restored)),
        'whole_file_inverse_sha256':sha(restored),'all_other_native_bytes_identical':True,
        'machine_code_and_shader_literals_identical':True,'section_headers_byte_identical':True}

def current_fingerprint(source=ROOT):
    source=Path(source);path=source/'native1960/build_native1960.py'
    spec=importlib.util.spec_from_file_location('native_identity_compute1995',path)
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    return module.compute_fingerprint1990(source/'native1960')[0]

def verify_native_sources(source=ROOT):
    source=Path(source);pins=json.loads((source/'native_identity1995_baseline.json').read_text())
    need(pins.get('schema')=='ulike1995-native-source-baseline-v1','Native source baseline schema')
    manifest=source/'native1960/compute_dependencies1990.json'
    need(sha(manifest.read_bytes())==pins['compute_manifest_sha256'],'Native dependency classification changed')
    entries=json.loads(manifest.read_text())['native_dependencies']
    need(set(entries)==set(pins['native_source_sha256']),'Native baseline source inventory differs')
    for name,digest in pins['native_source_sha256'].items():
        need(sha((source/name).read_bytes())==digest,'Native/shader/CPU source changed from .94: '+name)
    return {'native_source_sha256':pins['native_source_sha256'],'compute_manifest_sha256':pins['compute_manifest_sha256'],
            'native_shader_and_cpu_sources_identical_to_1994':True}
