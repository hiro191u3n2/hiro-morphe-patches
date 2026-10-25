/* Actual production safeguard calls. Checkerboard inputs require lag1 and
 * period-four checkerboards require lag4; disabling either must be detected. */
#include <stdio.h>
#include FRONT_KERNEL
int main(void) {
    unsigned tested=0;
    for (int n=4;n<=8;n+=4) for (int scene=0;scene<4;scene++) {
        double source[64],residual[64];
        for(int y=0;y<n;y++)for(int x=0;x<n;x++) {
            int i=y*n+x;source[i]=128;
            residual[i]=scene==0?(((x+y)&1)?12:-12):scene==1?(((x/2+y/2)&1)?12:-12):scene==2?0:0.000001;
        }
        float value=residual_safeguard(source,residual,n,4.f);uint32_t bits=0;memcpy(&bits,&value,sizeof(bits));
        printf("%d %d %08x\n",n,scene,bits);tested++;
    }
    return tested==8?0:1;
}
