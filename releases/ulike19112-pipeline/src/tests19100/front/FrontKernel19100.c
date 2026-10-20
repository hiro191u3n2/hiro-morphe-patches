/* Actual old/new native kernels and JNI transactions. Outputs include raw
 * transform/model bits plus complete saved pixels. Timing uses emulated-process
 * CPU time to exclude host descheduling; it is not an Android device forecast. */
#ifndef FRONT_KERNEL
#define FRONT_KERNEL "../../native1955/single_noise1955.c"
#endif
#include FRONT_KERNEL
#include <time.h>
#include <stdio.h>

typedef struct {jsize length;void *data;} Array1997;
static unsigned long checks1997,pixels1997,tiles1997;
static void check1997(int ok,const char *why){checks1997++;if(!ok){fprintf(stderr,"front ARM: %s\n",why);exit(2);}}
static jsize JNICALL length1997(JNIEnv*e,jarray a){(void)e;return ((Array1997*)a)->length;}
static jboolean JNICALL same1997(JNIEnv*e,jobject a,jobject b){(void)e;return a==b;}
static jint *JNICALL ints1997(JNIEnv*e,jintArray a,jboolean *copy){(void)e;(void)copy;return ((Array1997*)a)->data;}
static jfloat *JNICALL floats1997(JNIEnv*e,jfloatArray a,jboolean *copy){(void)e;(void)copy;return ((Array1997*)a)->data;}
static void JNICALL releaseints1997(JNIEnv*e,jintArray a,jint*p,jint mode){(void)e;(void)a;(void)p;(void)mode;}
static void JNICALL releasefloats1997(JNIEnv*e,jfloatArray a,jfloat*p,jint mode){(void)e;(void)a;(void)p;(void)mode;}
static jboolean JNICALL exception1997(JNIEnv*e){(void)e;return JNI_FALSE;}
static void JNICALL put1997(JNIEnv*e,jintArray a,jsize at,jsize n,const jint*p){(void)e;Array1997*v=(Array1997*)a;check1997(at>=0&&n>=0&&at+n<=v->length,"bounded JNI output commit");memcpy((jint*)v->data+at,p,(size_t)n*4);}
static uint32_t state1997=1997143;
static uint32_t random1997(void){state1997^=state1997<<13;state1997^=state1997>>17;state1997^=state1997<<5;return state1997;}
static int modelColumns19100=1,modelRows19100=1,patchW19100,patchH19100;
static void run1997(JNIEnv *env,const jint *input,jint *result,int width,int height,int core,
        int nr,int shadows,int colour,Array1997 *model,const jint *policy){
    for(int first=0;first<height;first+=core){
        int finish=mini(height,first+core),top=maxi(0,first-HALO),bottom=mini(height,finish+HALO);
        int rows=bottom-top,begin=first-top,end=finish-top,count=width*rows;
        jint *output=malloc((size_t)count*4);check1997(output!=NULL,"output allocation");
        for(int i=0;i<count;i++)output[i]=0x13579bdf;
        Array1997 in={count,(void*)(input+width*top)},out={count,output};
        Array1997 protection={(finish-first)*width*2,(void*)(policy+width*first*2)};
        colour_enabled1976=colour;
        check1997(Java_com_hiro_ulike_SingleNoise1955_processNative(env,NULL,
            (jintArray)&in,(jintArray)&out,width,rows,begin,end,0,rows,top,nr,shadows,
            width,height,modelColumns19100,modelRows19100,patchW19100,patchH19100,(jfloatArray)model,(jintArray)&protection),"production JNI accepts exact halo and absolute origin");
        colour_enabled1976=0;
        for(int i=0;i<count;i++)if(i<begin*width||i>=end*width)check1997(output[i]==0x13579bdf,"halo output remains unmodified");
        memcpy(result+first*width,output+begin*width,(size_t)(finish-first)*width*4);
        free(output);tiles1997++;
    }
}
static uint64_t nanos19100(void){struct timespec t;clock_gettime(CLOCK_PROCESS_CPUTIME_ID,&t);return (uint64_t)t.tv_sec*1000000000+t.tv_nsec;}
static void write19100(FILE*f,const void*value,size_t size){check1997(fwrite(value,1,size,f)==size,"complete proof output");}
static float *model19100(int width,int height,int scene,int *count) {
    modelColumns19100=width>13?3:1;modelRows19100=height>13?3:1;
    patchW19100=mini(13,width);patchH19100=mini(13,height);
    int cells=modelColumns19100*modelRows19100;*count=cells*3+MODEL_EXTRA;
    float *data=malloc((size_t)*count*4);check1997(data!=NULL,"model allocation");
    for(int i=0;i<cells;i++) {
        data[i]=scene==0?.29f:scene==1?.30f:(float)(random1997()%4000)/333.f+.2f;
        data[cells+i]=scene==2?.29f:(float)(random1997()%5000)/333.f+.2f;
        data[2*cells+i]=(float)(random1997()%25500)/100.f;
    }
    for(int i=cells*3;i<*count;i++)data[i]=(float)(random1997()%1000)/333.f+.2f;
    return data;
}
static void inputs19100(int width,int height,int scene,jint *input,jint *policy) {
    for(int y=0;y<height;y++)for(int x=0;x<width;x++){
        int i=y*width+x;uint32_t r=random1997();int v=60+(int)(r%107),alpha=scene==1&&i%41==0?0x70:0xff;
        input[i]=(jint)((uint32_t)alpha<<24|(uint32_t)(scene==2?(x<width/2?60:180):v)<<16|(uint32_t)v<<8|(uint32_t)(scene==3?(int)(r%256):v));
        policy[2*i]=i%29==0?0:(int)(r%257);policy[2*i+1]=(int)((r>>16)%257);
    }
}
static void raw19100(FILE *f) {
    for(int n=4;n<=8;n+=4)for(int trial=0;trial<4096;trial++) {
        double values[64]={0},temp[64]={0};
        for(int i=0;i<n*n;i++)values[i]=((int32_t)random1997())/8192.0;
        transform(values,temp,n,0);write19100(f,values,(size_t)n*n*8);
        transform(values,temp,n,1);write19100(f,values,(size_t)n*n*8);
    }
    for(int scene=0;scene<4;scene++) {
        int width=67,height=83,count;float *d=model19100(width,height,scene,&count);Model m={0};
        m.width=width;m.height=height;m.columns=modelColumns19100;m.rows=modelRows19100;
        int cells=m.columns*m.rows;m.luma=d;m.chroma=d+cells;m.mean=d+2*cells;
        m.brightness_y=d+3*cells;m.brightness_c=m.brightness_y+BINS;m.ratios=m.brightness_c+BINS;
        m.left=(patchW19100-1)*.5f;m.top=(patchH19100-1)*.5f;
        m.step_x=(float)(width-patchW19100)/(m.columns-1);m.step_y=(float)(height-patchH19100)/(m.rows-1);
        for(int i=0;i<8192;i++) {
            int x=(int)(random1997()%80)-7,y=(int)(random1997()%96)-7;float mean=(float)(random1997()%25500)/100.f,sy,sc;
#ifdef FRONT_BASELINE
            sy=model_sigma(&m,x,y,mean,0);sc=model_sigma(&m,x,y,mean,1);
#else
            model_sigmas19100(&m,x,y,mean,&sy,&sc);
#endif
            write19100(f,&sy,4);write19100(f,&sc,4);
        }
        free(d);
    }
}
int main(int argc,char **argv){
    check1997(argc==3,"proof file and benchmark mode required");
    struct JNINativeInterface_ table={0};table.GetArrayLength=length1997;table.IsSameObject=same1997;
    table.GetIntArrayElements=ints1997;table.GetFloatArrayElements=floats1997;
    table.ReleaseIntArrayElements=releaseints1997;table.ReleaseFloatArrayElements=releasefloats1997;
    table.ExceptionCheck=exception1997;table.SetIntArrayRegion=put1997;JNIEnv env=&table;
    FILE *f=fopen(argv[1],"wb");check1997(f!=NULL,"proof file");int cases=0;
    if(atoi(argv[2])==0) {
        raw19100(f);
        const int widths[]={1,2,7,8,17,33,64,67},heights[]={1,7,13,17,33,65,71,83};
        for(int shape=0;shape<8;shape++)for(int scene=0;scene<4;scene++) {
            int width=widths[shape],height=heights[shape],n=width*height,count;
            jint *input=malloc((size_t)n*4),*copy=malloc((size_t)n*4),*actual=malloc((size_t)n*4),*policy=malloc((size_t)n*8);
            check1997(input&&copy&&actual&&policy,"fixture buffers");inputs19100(width,height,scene,input,policy);memcpy(copy,input,(size_t)n*4);
            float *values=model19100(width,height,scene,&count);Array1997 model={count,values};
            for(int colour=0;colour<2;colour++)for(int nr=0;nr<=4;nr++) {
                run1997(&env,input,actual,width,height,16+16*((scene+nr)%4),nr,scene%2,colour,&model,policy);
                write19100(f,actual,(size_t)n*4);pixels1997+=(unsigned long)n;
                check1997(memcmp(input,copy,(size_t)n*4)==0,"source immutable");cases++;
            }
            free(input);free(copy);free(actual);free(policy);free(values);
        }
        fclose(f);printf("{\"status\":\"passed\",\"pixel_cases\":%d,\"pixels\":%lu,\"assertions\":%lu,\"transform_cases\":8192,\"model_cases\":32768}\n",cases,pixels1997,checks1997);return 0;
    }
    int width=192,height=128,n=width*height,count;
    jint *input=malloc((size_t)n*4),*actual=malloc((size_t)n*4),*policy=malloc((size_t)n*8);
    check1997(input&&actual&&policy,"benchmark buffers");inputs19100(width,height,3,input,policy);
    float *values=model19100(width,height,3,&count);Array1997 model={count,values};
    uint64_t times[5];
    for(int trial=-1;trial<5;trial++) {
        uint64_t start=nanos19100();
        for(int repeat=0;repeat<3;repeat++)run1997(&env,input,actual,width,height,256,4,1,0,&model,policy);
        if(trial>=0)times[trial]=nanos19100()-start;
    }
    write19100(f,actual,(size_t)n*4);fclose(f);
    printf("{\"status\":\"passed\",\"bench_ns\":[%llu,%llu,%llu,%llu,%llu],\"bench_pixels\":%d}\n",(unsigned long long)times[0],(unsigned long long)times[1],(unsigned long long)times[2],(unsigned long long)times[3],(unsigned long long)times[4],n*3);
    free(input);free(actual);free(policy);free(values);return 0;
}
