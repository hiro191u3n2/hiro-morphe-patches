package com.hiro.ulike;

/** Exclusive, processed-domain row ring; no snapshot or thread-local image state.
 * Every borrowed row is overwritten before read. The exact luminance and two
 * signed chroma differences fit in one int; opaque remains an explicit bit.
 */
final class NoiseCache1944 implements AutoCloseable {
    static final int OPAQUE=1<<26;
    private final int[] source;
    final int width,radius,taps,slots;
    final int[] dx,spatial,ys,ringBase,nearRows;
    final int[] packed,xs;
    private final int[] tags;
    int centerOffset;
    private boolean closed;

    NoiseCache1944(int[] source,int width,int radius) {
        this.source=source;this.width=width;this.radius=radius;
        this.slots=radius*2+1;
        dx=radius==4?new int[]{-4,-2,-1,0,1,2,4}:new int[slots];
        if(radius!=4)for(int i=0;i<slots;i++)dx[i]=i-radius;
        taps=dx.length;
        spatial=radius==4?new int[]{1,3,5,6,5,3,1}:new int[taps];
        if(radius!=4)for(int i=0;i<taps;i++)spatial[i]=4-Math.abs(dx[i]);
        ys=new int[taps];ringBase=new int[taps];nearRows=new int[slots];tags=new int[slots];
        java.util.Arrays.fill(tags,-1);
        int[] first=SpeedWorkers1935.borrowInts(width*slots);
        try {xs=SpeedWorkers1935.borrowInts(width*taps);}
        catch(Throwable failure){SpeedWorkers1935.release(first);throw failure;}
        packed=first;
        for(int col=0;col<width;col++)for(int i=0;i<taps;i++)
            xs[col*taps+i]=Math.max(0,Math.min(width-1,col+dx[i]));
    }
    void prepare(int row,int lo,int hi) {
        for(int sy=Math.max(lo,row-radius);sy<=Math.min(hi-1,row+radius);sy++) {
            int slot=sy%slots;
            if(tags[slot]==sy)continue;
            int src=sy*width,dst=slot*width;
            for(int x=0;x<width;x++) {
                int p=source[src+x];
                if((p>>>24)!=255){packed[dst+x]=0;continue;}
                int r=(p>>>16)&255,g=(p>>>8)&255,b=p&255;
                packed[dst+x]=OPAQUE|((77*r+150*g+29*b+128)>>>8)|((r-g+255)<<8)|((b-g+255)<<17);
            }
            tags[slot]=sy;
        }
        for(int i=0;i<taps;i++) {
            ys[i]=Math.max(lo,Math.min(hi-1,row+dx[i]));
            ringBase[i]=(ys[i]%slots)*width;
        }
        centerOffset=(row%slots)*width;
        for(int i=0;i<slots;i++)nearRows[i]=(Math.max(lo,Math.min(hi-1,row+i-radius))%slots)*width;
    }
    int column(int col,int tap){return xs[col*taps+tap];}
    int value(int row,int x){return packed[(row%slots)*width+x];}
    int texture(int x,int row,int lo,int hi,int threshold,int[] samples) {
        int support=radius;
        if(support<3 || x<support || x>=width-support || row<lo+support || row>=hi-support)return 0;
        int best=0,length=support*2+1;
        for(int direction=0;direction<4;direction++) {
            int stepX=direction==1?0:direction==3?-1:1;
            int stepY=direction==0?0:1;
            boolean opaque=true;int min=255,max=0,adjacent=0;
            for(int i=0;i<length;i++) {
                int offset=i-support,p=packed[nearRows[offset*stepY+support]+x+offset*stepX];
                if((p&OPAQUE)==0){opaque=false;break;}
                int v=samples[i]=p&255;min=Math.min(min,v);max=Math.max(max,v);
                if(i>0)adjacent+=Math.abs(v-samples[i-1]);
            }
            if(!opaque || max-min<threshold*2 || adjacent<(length-1)*threshold)continue;
            int average=(adjacent+(length-2)/2)/(length-1);
            for(int period=2;period<=Math.min(4,length-4);period++) {
                int error=0,count=length-period;
                for(int i=0;i<count;i++)error+=Math.abs(samples[i]-samples[i+period]);
                error=(error+count/2)/count;
                int confidence=Math.max(0,Math.min(256,(average*4-error*9)*256/Math.max(1,average*4)));
                confidence=confidence*confidence>>8;best=Math.max(best,confidence);
            }
        }
        return best;
    }
    public void close() {
        if(closed)return;closed=true;
        SpeedWorkers1935.release(packed);SpeedWorkers1935.release(xs);
    }
}
