#define ULIKE_TEST_NEON1959 1
#include "../native1958/smooth_noise1958.c"
#include <stdio.h>
#if !defined(__aarch64__)
#error Actual AArch64 target is mandatory for this gate
#endif
extern void oracle_prepare1959(const jint*,int,int,int,int,const float*,int,jint*);
extern void oracle_process1959(const jint*,int,int,int,int,const float*,const jint*,const jint*,const jint*,const jint*,jint*);
extern void oracle_guide1959(const jint*,int,int,int,int,float,float,float*);
extern int oracle_confidence1959(const jint*,int,int,int,int,const float*);
static uint64_t assertions1959,pixels1959,guide_bits1959;
static void check1959(int value,const char *label){assertions1959++;if(!value){fprintf(stderr,"Actual ARM .58 equivalence failed: %s\n",label);exit(1);}}
static uint32_t random1959(uint32_t *s){*s=*s*1664525u+1013904223u;return *s;}
typedef struct {Scratch1959 *lease;void *buffer;int id;} LeaseTest1959;
static pthread_mutex_t lease_mutex1959=PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t lease_condition1959=PTHREAD_COND_INITIALIZER;
static int lease_ready1959,lease_go1959;
static void *lease_thread1959(void *argument){
    LeaseTest1959 *t=argument;Scratch1959 tmp={0};t->lease=scratch_acquire1959(&tmp);
    if(t->lease){t->buffer=scratch_reserve1959(t->lease,0,4096);if(t->buffer)memset(t->buffer,t->id,4096);}
    pthread_mutex_lock(&lease_mutex1959);lease_ready1959++;pthread_cond_broadcast(&lease_condition1959);while(!lease_go1959)pthread_cond_wait(&lease_condition1959,&lease_mutex1959);pthread_mutex_unlock(&lease_mutex1959);
    if(t->lease){if(!t->buffer){fprintf(stderr,"concurrent reserve failed\n");exit(1);}for(int i=0;i<4096;i++)if(((unsigned char*)t->buffer)[i]!=(unsigned char)t->id){fprintf(stderr,"leased buffer overwritten or trimmed\n");exit(1);}scratch_release1959(t->lease);}return NULL;
}
static void concurrent_scratch1959(void){
    scratch_trim1959();LeaseTest1959 workers[5]={{0}};pthread_t threads[5];lease_ready1959=lease_go1959=0;
    for(int i=0;i<5;i++){workers[i].id=i+1;check1959(pthread_create(threads+i,NULL,lease_thread1959,workers+i)==0,"start real concurrent worker");}
    pthread_mutex_lock(&lease_mutex1959);while(lease_ready1959!=5)pthread_cond_wait(&lease_condition1959,&lease_mutex1959);
    int accepted=0;for(int i=0;i<5;i++)if(workers[i].lease){accepted++;for(int j=0;j<i;j++)if(workers[j].lease)check1959(workers[i].lease!=workers[j].lease,"concurrent exclusive owners");}
    check1959(accepted==4,"hard concurrent four owner cap");check1959(scratch_bytes1959()==16384,"concurrent busy accounted");scratch_trim1959();check1959(scratch_bytes1959()==16384,"concurrent trim leaves live writes");lease_go1959=1;pthread_cond_broadcast(&lease_condition1959);pthread_mutex_unlock(&lease_mutex1959);
    for(int i=0;i<5;i++){check1959(pthread_join(threads[i],NULL)==0,"join native workers");}
    check1959(scratch_bytes1959()==0,"concurrent discard after release");
}
static void scratch_check1959(void){
    Scratch1959 tmp={0},*owned[4];scratch_trim1959();check1959(scratch_bytes1959()==0,"scratch initial trim");
    for(int i=0;i<4;i++){owned[i]=scratch_acquire1959(&tmp);check1959(owned[i]!=NULL,"exclusive lease");check1959(scratch_reserve1959(owned[i],0,4096)!=NULL,"scratch reserve");for(int j=0;j<i;j++)check1959(owned[i]!=owned[j],"leases distinct");}
    check1959(scratch_acquire1959(&tmp)==NULL,"fifth lease falls back");check1959(scratch_bytes1959()==16384,"all busy bytes accounted");
    scratch_trim1959();check1959(scratch_bytes1959()==16384,"trim preserves busy bytes");for(int i=0;i<4;i++)scratch_release1959(owned[i]);check1959(scratch_bytes1959()==0,"busy discarded after release");
    for(int i=0;i<3;i++){owned[i]=scratch_acquire1959(&tmp);check1959(scratch_reserve1959(owned[i],0,40u*1024*1024)!=NULL,"large admitted buffer");}
    check1959(scratch_bytes1959()==120u*1024*1024,"large busy accounting");for(int i=0;i<3;i++)scratch_release1959(owned[i]);check1959(scratch_bytes1959()<=SCRATCH_TOTAL_IDLE1959,"idle combined cap");scratch_trim1959();check1959(scratch_bytes1959()==0,"idle cleared");
    Scratch1959 *s=scratch_acquire1959(&tmp);check1959(scratch_reserve1959(s,0,SCRATCH_RETAIN1959+1)!=NULL,"oversize active");check1959(scratch_bytes1959()==SCRATCH_RETAIN1959+1,"oversize accounted");scratch_release1959(s);check1959(scratch_bytes1959()==0,"oversize not retained");
}
static void test_shape1959(int w,int h,int pattern){
    size_t count=(size_t)w*h;uint32_t random=(uint32_t)(w*9001+h*707+pattern+1);
    jint *input=malloc(count*4),*expected=malloc(count*4),*output=malloc(count*4),*policy=malloc(count*8),*mapsdata[3];
    int rw=(w+63)/64,rh=(h+63)/64;float *ev=malloc((16+3*rw*rh)*sizeof(float));
    check1959(input&&expected&&output&&policy&&ev,"test buffers");
    for(size_t i=0;i<count;i++){
        uint32_t v=random1959(&random);int x=i%w,y=i/w,r,g,b;
        if(pattern==0){r=104+(int)(v%17)-8;g=112+(int)((v>>8)%17)-8;b=120+(int)((v>>16)%17)-8;}
        else if(pattern==1){r=28+(int)(v%21)-10;g=30+(int)((v>>8)%21)-10;b=33+(int)((v>>16)%21)-10;}
        else if(pattern==2){r=(x*19+y*11)&255;g=(x*7+y*23)&255;b=(x*5+y*13)&255;}
        else{r=v&255;g=(v>>8)&255;b=(v>>16)&255;}
        input[i]=(jint)((pattern==3&&i%11==0?0x7f000000u:0xff000000u)|((uint32_t)r<<16)|((uint32_t)g<<8)|b);
        policy[i*2]=(i%23==0?0:(i%13==0?127:256));policy[i*2+1]=(i%7==0?200:(i%19==0?256:0));
    }
    for(int i=0;i<16;i++)ev[i]=(pattern<2?2.4f+(i%4)*.7f:pattern==2?.40f+(i%3)*.5f:9.f+(i%5));
    for(int i=0;i<rw*rh;i++){ev[16+i*3]=pattern<2?3.3f:pattern==2?1.7f:12.f;ev[17+i*3]=pattern<2?4.1f:pattern==2?2.1f:10.f;ev[18+i*3]=(i%3)*.17f;}
    int mw=w,mh=h;Region maps[3];
    for(int k=0;k<3;k++){mw=ceil_div2(mw);mh=ceil_div2(mh);mapsdata[k]=malloc((size_t)mw*mh*4);check1959(mapsdata[k]!=NULL,"map buffer");for(int i=0;i<mw*mh;i++){uint32_t z=random1959(&random);mapsdata[k][i]=(jint)((80u+(z%61))<<24|((uint32_t)(int8_t)((z>>8)%17-8)<<16&0xff0000u)|((uint32_t)(int8_t)((z>>16)%17-8)<<8&0xff00u)|(((z>>24)%17-8)&255));}memset(&maps[k],0,sizeof(Region));maps[k].pixels=mapsdata[k];maps[k].width=mw;maps[k].height=mh;maps[k].finish=mh;}
    Region src={0};src.pixels=input;src.width=w;src.height=h;src.finish=h;src.colors=malloc((size_t)w*16*sizeof(Color));check1959(src.colors!=NULL,"row cache");
    for(int noise=1;noise<=4;noise+=3)for(int shadows=0;shadows<=1;shadows++)for(int mode=0;mode<=3;mode++){
        if(mode==3)oracle_process1959(input,w,h,noise,shadows,ev,mapsdata[0],mapsdata[1],mapsdata[2],policy,expected);
        else oracle_prepare1959(input,w,h,noise,shadows,ev,mode,expected);
        for(int k=0;k<16;k++)src.color_rows[k]=-1;
        for(int y=0;y<h;y++){
            cache_rows(&src,y,mode==1?7:5,0,h);
            for(int x=0;x<w;){int lanes=x+4<=w?4:1;PixelWork1959 fast[4];if(lanes==4)neon_work1959(&src,x,y,0,h,0,h,0,ev,mode,fast);
                for(int lane=0;lane<lanes;lane++){
                    int xx=x+lane;size_t at=(size_t)y*w+xx;
                    if(lanes==4){float gold[3];oracle_guide1959(input,w,h,xx,y,fast[lane].sy,fast[lane].sc,gold);check1959(memcmp(gold,fast[lane].flat,2*sizeof(float))==0,"four-lane guide float bits");guide_bits1959+=2;if(mode!=3){check1959(memcmp(gold+2,&fast[lane].legacy_flat,sizeof(float))==0,"four-lane structure float bits");guide_bits1959++;}}
                    if(mode==3){jint confidence=-1;process_pixel(&src,maps,xx,y,0,h,0,h,0,noise,shadows,ev,policy+at*2,output+at,lanes==4?fast+lane:NULL,&confidence);if((input[at]>>24&255)==255&&policy[at*2]>0)check1959(confidence==oracle_confidence1959(input,w,h,xx,y,ev),"shared NR13 exact Q8");}
                    else prepare_pixel(&src,xx,y,noise,shadows,ev,mode,output+at,lanes==4?fast+lane:NULL);
                    if(output[at]!=expected[at]){fprintf(stderr,"w%d h%d pattern%d mode%d noise%d shadow%d x%d y%d expected%08x actual%08x\n",w,h,pattern,mode,noise,shadows,xx,y,(unsigned)expected[at],(unsigned)output[at]);check1959(0,"output pixel");}check1959(1,"output pixel");pixels1959++;
                }x+=lanes;
            }
        }
    }
    free(src.colors);free(input);free(expected);free(output);free(policy);free(ev);for(int k=0;k<3;k++)free(mapsdata[k]);
}
int main(void){
    scratch_check1959();concurrent_scratch1959();int shapes[][2]={{1,1},{2,3},{3,2},{4,9},{5,7},{7,5},{16,17},{17,16},{33,19},{65,9},{9,129}};
    for(size_t i=0;i<sizeof(shapes)/sizeof(shapes[0]);i++)for(int pattern=0;pattern<4;pattern++)test_shape1959(shapes[i][0],shapes[i][1],pattern);
    check1959(neon_blocks1959>0,"actual NEON four-lane execution");
    printf("{\"status\":\"passed\",\"assertions\":%llu,\"pixel_comparisons\":%llu,\"guide_float_bit_comparisons\":%llu,\"actual_neon_four_pixel_blocks\":%llu,\"arm_neon_actual_execution\":true,\"arm_neon_pixel_equivalence\":true,\"physical_android_tested\":false}\n",(unsigned long long)assertions1959,(unsigned long long)pixels1959,(unsigned long long)guide_bits1959,(unsigned long long)neon_blocks1959);return 0;
}
