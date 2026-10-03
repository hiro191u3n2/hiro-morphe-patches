#pragma once
#include <stddef.h>
#include <stdint.h>

// All pointers below are normal GLES entry points. No vendor object offsets or
// CPU face/topology inference are used. This core is exercised with a fake GL.
namespace hiro_readback {
constexpr unsigned MAX_VERTICES = 8192, MAX_INDICES = 65536;
enum Status { OK=0, NOT_MATCHED=1, NO_PROGRAM=2, INDEX_LAYOUT=3,
    ATTRIBUTE_LAYOUT=4, BUFFER_BOUNDS=5, BUFFER_MAPPED=6, MAP_FAILED=7,
    UNMAP_FAILED=8, UNIFORM_LAYOUT=9, NONFINITE=10, BUDGET=11, QUERY_FAILED=12 };
struct GL {
    void (*integer)(unsigned,int*);
    int (*attribLocation)(unsigned,const char*);
    void (*attribInteger)(unsigned,unsigned,int*);
    void (*attribPointer)(unsigned,unsigned,void**);
    void (*bindBuffer)(unsigned,unsigned);
    void (*bufferInteger)(unsigned,unsigned,int*);
    void* (*mapBuffer)(unsigned,ptrdiff_t,ptrdiff_t,unsigned);
    unsigned char (*unmapBuffer)(unsigned);
    void (*uniformIndices)(unsigned,int,const char* const*,unsigned*);
    void (*uniformProperties)(unsigned,int,const unsigned*,unsigned,int*);
    int (*uniformLocation)(unsigned,const char*);
    void (*uniformFloats)(unsigned,int,float*);
};
struct Draw {
    unsigned program=0, mode=0, indexType=0, count=0, firstVertex=0, vertexCount=0;
    unsigned indexBuffer=0, positionBuffer=0, uvBuffer=0, opacityBuffer=0;
    uint64_t indexOffset=0;
    int viewport[4]={};
    float mvp[16]={}, st[16]={}, intensity=0;
    // Positions/UV/opacity correspond to [firstVertex,firstVertex+vertexCount).
    // Indices retain their original numbers. This is not a face-ID mapping.
    float position[MAX_VERTICES*2], uv[MAX_VERTICES*2], opacity[MAX_VERTICES];
    uint32_t indices[MAX_INDICES];
};
Status capture(const GL&,unsigned mode,int count,unsigned type,const void* indexOffset,Draw&);
const char* statusName(Status);
}
