/* GX13: exact binary64 coefficient preparation, original native1955 arithmetic.
 * The GPU receives only values already cast to float by the1955 CPU algorithm;
 * no double DCT/safeguard/sample is approximated or removed.
 * Include the frozen scalar/NEON helpers without exporting duplicate1955JNI.
 */
#define Java_com_hiro_ulike_SingleNoise1955_nativeAbi gx13_reference_abi1961
#define Java_com_hiro_ulike_SingleNoise1955_processNative gx13_reference_process1961
#define COLOUR_CACHE_CPU_INCLUDED1976
#include "../native1955/single_noise1955.c"
#undef COLOUR_CACHE_CPU_INCLUDED1976
#undef Java_com_hiro_ulike_SingleNoise1955_nativeAbi
#undef Java_com_hiro_ulike_SingleNoise1955_processNative

static int export_block1961(const jint *input, int width, int valid_begin, int valid_end,
        int origin_y, const Model *m,
        int noise, int shadows, int bx, int by, int n, int start, int finish, Workspace *w, float *out) {
    out[0]=0.f;
    int absolute_start = start+origin_y, absolute_end = finish+origin_y;
    if (by+n <= absolute_start || by >= absolute_end || bx+n <= 0) return 1;
    double mean = 0; int opaque = 1;
    if (!source_block1976(input,width,valid_begin,valid_end,origin_y,m->height,
            bx,by,n,w,&mean,&opaque)) return 0;
    if (!opaque) return 1;
    mean /= n*n;
    int cx = maxi(0, mini(width-1, bx+n/2)), cy = maxi(0, mini(m->height-1, by+n/2));
    float sy = model_sigma(m, cx, cy, (float)mean, 0), sc = model_sigma(m, cx, cy, (float)mean, 1);
    if (sy < .30f && sc < .30f) return 1;
    float strength = .35f + noise*.21f;
    if (shadows) strength *= 1.f + .12f*clampf((128-(float)mean)/96, 0, 1);
    w->sigma[0] = sy; w->sigma[1] = w->sigma[2] = sc;
    for (int plane = 0; plane < 3; plane++) {
        for (int band = 0; band < BANDS; band++) {
            float shape = spectral(m, plane, band, (float)mean);
            w->variance[plane*BANDS+band] = w->sigma[plane]*w->sigma[plane]*shape*shape*strength;
        }
        transform(w->coefficient[plane], w->temp, n, 0);
        for (int v = 0; v < n; v++) for (int u = 0; u < n; u++) {
            if (u+v == 0) continue; /* Keep DC at both scales. */
            int at = v*n+u; double coefficient = w->coefficient[plane][at];
            int frequency = (u+v)*8/n, band = frequency < 4 ? 0 : frequency < 8 ? 1 : 2;
            float variance = w->variance[plane*BANDS+band];
            if (plane == 0 && u+v == 1) variance *= .32f;
            double power = coefficient*coefficient;
            if (variance <= .03f || power > variance*36) continue;
            double gain = power/(power+variance*1.25);
            w->coefficient[plane][at] = coefficient*gain;
        }
        transform(w->coefficient[plane], w->temp, n, 1);
    }
    for (int i = 0; i < n*n; i++) w->residual[i] = w->source[0][i]-w->coefficient[0][i];
    float safeguard = residual_safeguard(w->source[0], w->residual, n, sy);
    out[0]=1.f;out[1]=safeguard;
    for(int plane=0;plane<3;plane++)for(int i=0;i<n*n;i++)
        out[2+plane*n*n+i]=(float)(w->coefficient[plane][i]-w->source[plane][i]);
    return 1;
}

JNIEXPORT jfloatArray JNICALL Java_com_hiro_ulike_SingleResidual1961_prepareNative(
        JNIEnv *env,jclass cls,jintArray source,jint width,jint rows,jint begin,jint end,
        jint valid_begin,jint valid_end,jint origin_y,jint noise,jboolean shadows,
        jint model_height,jint columns,jint model_rows,jint patch_w,jint patch_h,
        jfloatArray model_data) {
    (void)cls;
    int64_t source_count=(int64_t)width*rows,cells=(int64_t)columns*model_rows;
    int64_t start=(int64_t)origin_y+begin,finish=(int64_t)origin_y+end;
    if(!source||!model_data||width<1||rows<1||model_height<1||model_height>INT_MAX-8||
       width>INT_MAX-8||columns<1||model_rows<1||cells>(INT_MAX-MODEL_EXTRA)/3||
       patch_w<1||patch_h<1||patch_w>width||patch_h>model_height||
       (columns>1&&patch_w==width)||(model_rows>1&&patch_h==model_height)||noise<1||noise>4||
       valid_begin<0||valid_end>rows||begin<valid_begin||end<=begin||end>valid_end||
       origin_y+(int64_t)valid_begin<0||origin_y+(int64_t)valid_end>model_height||
       source_count>INT_MAX||source_count>(*env)->GetArrayLength(env,source)||
       cells*3+MODEL_EXTRA>(*env)->GetArrayLength(env,model_data)||
       origin_y+(int64_t)valid_begin>(start-HALO>0?start-HALO:0)||
       origin_y+(int64_t)valid_end<(finish+HALO<model_height?finish+HALO:model_height))return NULL;
    int first=floor_div4((int)start-7)*4,block_cols=(width+3)/4+1;
    int64_t block_rows=(finish-first+3)/4,record_count=(int64_t)block_cols*block_rows*248;
    if(record_count<1||record_count>INT_MAX||record_count>(int64_t)(UINT32_C(512)*1024*1024/sizeof(float)))return NULL;
    jint *input=(*env)->GetIntArrayElements(env,source,NULL);if(!input)return NULL;
    jfloat *data=(*env)->GetFloatArrayElements(env,model_data,NULL);
    jfloatArray result=NULL;jfloat *records=NULL;int success=0;
    if(!data)goto done;
    for(int64_t i=0;i<cells*3+MODEL_EXTRA;i++)if(!isfinite(data[i])||data[i]<0||data[i]>1000000.f)goto done;
    result=(*env)->NewFloatArray(env,(jsize)record_count);if(!result)goto done;
    records=(*env)->GetFloatArrayElements(env,result,NULL);if(!records)goto done;
    Model m={0};Workspace w={0};w.colour_enabled1976=colour_enabled1976;
    m.width=width;m.height=model_height;m.columns=columns;m.rows=model_rows;
    m.luma=data;m.chroma=data+cells;m.mean=data+cells*2;
    m.brightness_y=data+cells*3;m.brightness_c=m.brightness_y+BINS;m.ratios=m.brightness_c+BINS;
    m.left=(patch_w-1)*.5f;m.top=(patch_h-1)*.5f;
    m.step_x=columns==1?1.f:(float)(width-patch_w)/(columns-1);
    m.step_y=model_rows==1?1.f:(float)(model_height-patch_h)/(model_rows-1);
    for(int64_t row=0;row<block_rows;row++)for(int col=0;col<block_cols;col++) {
        int bx=col*4-4,by=first+(int)row*4;float *out=records+(row*block_cols+col)*248;
        if(!export_block1961(input,width,valid_begin,valid_end,origin_y,&m,noise,shadows,bx,by,8,begin,end,&w,out))goto done;
        out[196]=0.f;
        if(by+4>start&&bx>=0&&!export_block1961(input,width,valid_begin,valid_end,origin_y,
             &m,noise,shadows,bx,by,4,begin,end,&w,out+196))goto done;
    }
    success=1;
done:
    if(records)(*env)->ReleaseFloatArrayElements(env,result,records,success?0:JNI_ABORT);
    if(data)(*env)->ReleaseFloatArrayElements(env,model_data,data,JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env,source,input,JNI_ABORT);
    if(!success&&result){(*env)->DeleteLocalRef(env,result);result=NULL;}
    return result;
}

/* GX34/GX35 direct preparation cache. Only two tail rows from the immediately
 * preceding successful band are reusable. Every reused record is guarded by
 * identical model bytes, geometry/halo settings and all 64 clamped source
 * samples. The alternate pair is written transactionally and committed after
 * the complete band. Java owns every byte until its joined lease closes. */
#include <stddef.h>
#define GX34_CACHE_MAGIC UINT32_C(0x19643435)
#define GX34_RECORD_FLOATS 248

typedef struct {
    uint32_t magic;
    int width,rows,valid_begin,valid_end,origin_y,noise,shadows,model_height;
    int columns,model_rows,patch_w,patch_h,model_count;
    int bank,valid,prepared,reserved,hits8,hits4;
} ResidualCacheHeader1964;
typedef struct {
    float records[GX34_RECORD_FLOATS];
    jint samples[64];
    int by,valid8,valid4;
} ResidualCacheBlock1964;
_Static_assert(sizeof(ResidualCacheHeader1964)==80,"GX34 Java header size");
_Static_assert(sizeof(ResidualCacheBlock1964)==1260,"GX34 Java block size");

static int direct_samples1964(const jint *input,int width,int valid_begin,int valid_end,
        int origin_y,int model_height,int bx,int by,jint samples[64]) {
    for(int yy=0;yy<8;yy++)for(int xx=0;xx<8;xx++) {
        int gx=maxi(0,mini(width-1,bx+xx));
        int gy=maxi(0,mini(model_height-1,by+yy))-origin_y;
        if(gy<valid_begin||gy>=valid_end)return 0;
        samples[yy*8+xx]=input[(size_t)gy*width+gx];
    }
    return 1;
}
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_SingleResidual1961_prepareDirectNative(
        JNIEnv *env,jclass cls,jobject cache_buffer,jobject record_buffer,jintArray source,
        jint width,jint rows,jint begin,jint end,jint valid_begin,jint valid_end,
        jint origin_y,jint noise,jboolean shadows,jint model_height,jint columns,
        jint model_rows,jint patch_w,jint patch_h,jfloatArray model_data) {
    (void)cls;
    int64_t source_count=(int64_t)width*rows,cells=(int64_t)columns*model_rows;
    int64_t start=(int64_t)origin_y+begin,finish=(int64_t)origin_y+end;
    if(!source||!model_data||!cache_buffer||!record_buffer||width<1||rows<1||
       model_height<1||model_height>INT_MAX-8||width>INT_MAX-8||columns<1||model_rows<1||
       cells>(INT_MAX-MODEL_EXTRA)/3||patch_w<1||patch_h<1||patch_w>width||patch_h>model_height||
       (columns>1&&patch_w==width)||(model_rows>1&&patch_h==model_height)||noise<1||noise>4||
       valid_begin<0||valid_end>rows||begin<valid_begin||end<=begin||end>valid_end||
       origin_y+(int64_t)valid_begin<0||origin_y+(int64_t)valid_end>model_height||
       source_count>INT_MAX||source_count>(*env)->GetArrayLength(env,source)||
       cells*3+MODEL_EXTRA>(*env)->GetArrayLength(env,model_data)||
       origin_y+(int64_t)valid_begin>(start-HALO>0?start-HALO:0)||
       origin_y+(int64_t)valid_end<(finish+HALO<model_height?finish+HALO:model_height))return JNI_FALSE;
    int first=floor_div4((int)start-7)*4,block_cols=(width+3)/4+1;
    int64_t block_rows=(finish-first+3)/4,record_count=(int64_t)block_cols*block_rows*248;
    int model_count=(int)(cells*3+MODEL_EXTRA);
    int64_t cache_bytes=sizeof(ResidualCacheHeader1964)+(int64_t)model_count*sizeof(float)+
        (int64_t)block_cols*4*sizeof(ResidualCacheBlock1964);
    if(record_count<1||record_count>INT_MAX||record_count>(int64_t)(UINT32_C(512)*1024*1024/sizeof(float))||
       cache_bytes<1||cache_bytes>UINT32_C(512)*1024*1024)return JNI_FALSE;
    float *records=(*env)->GetDirectBufferAddress(env,record_buffer);
    ResidualCacheHeader1964 *cache=(*env)->GetDirectBufferAddress(env,cache_buffer);
    if(!records||!cache||((uintptr_t)records&3)||((uintptr_t)cache&3)||
       (*env)->GetDirectBufferCapacity(env,record_buffer)!=record_count*(int64_t)sizeof(float)||
       (*env)->GetDirectBufferCapacity(env,cache_buffer)<cache_bytes)return JNI_FALSE;
    jint *input=(*env)->GetIntArrayElements(env,source,NULL);if(!input)return JNI_FALSE;
    jfloat *data=(*env)->GetFloatArrayElements(env,model_data,NULL);int success=0;
    if(!data)goto done1964;
    for(int i=0;i<model_count;i++)if(!isfinite(data[i])||data[i]<0||data[i]>1000000.f)goto done1964;
    ResidualCacheHeader1964 expected={0};
    expected.magic=GX34_CACHE_MAGIC;expected.width=width;expected.rows=rows;
    expected.valid_begin=valid_begin;expected.valid_end=valid_end;expected.origin_y=origin_y;
    expected.noise=noise;expected.shadows=shadows?1:0;expected.model_height=model_height;
    expected.columns=columns;expected.model_rows=model_rows;expected.patch_w=patch_w;
    expected.patch_h=patch_h;expected.model_count=model_count;
    float *saved_model=(float *)(cache+1);
    ResidualCacheBlock1964 *saved=(ResidualCacheBlock1964 *)(saved_model+model_count);
    int reusable=cache->valid&&cache->bank>=0&&cache->bank<2&&
        memcmp(cache,&expected,offsetof(ResidualCacheHeader1964,bank))==0&&
        memcmp(saved_model,data,(size_t)model_count*sizeof(float))==0;
    int old_bank=reusable?cache->bank:0,new_bank=old_bank^1;
    ResidualCacheBlock1964 *previous=saved+(size_t)old_bank*block_cols*2;
    ResidualCacheBlock1964 *next=saved+(size_t)new_bank*block_cols*2;
    memset(next,0,(size_t)block_cols*2*sizeof(*next));
    memset(records,0,(size_t)record_count*sizeof(float));
    Model m={0};Workspace w={0};w.colour_enabled1976=colour_enabled1976;
    m.width=width;m.height=model_height;m.columns=columns;m.rows=model_rows;
    m.luma=data;m.chroma=data+cells;m.mean=data+cells*2;
    m.brightness_y=data+cells*3;m.brightness_c=m.brightness_y+BINS;m.ratios=m.brightness_c+BINS;
    m.left=(patch_w-1)*.5f;m.top=(patch_h-1)*.5f;
    m.step_x=columns==1?1.f:(float)(width-patch_w)/(columns-1);
    m.step_y=model_rows==1?1.f:(float)(model_height-patch_h)/(model_rows-1);
    int hits8=0,hits4=0;
    for(int64_t row=0;row<block_rows;row++)for(int col=0;col<block_cols;col++) {
        int bx=col*4-4,by=first+(int)row*4;
        float *out=records+(row*block_cols+col)*248;
        int active8=by+8>start&&by<finish,active4=by+4>start&&by<finish&&bx>=0;
        jint samples[64];int have_samples=0,tail=row>=block_rows-2;
        ResidualCacheBlock1964 *old=NULL,*possible=NULL;
        if(reusable)for(int r=0;r<2;r++) {
            ResidualCacheBlock1964 *candidate=previous+(size_t)r*block_cols+col;
            if(candidate->by==by&&((active8&&candidate->valid8)||(active4&&candidate->valid4))){possible=candidate;break;}
        }
        // Only overlap candidates and the two newly retained rows need an
        // extra exact sample snapshot; interior transforms read input once.
        if((active8||active4)&&(possible||tail)) {
            have_samples=direct_samples1964(input,width,valid_begin,valid_end,origin_y,model_height,bx,by,samples);
            if(!have_samples)goto done1964;
            if(possible&&memcmp(possible->samples,samples,sizeof(samples))==0)old=possible;
        }
        if(active8&&old&&old->valid8){memcpy(out,old->records,196*sizeof(float));hits8++;}
        else if(!export_block1961(input,width,valid_begin,valid_end,origin_y,&m,noise,shadows,bx,by,8,begin,end,&w,out))goto done1964;
        if(active4&&old&&old->valid4){memcpy(out+196,old->records+196,52*sizeof(float));hits4++;}
        else if(active4&&!export_block1961(input,width,valid_begin,valid_end,origin_y,&m,
                noise,shadows,bx,by,4,begin,end,&w,out+196))goto done1964;
        if(tail) {
            int tail=(int)(row-(block_rows>2?block_rows-2:0));
            ResidualCacheBlock1964 *save=next+(size_t)tail*block_cols+col;
            save->by=by;save->valid8=active8;save->valid4=active4;
            if(have_samples)memcpy(save->samples,samples,sizeof(samples));
            memcpy(save->records,out,248*sizeof(float));
        }
    }
    expected.bank=new_bank;expected.valid=1;expected.prepared=reusable&&cache->prepared<INT_MAX?cache->prepared+1:1;
    expected.hits8=hits8;expected.hits4=hits4;
    memcpy(saved_model,data,(size_t)model_count*sizeof(float));
    memcpy(cache,&expected,sizeof(expected));success=1;
done1964:
    if(!success&&cache)cache->valid=0;
    if(data)(*env)->ReleaseFloatArrayElements(env,model_data,data,JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env,source,input,JNI_ABORT);
    return success?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jfloatArray JNICALL Java_com_hiro_ulike_ColourCache1976_prepareNative(
        JNIEnv *env,jclass cls,jintArray source,jint width,jint rows,jint begin,jint end,
        jint valid_begin,jint valid_end,jint origin_y,jint noise,jboolean shadows,
        jint model_height,jint columns,jint model_rows,jint patch_w,jint patch_h,
        jfloatArray model_data,jboolean cached) {
    int previous=colour_enabled1976;colour_enabled1976=cached?1:0;
    jfloatArray result=Java_com_hiro_ulike_SingleResidual1961_prepareNative(env,cls,
        source,width,rows,begin,end,valid_begin,valid_end,origin_y,noise,shadows,
        model_height,columns,model_rows,patch_w,patch_h,model_data);
    colour_enabled1976=previous;return result;
}
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_ColourCache1976_directNative(
        JNIEnv *env,jclass cls,jobject cache_buffer,jobject record_buffer,jintArray source,
        jint width,jint rows,jint begin,jint end,jint valid_begin,jint valid_end,
        jint origin_y,jint noise,jboolean shadows,jint model_height,jint columns,
        jint model_rows,jint patch_w,jint patch_h,jfloatArray model_data,jboolean cached) {
    int previous=colour_enabled1976;colour_enabled1976=cached?1:0;
    jboolean result=Java_com_hiro_ulike_SingleResidual1961_prepareDirectNative(env,cls,
        cache_buffer,record_buffer,source,width,rows,begin,end,valid_begin,valid_end,
        origin_y,noise,shadows,model_height,columns,model_rows,patch_w,patch_h,model_data);
    colour_enabled1976=previous;return result;
}
