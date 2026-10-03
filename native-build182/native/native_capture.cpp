#include "readback_core.h"
#include <jni.h>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <dlfcn.h>
#include <elf.h>
#include <fcntl.h>
#include <link.h>
#include <pthread.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <limits.h>

namespace {
using namespace hiro_readback;
using DrawElements=void(*)(GLenum,GLsizei,GLenum,const void*);
pthread_mutex_t lock=PTHREAD_MUTEX_INITIALIZER;
void* original=nullptr;
unsigned long long busySkipped=0;
Draw drawStorage;
bool installed=false;
thread_local bool nested=false;
struct Session {
    FILE* output=nullptr; Draw* scratch=nullptr; bool armed=false,ioFailed=false;
    int64_t nonce=0,deadline=0;
    unsigned records=0,maxRecords=0,successes=0,rejected=0;
    unsigned long long seen=0,notMatched=0,unsupported=0,busyBaseline=0,bytes=0;
    EGLContext contexts[16]={}; unsigned contextCount=0;
};
Session session;
// A largest record is below 2 MiB; stop at 30 MiB to keep output below 32 MiB.
constexpr uint64_t MAX_BYTES=30u*1024u*1024u;
constexpr uintptr_t DRAW_SLOT=0x1977778;
int64_t now(){timespec t{};if(clock_gettime(CLOCK_MONOTONIC,&t))return 0;return int64_t(t.tv_sec)*1000000000LL+t.tv_nsec;}
void exception(JNIEnv* env,const char* text){jclass c=env->FindClass("java/lang/IllegalStateException");if(c)env->ThrowNew(c,text);}
void footer(){
    session.armed=false;
    if(session.output){
        fprintf(session.output,"{\"kind\":\"end\",\"records\":%u,\"captured\":%u,\"rejected\":%u,\"seen\":%llu,\"not_matched\":%llu,\"unsupported_context\":%llu,\"busy_skipped\":%llu,\"io_failed_before_footer_flush\":%s}\n",
            session.records,session.successes,session.rejected,session.seen,session.notMatched,session.unsupported,
            (__atomic_load_n(&busySkipped,__ATOMIC_RELAXED)-session.busyBaseline),session.ioFailed?"true":"false");
        if(fflush(session.output)!=0)session.ioFailed=true;
        long end=ftell(session.output);if(end>=0)session.bytes=static_cast<unsigned long long>(end);
        if(fclose(session.output)!=0)session.ioFailed=true;session.output=nullptr;
    }
    session.scratch=nullptr;
}
void bits(const char* key,const float* values,unsigned count){
    fprintf(session.output,",\"%s\":[",key);
    for(unsigned i=0;i<count;i++){uint32_t b;memcpy(&b,values+i,4);fprintf(session.output,"%s%u",i?",":"",b);}
    fputc(']',session.output);
}
void writeDraw(Status status,unsigned context,unsigned long long sequence){
    FILE* f=session.output;Draw& d=*session.scratch;
    fprintf(f,"{\"kind\":\"draw\",\"sequence\":%llu,\"context_ordinal\":%u,\"status\":\"%s\",\"face_id\":null,\"same_shot_proven\":false",
        sequence,context,statusName(status));
    if(status==OK){
        fprintf(f,",\"program\":%u,\"mode\":%u,\"index_type\":%u,\"index_buffer\":%u,\"index_offset\":%llu,\"position_buffer\":%u,\"uv_buffer\":%u,\"opacity_buffer\":%u,\"first_vertex\":%u,\"vertex_count\":%u,\"viewport\":[%d,%d,%d,%d],\"indices\":[",
            d.program,d.mode,d.indexType,d.indexBuffer,static_cast<unsigned long long>(d.indexOffset),d.positionBuffer,d.uvBuffer,d.opacityBuffer,d.firstVertex,d.vertexCount,d.viewport[0],d.viewport[1],d.viewport[2],d.viewport[3]);
        for(unsigned i=0;i<d.count;i++)fprintf(f,"%s%u",i?",":"",d.indices[i]);
        fputc(']',f);bits("position_f32bits",d.position,d.vertexCount*2);
        bits("uv_f32bits",d.uv,d.vertexCount*2);bits("opacity_f32bits",d.opacity,d.vertexCount);
        bits("mvp_column_major_f32bits",d.mvp,16);bits("st_column_major_f32bits",d.st,16);bits("intensity_f32bits",&d.intensity,1);
    }
    fputs("}\n",f);session.records++;if(status==OK)session.successes++;else session.rejected++;
    long pos=ftell(f);if(pos<0||ferror(f))session.ioFailed=true;else session.bytes=static_cast<unsigned long long>(pos);
    if(session.records>=session.maxRecords||session.bytes>=MAX_BYTES||session.ioFailed)footer();
}
const GL functions={glGetIntegerv,glGetAttribLocation,glGetVertexAttribiv,glGetVertexAttribPointerv,
    glBindBuffer,glGetBufferParameteriv,glMapBufferRange,glUnmapBuffer,
    glGetUniformIndices,glGetActiveUniformsiv,glGetUniformLocation,glGetUniformfv};

void observedDraw(GLenum mode,GLsizei count,GLenum type,const void* indices){
    // No interception recursively enters itself. The original draw is always
    // called exactly once and only after every read mapping/binding is restored.
    if(!nested){
        nested=true;
        if(pthread_mutex_trylock(&lock)==0){
            if(session.armed){
                session.seen++;
                if(now()>=session.deadline){footer();}
                else {
                    EGLContext context=eglGetCurrentContext();
                    const char* version=context==EGL_NO_CONTEXT?nullptr:reinterpret_cast<const char*>(glGetString(GL_VERSION));
                    if(!version||strncmp(version,"OpenGL ES 3.",12)!=0)session.unsupported++;
                    else {
                        unsigned ordinal=0;
                        while(ordinal<session.contextCount&&session.contexts[ordinal]!=context)ordinal++;
                        if(ordinal==session.contextCount&&session.contextCount<16)session.contexts[session.contextCount++]=context;
                        if(ordinal>=16)session.unsupported++;
                        else {
                            Status status=capture(functions,mode,count,type,indices,*session.scratch);
                            if(status==NOT_MATCHED||status==NO_PROGRAM)session.notMatched++;
                            else writeDraw(status,ordinal+1,session.seen);
                        }
                    }
                }
            }
            pthread_mutex_unlock(&lock);
        } else __atomic_fetch_add(&busySkipped,1ULL,__ATOMIC_RELAXED);
        nested=false;
    }
    DrawElements next=reinterpret_cast<DrawElements>(__atomic_load_n(&original,__ATOMIC_ACQUIRE));if(next)next(mode,count,type,indices);
}

struct Target {const char* path;void** slot=nullptr;const char* error="LIBEFFECT_NOT_LOADED";};
bool range(const dl_phdr_info* info,uintptr_t p,size_t n){
    if(p+n<p)return false;
    for(unsigned i=0;i<info->dlpi_phnum;i++){
        const auto& h=info->dlpi_phdr[i];uintptr_t start=info->dlpi_addr+h.p_vaddr;
        if(h.p_type==PT_LOAD&&(h.p_flags&PF_R)&&p>=start&&p+n<=start+h.p_memsz)return true;
    }
    return false;
}
uintptr_t relocated(const dl_phdr_info* info,uintptr_t p,size_t n){
    if(range(info,p,n))return p;
    if(p<=UINTPTR_MAX-info->dlpi_addr&&range(info,info->dlpi_addr+p,n))return info->dlpi_addr+p;
    return 0;
}
int locate(dl_phdr_info* info,size_t,void* raw){
    auto& target=*static_cast<Target*>(raw);char resolved[PATH_MAX];
    if(!info->dlpi_name||!realpath(info->dlpi_name,resolved)||strcmp(resolved,target.path))return 0;
#if !defined(__aarch64__)
    target.error="ARM64_REQUIRED";return 1;
#else
    const ElfW(Dyn)* dynamic=nullptr;size_t entries=0;
    for(unsigned i=0;i<info->dlpi_phnum;i++)if(info->dlpi_phdr[i].p_type==PT_DYNAMIC){
        auto& h=info->dlpi_phdr[i];uintptr_t address=info->dlpi_addr+h.p_vaddr;
        if(!range(info,address,h.p_memsz)){target.error="DYNAMIC_RANGE";return 1;}
        dynamic=reinterpret_cast<const ElfW(Dyn)*>(address);entries=h.p_memsz/sizeof(ElfW(Dyn));
    }
    if(!dynamic||entries>16384){target.error="DYNAMIC_LAYOUT";return 1;}
    uintptr_t strings=0,symbols=0,relocations=0;size_t strsize=0,relsize=0,syment=0;ElfW(Sxword) reltype=0;
    for(size_t i=0;i<entries&&dynamic[i].d_tag!=DT_NULL;i++){
        auto& d=dynamic[i];switch(d.d_tag){
            case DT_STRTAB:strings=d.d_un.d_ptr;break;case DT_STRSZ:strsize=d.d_un.d_val;break;
            case DT_SYMTAB:symbols=d.d_un.d_ptr;break;case DT_SYMENT:syment=d.d_un.d_val;break;
            case DT_JMPREL:relocations=d.d_un.d_ptr;break;case DT_PLTRELSZ:relsize=d.d_un.d_val;break;
            case DT_PLTREL:reltype=d.d_un.d_val;break;default:break;
        }
    }
    if(!strsize||strsize>8u*1024u*1024u||!relsize||relsize>8u*1024u*1024u||relsize%sizeof(ElfW(Rela))||reltype!=DT_RELA||syment!=sizeof(ElfW(Sym))){target.error="PLT_LAYOUT";return 1;}
    strings=relocated(info,strings,strsize);symbols=relocated(info,symbols,sizeof(ElfW(Sym)));relocations=relocated(info,relocations,relsize);
    if(!strings||!symbols||!relocations){target.error="PLT_RANGE";return 1;}
    auto* rel=reinterpret_cast<const ElfW(Rela)*>(relocations);unsigned matches=0;
    for(size_t i=0;i<relsize/sizeof(ElfW(Rela));i++)if(rel[i].r_offset==DRAW_SLOT){
        if(ELF64_R_TYPE(rel[i].r_info)!=R_AARCH64_JUMP_SLOT||rel[i].r_addend!=0){target.error="DRAW_RELOCATION";return 1;}
        uint64_t index=ELF64_R_SYM(rel[i].r_info);if(index>1000000){target.error="SYMBOL_BOUND";return 1;}
        uintptr_t address=symbols+index*sizeof(ElfW(Sym));if(!range(info,address,sizeof(ElfW(Sym)))){target.error="SYMBOL_RANGE";return 1;}
        const auto& sym=*reinterpret_cast<const ElfW(Sym)*>(address);
        const char expected[]="glDrawElements";
        if(sym.st_shndx!=SHN_UNDEF||sym.st_name>strsize||sizeof(expected)>strsize-sym.st_name||memcmp(reinterpret_cast<const void*>(strings+sym.st_name),expected,sizeof(expected))){target.error="DRAW_SYMBOL";return 1;}
        uintptr_t slot=info->dlpi_addr+DRAW_SLOT;if(!range(info,slot,sizeof(void*))||slot%alignof(void*)){target.error="DRAW_SLOT_RANGE";return 1;}
        target.slot=reinterpret_cast<void**>(slot);matches++;
    }
    target.error=matches==1?nullptr:"DRAW_SLOT_NOT_UNIQUE";return 1;
#endif
}
int protections(void* pointer){
    FILE* maps=fopen("/proc/self/maps","r");if(!maps)return -1;
    char line[1024],mode[5];unsigned long long first,last;int result=-1;auto p=reinterpret_cast<uintptr_t>(pointer);
    while(fgets(line,sizeof(line),maps))if(sscanf(line,"%llx-%llx %4s",&first,&last,mode)==3&&p>=first&&p+sizeof(void*)<=last){
        result=(mode[0]=='r'?PROT_READ:0)|(mode[1]=='w'?PROT_WRITE:0)|(mode[2]=='x'?PROT_EXEC:0);break;
    }
    fclose(maps);return result;
}
}

extern "C" JNIEXPORT void JNICALL Java_com_hiro_ulike_readback_NativeDrawCapture_nativeInstall(JNIEnv* env,jclass,jstring path){
    if(!path){exception(env,"MISSING_EFFECT_PATH");return;}
    const char* utf=env->GetStringUTFChars(path,nullptr);if(!utf)return;
    char canonical[PATH_MAX];bool valid=realpath(utf,canonical)!=nullptr;env->ReleaseStringUTFChars(path,utf);
    if(!valid){exception(env,"EFFECT_PATH_UNAVAILABLE");return;}
    pthread_mutex_lock(&lock);
    if(installed){pthread_mutex_unlock(&lock);return;}
    Target target{canonical};dl_iterate_phdr(locate,&target);
    const char* error=target.error;
    if(!error){
        void* current=__atomic_load_n(target.slot,__ATOMIC_ACQUIRE);
        void* expected=reinterpret_cast<void*>(glDrawElements);
        int prot=protections(target.slot);long page=sysconf(_SC_PAGESIZE);
        if(current!=expected)error="DRAW_ALREADY_INTERPOSED";
        else if(prot<0||!(prot&PROT_READ)||(prot&PROT_EXEC)||page<=0)error="GOT_PROTECTION_UNSUPPORTED";
        else {
            void* base=reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(target.slot)/page*page);
            if(mprotect(base,static_cast<size_t>(page),prot|PROT_WRITE))error="GOT_WRITE_DENIED";
            else {
                __atomic_store_n(&original,current,__ATOMIC_RELEASE);
                __atomic_store_n(target.slot,reinterpret_cast<void*>(observedDraw),__ATOMIC_RELEASE);
                if(mprotect(base,static_cast<size_t>(page),prot)){
                    __atomic_store_n(target.slot,current,__ATOMIC_RELEASE);
                    mprotect(base,static_cast<size_t>(page),prot);error="GOT_PROTECTION_RESTORE_FAILED";
                }else installed=true;
            }
        }
    }
    pthread_mutex_unlock(&lock);if(error)exception(env,error);
}

extern "C" JNIEXPORT void JNICALL Java_com_hiro_ulike_readback_NativeDrawCapture_nativeBegin(JNIEnv* env,jclass,jstring path,jlong nonce,jint records,jint seconds){
    if(!path||nonce<=0||records<1||records>32||seconds<1||seconds>60){exception(env,"CAPTURE_BOUNDS");return;}
    const char* utf=env->GetStringUTFChars(path,nullptr);if(!utf)return;
    pthread_mutex_lock(&lock);const char* error=nullptr;
    if(!installed)error="HOOK_NOT_INSTALLED";
    else if(session.armed||session.output)error="CAPTURE_ALREADY_ACTIVE";
    else {
        int fd=open(utf,O_WRONLY|O_CREAT|O_EXCL|O_CLOEXEC|O_NOFOLLOW,0600);
        if(fd<0)error="PRIVATE_OUTPUT_CREATE_FAILED";
        else {
            FILE* f=fdopen(fd,"wb");Draw* scratch=&drawStorage;
            if(!f){close(fd);unlink(utf);error="CAPTURE_ALLOCATION_FAILED";}
            else {
                session=Session{};session.busyBaseline=__atomic_load_n(&busySkipped,__ATOMIC_RELAXED);session.output=f;session.scratch=scratch;session.nonce=nonce;session.maxRecords=records;
                session.deadline=now()+static_cast<int64_t>(seconds)*1000000000LL;
                fprintf(f,"{\"kind\":\"header\",\"schema\":\"ulike-native-draw-readback-1\",\"nonce\":%lld,\"float_encoding\":\"ieee754-binary32-uint32-bits\",\"effect_sha256\":\"d40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e\",\"capture_scope\":\"matching draw calls during bounded arm window\",\"same_shot_proven\":false,\"face_ids_proven\":false,\"shader_identity_proven\":false,\"photo_processing_connected\":false}\n",static_cast<long long>(nonce));
                if(ferror(f)){session.ioFailed=true;footer();error="CAPTURE_HEADER_WRITE_FAILED";}
                else session.armed=true;
            }
        }
    }
    pthread_mutex_unlock(&lock);env->ReleaseStringUTFChars(path,utf);if(error)exception(env,error);
}

extern "C" JNIEXPORT jstring JNICALL Java_com_hiro_ulike_readback_NativeDrawCapture_nativeFinish(JNIEnv* env,jclass,jlong nonce){
    pthread_mutex_lock(&lock);
    if(nonce<=0||session.nonce!=nonce){pthread_mutex_unlock(&lock);exception(env,"CAPTURE_TOKEN_MISMATCH");return nullptr;}
    footer();char result[512];snprintf(result,sizeof(result),"captured=%u / rejected=%u / observed_draws=%llu / nonmatching=%llu / unsupported_context=%llu / busy_skipped=%llu / bytes=%llu / io_failed=%s / same_shot=false / face_ids=unknown",
        session.successes,session.rejected,session.seen,session.notMatched,session.unsupported,(__atomic_load_n(&busySkipped,__ATOMIC_RELAXED)-session.busyBaseline),session.bytes,session.ioFailed?"true":"false");
    pthread_mutex_unlock(&lock);return env->NewStringUTF(result);
}
