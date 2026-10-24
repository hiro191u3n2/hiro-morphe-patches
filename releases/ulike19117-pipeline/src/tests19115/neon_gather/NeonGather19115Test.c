/* Keep the complete existing independent scalar differential unchanged. */
#define main original_arm_suite19115
#include "../../tests1959/native-arm1959.c"
#undef main
static uint64_t gather_bits19115,interior19115,edge19115,uncached19115;
static void gather_case19115(int width,int height,int first,int finish) {
    Region r={0};r.width=width;r.height=height;r.first=first;r.finish=finish;
    r.pixels=malloc((size_t)width*(finish-first)*sizeof(jint));
    r.colors=malloc((size_t)width*16*sizeof(Color));
    check1959(r.pixels&&r.colors,"focused gather allocation");
    uint32_t random=0x19581915u;
    for(int y=first;y<finish;y++)for(int x=0;x<width;x++){
        uint32_t bits=random1959(&random);int alpha=(x+y)%5==0?(x*71+y*13)&255:255;
        r.pixels[(size_t)(y-first)*width+x]=(jint)((bits&0xffffffu)|((uint32_t)alpha<<24));
    }
    for(int mode=0;mode<2;mode++)for(int y=first;y<finish;y++){
        for(int k=0;k<16;k++)r.color_rows[k]=-1;
        if(mode==0)cache_rows(&r,y,0,first,finish);
        for(int x=-4;x<width+4;x++){
            VC1959 actual=vgather1959(&r,x,y);float a[3][4];uint32_t opacity[4];
            vst1q_f32(a[0],actual.y);vst1q_f32(a[1],actual.r);vst1q_f32(a[2],actual.b);vst1q_u32(opacity,actual.opaque);
            for(int lane=0;lane<4;lane++){
                Color expected=color(&r,x+lane,y);float gold[3]={expected.y,expected.r,expected.b};
                for(int channel=0;channel<3;channel++){check1959(memcmp(&a[channel][lane],&gold[channel],sizeof(float))==0,"cached gather raw float bits");gather_bits19115++;}
                check1959(opacity[lane]==((pixel(&r,x+lane,y)>>24)==255?UINT32_MAX:0),"cached gather exact alpha mask");gather_bits19115++;
            }
            if(mode)uncached19115++;else if(x>=0&&x<=width-4)interior19115++;else edge19115++;
        }
    }
    free(r.pixels);free(r.colors);
}
int main(void){
    int status=original_arm_suite19115();if(status)return status;
    int cases[][4]={{1,1,0,1},{3,5,0,5},{4,9,0,9},{5,17,0,17},{17,35,3,32},{3060,43,11,31},{4080,44,12,32}};
    for(size_t i=0;i<sizeof(cases)/sizeof(cases[0]);i++)gather_case19115(cases[i][0],cases[i][1],cases[i][2],cases[i][3]);
    check1959(interior19115>0&&edge19115>0&&uncached19115>0,"focused cached interior, border and uncached paths");
    printf("GATHER_RESULT {\"status\":\"passed\",\"assertions\":%llu,\"pixel_comparisons\":%llu,\"guide_float_bit_comparisons\":%llu,\"gather_raw_word_comparisons\":%llu,\"interior_cases\":%llu,\"edge_cases\":%llu,\"uncached_cases\":%llu,\"physical_android_tested\":false}\n",(unsigned long long)assertions1959,(unsigned long long)pixels1959,(unsigned long long)guide_bits1959,(unsigned long long)gather_bits19115,(unsigned long long)interior19115,(unsigned long long)edge19115,(unsigned long long)uncached19115);
    return 0;
}
