package com.hiro.ulike;

/** Conservative 6/8/12/16-pixel chroma-wave detector. Requires repeat on both sides
 * over two cycles, opposite phase agreement, a smooth wave and no matching luma
 * pattern. Strongly coloured motifs and isolated colour boundaries are retained.
 * An RGB image alone cannot unambiguously identify every isoluminant true motif. */
public final class LongMoire1934 {
    private static final int[] PERIODS={6,8,12,16};
    private LongMoire1934() {}
    static int u(int p){return (p&255)-((p>>>8)&255);}
    static int v(int p){return ((p>>>16)&255)-((p>>>8)&255);}
    static int y(int p){return QualityPixels1932.luma(p);}
    static int distance(int au,int av,int bu,int bv){return Math.max(Math.abs(au-bu),Math.abs(av-bv));}
    static int correct(int[] src,int at,int width,int center) {
        int cu=u(center),cv=v(center),cy=y(center);
        if(Math.max(Math.abs(cu),Math.abs(cv))>90)return center;
        int best=0,bestU=cu,bestV=cv;
        for(int direction=0;direction<4;direction++) {
            int step=direction==0?1:direction==1?width:direction==2?width+1:width-1;
            int closeM=src[at-step],closeP=src[at+step];
            if((closeM>>>24)!=255||(closeP>>>24)!=255)continue;
            // Fine hard colour stripes are plausibly a real printed motif, not a long wave.
            if(distance(u(closeM),v(closeM),u(closeP),v(closeP))>46)continue;
            for(int period:PERIODS) {
                int half=period/2;
                int m=src[at-half*step],p=src[at+half*step];
                if((m>>>24)!=255||(p>>>24)!=255)continue;
                int mu=u(m),mv=v(m),pu=u(p),pv=v(p);
                int amplitude=distance(cu*2,cv*2,mu+pu,mv+pv)/2;
                if(amplitude<10||amplitude>96)continue;
                int opposition=distance(mu,mv,pu,pv);
                if(opposition>3+amplitude/6)continue;
                int lumaPattern=Math.max(Math.abs(cy-y(m)),Math.abs(cy-y(p)));
                if(lumaPattern>2+amplitude/10)continue;
                int repeat=0;
                boolean valid=true;
                for(int sign=-1;sign<=1;sign+=2)for(int cycle=1;cycle<=2;cycle++) {
                    int q=src[at+sign*period*cycle*step];
                    if((q>>>24)!=255){valid=false;break;}
                    repeat=Math.max(repeat,distance(cu,cv,u(q),v(q)));
                    if(Math.abs(cy-y(q))>3+amplitude/10)valid=false;
                }
                if(!valid||repeat>2+amplitude/6)continue;
                int quarter=Math.max(1,period/4);
                int qm=src[at-quarter*step],qp=src[at+quarter*step];
                if((qm>>>24)!=255||(qp>>>24)!=255)continue;
                int meanU=(2*cu+mu+pu)/4,meanV=(2*cv+mv+pv)/4;
                // Do not broadly remove warm-coloured textiles, skin or coloured lighting.
                if(Math.max(Math.abs(meanU),Math.abs(meanV))>36)continue;
                // Quarter phases of a smooth wave average near its mean. Flat plateaus
                // and square-wave motifs fail this condition even with constant luma.
                int midpointError=distance(u(qm)+u(qp),v(qm)+v(qp),meanU*2,meanV*2)/2;
                if(midpointError>4+amplitude/4)continue;
                int nearChange=distance(u(closeM)+u(closeP),v(closeM)+v(closeP),cu*2,cv*2)/2;
                if(nearChange<1 || nearChange>3+amplitude*14/(period*period))continue;
                // Preserve real repeating luminance texture even at intermediate phases.
                int lumaRange=Math.max(Math.max(y(qm),y(qp)),cy)-Math.min(Math.min(y(qm),y(qp)),cy);
                if(lumaRange>3+amplitude/10)continue;
                int amount=Math.min(176,(amplitude-7)*8);
                amount=amount*(amplitude+2-repeat)/(amplitude+2);
                if(amount>best){best=amount;bestU=meanU;bestV=meanV;}
            }
        }
        if(best==0)return center;
        int ou=cu+round((bestU-cu)*best,256),ov=cv+round((bestV-cv)*best,256);
        int g=round(256*cy-77*ov-29*ou,256),r=g+ov,b=g+ou;
        if(r<0||r>255||g<0||g>255||b<0||b>255)return center;
        return (center&0xff000000)|(r<<16)|(g<<8)|b;
    }
    static int round(int n,int d){return n>=0?(n+d/2)/d:-((-n+d/2)/d);}
}
