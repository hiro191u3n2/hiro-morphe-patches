/* Real JNI range output, including halos and Bitmap-equivalent ARGB rows.
 * Include the existing published front harness unchanged, then add masks that
 * actually exercise the proposed protected-block short circuit. */
#define main original_front_main119
#include "FrontKernel19100.c"
#undef main

static void fixture119(int width,int height,int scene,jint *input,jint *policy) {
    for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
        int i=y*width+x;
        uint32_t r=random1997();
        int red=48+(int)(r%160),green=24+(int)((r>>8)%216),blue=12+(int)((r>>16)%232);
        int alpha=((scene==5&&i%17==0)||(scene==6&&x==width/2&&y%3==0))?0x70:0xff;
        input[i]=(jint)((uint32_t)alpha<<24|(uint32_t)red<<16|(uint32_t)green<<8|(uint32_t)blue);
        int budget=256,detail=0;
        switch(scene) {
            case 0:budget=0;break;
            case 1:detail=256;break;
            case 2:budget=((x/8+y/8)&1)?256:0;break;
            case 3:budget=(r&3)?0:256;detail=(r&8)?256:0;break;
            case 4:break;
            case 5:budget=(x+y)&1?-64:256;detail=(x+y)&2?512:0;break;
            case 6:budget=(x==width/2&&y==height/2)?256:0;break;
            case 7:detail=((x/4+y/4)&1)?256:0;break;
        }
        policy[2*i]=budget;policy[2*i+1]=detail;
    }
}

int main(int argc,char **argv) {
    check1997(argc==2,"targeted proof output path required");
    struct JNINativeInterface_ table={0};table.GetArrayLength=length1997;table.IsSameObject=same1997;
    table.GetIntArrayElements=ints1997;table.GetFloatArrayElements=floats1997;
    table.ReleaseIntArrayElements=releaseints1997;table.ReleaseFloatArrayElements=releasefloats1997;
    table.ExceptionCheck=exception1997;table.SetIntArrayRegion=put1997;JNIEnv env=&table;
    FILE *f=fopen(argv[1],"wb");check1997(f!=NULL,"targeted proof file");
    const int widths[]={8,17,31,67,128},heights[]={8,19,49,83,129};
    unsigned cases=0;uint64_t protected_pixels=0,total_pixels=0;
    for(int shape=0;shape<5;shape++)for(int scene=0;scene<8;scene++) {
        int width=widths[shape],height=heights[shape],n=width*height,count;
        jint *input=malloc((size_t)n*4),*original=malloc((size_t)n*4);
        jint *output=malloc((size_t)n*4),*policy=malloc((size_t)n*8);
        check1997(input&&original&&output&&policy,"targeted fixture buffers");
        fixture119(width,height,scene,input,policy);memcpy(original,input,(size_t)n*4);
        float *data=model19100(width,height,3,&count);int cells=modelColumns19100*modelRows19100;
        for(int i=0;i<cells;i++){data[i]=4.f;data[cells+i]=5.f;data[2*cells+i]=128.f;}
        for(int i=3*cells;i<3*cells+16;i++)data[i]=4.f;
        for(int i=3*cells+16;i<count;i++)data[i]=1.f;
        Array1997 model={count,data};
        for(int colour=0;colour<2;colour++)for(int shadows=0;shadows<2;shadows++)for(int noise=1;noise<=4;noise++) {
            int core=shape&1?16:64;
            run1997(&env,input,output,width,height,core,noise,shadows,colour,&model,policy);
            write19100(f,output,(size_t)n*4);cases++;total_pixels+=n;
            check1997(memcmp(input,original,(size_t)n*4)==0,"targeted JNI source immutable");
        }
        for(int i=0;i<n;i++)if(policy[2*i]<=0||policy[2*i+1]>=256)protected_pixels++;
        free(input);free(original);free(output);free(policy);free(data);
    }
    fclose(f);
    printf("{\"status\":\"passed\",\"cases\":%u,\"pixels\":%llu,\"protected_input_pixels\":%llu,\"skipped_blocks\":%llu}\n",
        cases,(unsigned long long)total_pixels,(unsigned long long)protected_pixels,
#ifdef FRONT_SKIP_CANDIDATE
        (unsigned long long)front_skip119_count
#else
        0ULL
#endif
    );
    return 0;
}
