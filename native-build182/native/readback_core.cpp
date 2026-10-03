#include "readback_core.h"
#include <math.h>
#include <string.h>
#include <stdint.h>

namespace hiro_readback {
namespace {
constexpr unsigned COPY_READ=0x8f36, BUFFER_SIZE=0x8764, MAPPED=0x88bc;
constexpr unsigned FLOAT=0x1406, MAT4=0x8b5c, READ=1;
constexpr unsigned CURRENT_PROGRAM=0x8b8d, ELEMENT_BINDING=0x8895, VIEWPORT=0x0ba2;
constexpr unsigned ENABLED=0x8622, SIZE=0x8623, STRIDE=0x8624, TYPE=0x8625;
constexpr unsigned NORMALIZED=0x886a, BUFFER_BINDING=0x889f, POINTER=0x8645;
constexpr unsigned INTEGER=0x88fd, DIVISOR=0x88fe, UTYPE=0x8a37, USIZE=0x8a38;
struct Restore {
    const GL& gl; int original;
    explicit Restore(const GL& g):gl(g),original(0){gl.integer(COPY_READ,&original);}
    ~Restore(){gl.bindBuffer(COPY_READ,static_cast<unsigned>(original));}
};
Status read(const GL& gl,unsigned buffer,uint64_t offset,uint64_t length,
            void (*consume)(const unsigned char*,void*),void* context){
    if(!buffer || !length || length>8u*1024u*1024u ||
       offset>static_cast<uint64_t>(PTRDIFF_MAX))return BUFFER_BOUNDS;
    gl.bindBuffer(COPY_READ,buffer);
    int size=0,mapped=0;gl.bufferInteger(COPY_READ,BUFFER_SIZE,&size);
    gl.bufferInteger(COPY_READ,MAPPED,&mapped);
    if(mapped)return BUFFER_MAPPED;
    if(size<0 || offset>static_cast<unsigned>(size) || length>static_cast<unsigned>(size)-offset)return BUFFER_BOUNDS;
    void* data=gl.mapBuffer(COPY_READ,static_cast<ptrdiff_t>(offset),static_cast<ptrdiff_t>(length),READ);
    if(!data)return MAP_FAILED;
    consume(static_cast<const unsigned char*>(data),context);
    return gl.unmapBuffer(COPY_READ)?OK:UNMAP_FAILED;
}
struct IndexCopy {Draw* draw;unsigned bytes;};
void copyIndices(const unsigned char* data,void* arg){
    auto& c=*static_cast<IndexCopy*>(arg);
    for(unsigned i=0;i<c.draw->count;i++){
        uint32_t value=0;memcpy(&value,data+i*c.bytes,c.bytes);c.draw->indices[i]=value;
    }
}
struct AttributeCopy {float* output;unsigned count,components,stride;};
void copyAttribute(const unsigned char* data,void* arg){
    auto& c=*static_cast<AttributeCopy*>(arg);
    for(unsigned i=0;i<c.count;i++)for(unsigned j=0;j<c.components;j++)
        memcpy(c.output+i*c.components+j,data+static_cast<size_t>(i)*c.stride+j*4,4);
}
Status attribute(const GL& gl,int location,unsigned components,Draw& d,float* output,unsigned& buffer){
    int enabled=0,size=0,stride=0,type=0,normalized=0,integer=0,divisor=0,binding=0;
    gl.attribInteger(location,ENABLED,&enabled);gl.attribInteger(location,SIZE,&size);
    gl.attribInteger(location,STRIDE,&stride);gl.attribInteger(location,TYPE,&type);
    gl.attribInteger(location,NORMALIZED,&normalized);gl.attribInteger(location,INTEGER,&integer);
    gl.attribInteger(location,DIVISOR,&divisor);gl.attribInteger(location,BUFFER_BINDING,&binding);
    if(!enabled || size!=static_cast<int>(components) || type!=static_cast<int>(FLOAT) ||
       normalized || integer || divisor || binding<=0 || stride<0)return ATTRIBUTE_LAYOUT;
    if(stride==0)stride=static_cast<int>(components*4);
    if(stride<static_cast<int>(components*4) || stride>65536)return ATTRIBUTE_LAYOUT;
    void* pointer=nullptr;gl.attribPointer(location,POINTER,&pointer);
    uint64_t base=reinterpret_cast<uintptr_t>(pointer);
    uint64_t delta=static_cast<uint64_t>(d.firstVertex)*static_cast<unsigned>(stride);
    if(base>UINT64_MAX-delta)return BUFFER_BOUNDS;
    uint64_t span=static_cast<uint64_t>(d.vertexCount-1)*static_cast<unsigned>(stride)+components*4;
    AttributeCopy c{output,d.vertexCount,components,static_cast<unsigned>(stride)};
    buffer=static_cast<unsigned>(binding);Status s=read(gl,buffer,base+delta,span,copyAttribute,&c);
    if(s!=OK)return s;
    for(unsigned i=0;i<d.vertexCount*components;i++)if(!isfinite(output[i]))return NONFINITE;
    return OK;
}
Status uniform(const GL& gl,unsigned program,const char* name,unsigned expected,float* target,unsigned length){
    unsigned index=~0u;gl.uniformIndices(program,1,&name,&index);if(index==~0u)return UNIFORM_LAYOUT;
    int type=0,size=0;gl.uniformProperties(program,1,&index,UTYPE,&type);
    gl.uniformProperties(program,1,&index,USIZE,&size);
    if(type!=static_cast<int>(expected)||size!=1)return UNIFORM_LAYOUT;
    int location=gl.uniformLocation(program,name);if(location<0)return UNIFORM_LAYOUT;
    gl.uniformFloats(program,location,target);
    for(unsigned i=0;i<length;i++)if(!isfinite(target[i]))return NONFINITE;
    return OK;
}
}
Status capture(const GL& gl,unsigned mode,int count,unsigned type,const void* indices,Draw& d){
    int program=0;gl.integer(CURRENT_PROGRAM,&program);if(program<=0)return NO_PROGRAM;
    int positions=gl.attribLocation(program,"attPosition"),uv=gl.attribLocation(program,"attUV"),opacity=gl.attribLocation(program,"attOpacity");
    if(positions<0||uv<0||opacity<0)return NOT_MATCHED;
    if(count<=0 || count>static_cast<int>(MAX_INDICES))return BUDGET;
    unsigned bytes=type==0x1401?1:type==0x1403?2:type==0x1405?4:0;
    if(!bytes)return INDEX_LAYOUT;
    int ebo=0;gl.integer(ELEMENT_BINDING,&ebo);if(ebo<=0)return INDEX_LAYOUT;
    d.program=program;d.mode=mode;d.count=count;d.indexType=type;d.indexBuffer=ebo;
    d.indexOffset=reinterpret_cast<uintptr_t>(indices);
    Restore restore(gl);IndexCopy copy{&d,bytes};
    Status s=read(gl,d.indexBuffer,d.indexOffset,static_cast<uint64_t>(count)*bytes,copyIndices,&copy);
    if(s!=OK)return s;
    unsigned minimum=~0u,maximum=0;
    for(unsigned i=0;i<d.count;i++){if(d.indices[i]<minimum)minimum=d.indices[i];if(d.indices[i]>maximum)maximum=d.indices[i];}
    if(static_cast<uint64_t>(maximum)-minimum+1>MAX_VERTICES)return BUDGET;
    d.firstVertex=minimum;d.vertexCount=maximum-minimum+1;
    if((s=attribute(gl,positions,2,d,d.position,d.positionBuffer))!=OK)return s;
    if((s=attribute(gl,uv,2,d,d.uv,d.uvBuffer))!=OK)return s;
    if((s=attribute(gl,opacity,1,d,d.opacity,d.opacityBuffer))!=OK)return s;
    if((s=uniform(gl,d.program,"uMVPMatrix",MAT4,d.mvp,16))!=OK)return s;
    if((s=uniform(gl,d.program,"uSTMatrix",MAT4,d.st,16))!=OK)return s;
    if((s=uniform(gl,d.program,"intensity",FLOAT,&d.intensity,1))!=OK)return s;
    gl.integer(VIEWPORT,d.viewport);
    return OK;
}
const char* statusName(Status s){
    const char* names[]={"ok","not_matched","no_program","index_layout","attribute_layout","buffer_bounds","buffer_already_mapped","map_failed","unmap_failed","uniform_layout","nonfinite","budget"};
    return s>=OK&&s<=BUDGET?names[s]:"unknown";
}
}
