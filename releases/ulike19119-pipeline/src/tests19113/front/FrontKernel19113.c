/* Add independent safeguard probes to the existing real-JNI output fixture.
 * The identical fixture is compiled against frozen .112 and current source. */
#define main complete_pixels19100
#include "../../tests19100/front/FrontKernel19100.c"
#undef main
static int paired_proof19113(const char *path) {
    FILE *f=fopen(path,"wb");check1997(f!=NULL,"pair proof file");unsigned cases=0;
    const int widths[]={1,2,7,8,17,33,64,67}, heights[]={1,7,13,17,33,65,71,83};
    for(int shape=0;shape<8;shape++)for(int scene=0;scene<8;scene++) {
        int width=widths[shape],height=heights[shape],count=width*height,mc;
        jint *input=malloc((size_t)count*4),*policy=malloc((size_t)count*8);
        check1997(input&&policy,"pair input");inputs19100(width,height,scene%4,input,policy);
        if(scene==4)for(int i=0;i<count;i++)input[i]=(jint)0xff000000;
        if(scene==5)for(int i=0;i<count;i++)input[i]=(jint)0xffffffff;
        if(scene==6&&width>4&&height>4)input[4*width+4]&=0x7fffffff;
        if(scene==7)input[0]&=0x7fffffff;
        float *d=model19100(width,height,scene%4,&mc);Model m={0};
        m.width=width;m.height=height;m.columns=modelColumns19100;m.rows=modelRows19100;
        int cells=m.columns*m.rows;m.luma=d;m.chroma=d+cells;m.mean=d+2*cells;
        m.brightness_y=d+3*cells;m.brightness_c=m.brightness_y+BINS;m.ratios=m.brightness_c+BINS;
        m.left=(patchW19100-1)*.5f;m.top=(patchH19100-1)*.5f;
        m.step_x=m.columns==1?1:(float)(width-patchW19100)/(m.columns-1);
        m.step_y=m.rows==1?1:(float)(height-patchH19100)/(m.rows-1);
        for(int colour=0;colour<2;colour++)for(int edge=0;edge<4;edge++) {
            Workspace w={0};w.colour_enabled1976=colour;
            w.delta=calloc((size_t)count*3,sizeof(float));w.weight=calloc((size_t)count,sizeof(float));
            check1997(w.delta&&w.weight,"pair work");int bx=(edge&1)?width-1:0,by=(edge&2)?height-1:0;
            check1997(block(input,width,0,height,0,&m,policy,0,4,scene%2,bx,by,8,0,height,&w),"8x8 block");
            check1997(block(input,width,0,height,0,&m,policy,0,4,scene%2,bx,by,4,0,height,&w),"4x4 block");
            write19100(f,w.source,sizeof(w.source));write19100(f,w.coefficient,sizeof(w.coefficient));
            write19100(f,w.delta,(size_t)count*3*sizeof(float));write19100(f,w.weight,(size_t)count*sizeof(float));
            free(w.delta);free(w.weight);cases++;
        }
        free(input);free(policy);free(d);
    }
    fclose(f);printf("{\"status\":\"passed\",\"pair_cases\":%u}\n",cases);return 0;
}
int main(int argc, char **argv) {
    if(argc==3&&atoi(argv[2])==3)return paired_proof19113(argv[1]);
    if (argc != 3 || atoi(argv[2]) != 2) return complete_pixels19100(argc,argv);
    FILE *f=fopen(argv[1],"wb");check1997(f!=NULL,"safeguard proof file");
    unsigned cases=0;
    for (int n=4;n<=8;n+=4) for (int scene=0;scene<8;scene++) for(int trial=0;trial<2048;trial++) {
        double source[64],residual[64];
        for(int y=0;y<n;y++)for(int x=0;x<n;x++) {
            int i=y*n+x;double r=((int32_t)random1997())/2147483648.0;
            source[i]=scene==0?0:scene==1?255:scene==2?(x<n/2?30:190):scene==3?(y<n/2?30:190):128+r*120;
            residual[i]=scene==0?r*0.00001:scene==1?r*0.001:scene==2?(x%2?12:-12):scene==3?(y%2?12:-12):scene==4?(x%4<2?10:-10):scene==5?(y%4<2?10:-10):scene==6?r*0.01:r*20;
        }
        float sigma=(float)(random1997()%3000)/100.f;
        float value=residual_safeguard(source,residual,n,sigma);
        write19100(f,&value,sizeof(value));cases++;
    }
    fclose(f);printf("{\"status\":\"passed\",\"safeguard_cases\":%u}\n",cases);return 0;
}
