#define _POSIX_C_SOURCE 200809L
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
int front_baseline19100(int,int,const int32_t*,int32_t*,const float*,const int32_t*);
int front_candidate19100(int,int,const int32_t*,int32_t*,const float*,const int32_t*);
static uint32_t state=1997143;
static uint32_t next(void){state^=state<<13;state^=state>>17;state^=state<<5;return state;}
static uint64_t nanos(void){struct timespec t;clock_gettime(CLOCK_PROCESS_CPUTIME_ID,&t);return (uint64_t)t.tv_sec*1000000000+t.tv_nsec;}
int main(void) {
    const int w=192,h=128,n=w*h;int32_t *input=malloc(n*4),*old=malloc(n*4),*actual=malloc(n*4),*policy=malloc(n*8);float model[115];
    if(!input||!old||!actual||!policy)return 2;
    for(int i=0;i<n;i++){uint32_t r=next(),v=60+r%107;input[i]=(int32_t)(0xff000000u|v<<16|v<<8|(r%256));policy[i*2]=r%257;policy[i*2+1]=(r>>16)%257;}
    for(int i=0;i<9;i++){model[i]=(next()%4000)/333.f+.2f;model[9+i]=(next()%5000)/333.f+.2f;model[18+i]=(next()%25500)/100.f;}
    for(int i=27;i<115;i++)model[i]=(next()%1000)/333.f+.2f;
    uint64_t times[2][12];
    for(int trial=-2;trial<12;trial++)for(int turn=0;turn<2;turn++) {
        int which=(trial&1)?1-turn:turn;uint64_t start=nanos();
        for(int repeat=0;repeat<3;repeat++) {
            int okay=which?front_candidate19100(w,h,input,actual,model,policy):front_baseline19100(w,h,input,old,model,policy);
            if(!okay)return 3;
        }
        if(trial>=0)times[which][trial]=nanos()-start;
        if(turn==1&&memcmp(old,actual,n*4)){fputs("benchmark output mismatch\n",stderr);return 4;}
    }
    printf("{\"status\":\"passed\",\"same_process\":true,\"benchmark_argb_exact\":true,\"pixels_per_sample\":%d,\"samples_ns\":{",n*3);
    for(int choice=0;choice<2;choice++) {
        printf("%s\"%s\":[",choice?",":"",choice?"candidate":"baseline");
        for(int trial=0;trial<12;trial++){printf("%s%llu",trial?",":"",(unsigned long long)times[choice][trial]);}
        printf("]");
    }
    puts("}}");free(input);free(old);free(actual);free(policy);return 0;
}
