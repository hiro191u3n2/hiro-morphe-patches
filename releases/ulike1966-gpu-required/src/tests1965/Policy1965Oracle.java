package com.hiro.ulike;
import java.io.*;
import java.util.*;
/** Offline frozen CPU oracle. Never packaged or invoked by the capture path. */
public final class Policy1965Oracle {
    static DataOutputStream out;
    static final double[] TRIG={-1.0/121645100408832000.0,1.0/355687428096000.0,-1.0/1307674368000.0,1.0/6227020800.0,-1.0/39916800.0,1.0/362880.0,-1.0/5040.0,1.0/120.0,-1.0/6.0,1.0/2432902008176640000.0,-1.0/6402373705728000.0,1.0/20922789888000.0,-1.0/87178291200.0,1.0/479001600.0,-1.0/3628800.0,1.0/40320.0,-1.0/720.0,1.0/24.0,-1.0/2.0};
    static void ints(int[] a)throws Exception {out.writeInt(a.length);for(int v:a)out.writeInt(v);}
    static int[] floatWords(float[] a){int[] b=new int[a.length];for(int i=0;i<a.length;i++)b[i]=Float.floatToRawIntBits(a[i]);return b;}
    static int[] doubleWords(double[] a){int[] b=new int[a.length*2];for(int i=0;i<a.length;i++){long v=Double.doubleToRawLongBits(a[i]);b[i*2]=(int)v;b[i*2+1]=(int)(v>>>32);}return b;}
    static void emit(String name,int[] u,double[] m,int[] src,int[] raster,float[] grid,int[] smooth,int[] expected,int[] sigma)throws Exception {
        out.writeUTF(name);ints(u);ints(doubleWords(m));ints(src);ints(raster);ints(floatWords(grid));ints(smooth);ints(expected);ints(sigma);
    }
    static void faces()throws Exception {
        Random random=new Random(196501);
        for(int phase=0;phase<24;phase++) {
            int w=193,h=177;int[] pixels=new int[w*h];
            for(int i=0;i<pixels.length;i++)pixels[i]=phase%3==0?0xff98785f+random.nextInt(5):phase%3==1?random.nextInt()|0xff000000:0xff1c1c1c+(i%9)*0x010101;
            ReferenceFacePixels1960.Anchor[] anchors={new ReferenceFacePixels1960.Anchor(76.3f,72.1f,31.7f,phase%4==0?0:phase%4==1?25:phase%4==2?-25:(random.nextFloat()*50-25),0,.60f),new ReferenceFacePixels1960.Anchor(151.6f,68.9f,22.3f,-7.3f,20,.91f),phase%5==0?new ReferenceFacePixels1960.Anchor(79f,72f,30f,0,0,.8f):null};
            ReferenceFacePixels1960.Raster r=ReferenceFacePixels1960.build(pixels,w,h,anchors);
            int[] expected=new int[pixels.length*2+1];for(int i=0;i<pixels.length;i++){expected[i*2]=r.skin[i]&255;expected[i*2+1]=r.detail[i]&255;}expected[expected.length-1]=r.reliable?1:0;
            double[] m=new double[128];System.arraycopy(TRIG,0,m,64,TRIG.length);for(int i=0;i<anchors.length;i++){int p=i*6;ReferenceFacePixels1960.Anchor a=anchors[i];if(a==null){m[p+5]=-1;continue;}m[p]=a.x;m[p+1]=a.y;m[p+2]=a.eyes;m[p+3]=a.roll;m[p+4]=a.yaw;m[p+5]=a.confidence;}
            int[] u=new int[32];u[0]=0;u[1]=w;u[2]=h;u[23]=10;u[24]=8;u[29]=anchors.length;u[30]=pixels.length;
            emit("face"+phase,u,m,pixels,new int[]{0},new float[]{0},new int[]{0},expected,new int[0]);
        }
    }
    static float sigma(int x,int y,float[] g) {
        float gx=Math.max(0,Math.min(1,(x-2.3f)/53.7f)),gy=Math.max(0,Math.min(2,(y-1.7f)/29.8f));
        int ix=(int)gx,iy=(int)gy,nx=Math.min(1,ix+1),ny=Math.min(2,iy+1);gx-=ix;gy-=iy;
        float a=g[iy*2+ix]*(1-gx)+g[iy*2+nx]*gx,b=g[ny*2+ix]*(1-gx)+g[ny*2+nx]*gx;return a*(1-gy)+b*gy;
    }
    static int smooth(int x,int y,int turn,int ow,int oh,int sw,int sh,int[] cells) {
        double xx=x,yy=y;
        if(turn!=0||ow!=sw||oh!=sh){int rw=turn==90||turn==270?sh:sw,rh=turn==90||turn==270?sw:sh;
            double scale=Math.max((double)ow/rw,(double)oh/rh),cw=Math.min((double)rw,ow/scale),ch=Math.min((double)rh,oh/scale);
            double left=Math.max(0,(rw-cw)*.5),top=Math.max(0,(rh-ch)*.5),rx=left+(x+.5)*cw/ow-.5,ry=top+(y+.5)*ch/oh-.5;
            if(turn==90){xx=ry;yy=sh-1-rx;}else if(turn==180){xx=sw-1-rx;yy=sh-1-ry;}else if(turn==270){xx=sw-1-ry;yy=rx;}else{xx=rx;yy=ry;}}
        if(xx<0||yy<0||xx>sw-1||yy>sh-1)return 0;
        int cols=(sw+3)/4,rows=(sh+3)/4;double gx=Math.max(0,Math.min(cols-1,(xx-1.5)/4)),gy=Math.max(0,Math.min(rows-1,(yy-1.5)/4));
        int ix=(int)gx,iy=(int)gy,nx=Math.min(cols-1,ix+1),ny=Math.min(rows-1,iy+1);double fx=gx-ix,fy=gy-iy;
        double a=cells[iy*cols+ix]*(1-fx)+cells[iy*cols+nx]*fx,b=cells[ny*cols+ix]*(1-fx)+cells[ny*cols+nx]*fx;return (int)Math.round(a*(1-fy)+b*fy);
    }
    static void policies()throws Exception {
        Random random=new Random(196502);
        for(int phase=0;phase<32;phase++) {
            int sw=63,sh=59,rw=17,rh=13,turn=(phase%4)*90,ow=phase%2==0?53:97,oh=phase%2==0?47:71;
            byte[] skin=new byte[rw*rh],detail=new byte[rw*rh];random.nextBytes(skin);random.nextBytes(detail);
            FaceRegions1934.Mask mask=FaceRegions1934.uprightRaster(sw,sh,rw,rh,skin,detail,turn);
            if(phase>=28)mask=mask.resample(turn,91,67);
            int resizeTurn=((phase/4)%4)*90;mask=mask.resample(resizeTurn,ow,oh);
            int[] raster=new int[skin.length*2];for(int i=0;i<skin.length;i++){raster[i*2]=skin[i]&255;raster[i*2+1]=detail[i]&255;}
            double[] m=new double[64];m[6]=7.3f;m[7]=phase%3==0?.72f:phase%3==1?1f:1.43f;m[8]=2.3f;m[9]=1.7f;m[10]=53.7f;m[11]=29.8f;m[12]=resizeTurn;m[13]=ow;m[14]=oh;
            int first=phase%3==1?7:0,rows=oh-first;
            int[] u=new int[32];u[0]=phase<16?1:2;u[1]=ow;u[2]=rows;u[3]=first;u[4]=sw;u[5]=sh;u[6]=rw;u[7]=rh;u[8]=1;u[9]=2;u[10]=3;u[11]=1;u[12]=192;u[13]=180;u[14]=phase%2;u[15]=1;u[16]=991;u[17]=phase%2;u[27]=turn;u[28]=1;u[30]=ow*rows;
            if(phase>=28){m[12]=turn;m[13]=91;m[14]=67;m[15]=resizeTurn;m[16]=ow;m[17]=oh;u[28]=2;}
            int[] cells=new int[((sw+3)/4)*((sh+3)/4)];for(int i=0;i<cells.length;i++)cells[i]=random.nextInt(256);
            u[18]=sw;u[19]=sh;u[20]=(sw+3)/4;u[21]=(sh+3)/4;u[22]=resizeTurn;u[23]=ow;u[24]=oh;u[25]=1;
            float[] grid={.31f,1.234f,7.8f,13.4f,24.12f,31.27f};int[] expected=new int[ow*rows*(u[0]==1?2:4)],sigmas=u[0]==1?new int[ow*rows]:new int[0];
            for(int y=first;y<oh;y++)for(int x=0;x<ow;x++) {
                int i=(y-first)*ow+x,s=mask.skinQ8(x,y),d=mask.detailQ8(x,y);float sigma=sigma(x,y,grid);
                if(u[0]==1){int b=256-((s*192)>>9);if(u[14]==0)b=180*b>>8;expected[i*2]=Math.max(0,Math.min(256,b));expected[i*2+1]=Math.max(0,Math.min(256,d));sigmas[i]=Float.floatToRawIntBits(sigma);}
                else {float scale=(float)m[7];if(!(u[17]!=0||Math.abs(scale-1f)<.00001f))sigma=(float)m[6];if(u[17]==0&&scale<1f)sigma*=(float)Math.sqrt(scale);
                    int c=smooth(x,y,resizeTurn,ow,oh,sw,sh,cells),t=3+Math.round(sigma*.65f);t+=Math.max(2,Math.round(Math.min(32f,(float)m[6])*.85f))*c>>8;
                    int floor=Math.max(u[16],Math.round((1f+sigma*1.8f)*256f)),a=Math.max(floor,Math.round((2f+Math.min(32f,(float)m[6])*2.4f)*256f));floor+=((a-floor)*c)>>8;
                    expected[i*4]=t;expected[i*4+1]=Math.max(3,Math.round(sigma*.75f));expected[i*4+2]=floor;expected[i*4+3]=256-(s*(20+(192>>3))>>8);}
            }
            emit("policy"+phase,u,m,new int[]{0},raster,grid,cells,expected,sigmas);
        }
    }
    static void cells()throws Exception {
        Random random=new Random(196503);
        for(int width:new int[]{1,3,4,5,17,65})for(int owned:new int[]{1,4,7}) {
            int begin=4,end=begin+owned,rows=end+2,origin=4,height=end+origin,columns=(width+3)/4,cells=columns*((owned+3)/4);
            int[] pixels=new int[width*rows],policy=new int[width*owned*2],confidence=new int[cells],expected=new int[cells];
            for(int i=0;i<pixels.length;i++)pixels[i]=i%19==0?0:0xff98785f;
            for(int i=0;i<policy.length;i++)policy[i]=random.nextInt(257);
            for(int i=0;i<cells;i++)confidence[i]=random.nextInt(257);
            // Frozen published GpuProtection1961.smoothReference expression.
            for(int row=begin;row<end;row+=4)for(int x=0;x<width;x+=4) {
                int minimum=256,y=row+origin;
                for(int dy=0;dy<4&&y+dy<height;dy++)for(int dx=0;dx<4&&x+dx<width;dx++) {
                    int r=row+dy,c=x+dx;if(r>=rows||(pixels[r*width+c]>>>24)!=255){minimum=0;continue;}
                    int at=((r-begin)*width+c)*2;minimum=Math.min(minimum,policy[at]*(256-policy[at+1])>>8);
                }
                int i=((row-begin)/4)*columns+x/4;expected[i]=Math.min(255,confidence[i]*minimum>>8);
            }
            int[] u=new int[32];u[0]=3;u[1]=width;u[2]=rows;u[3]=begin;u[4]=end;u[5]=origin;u[6]=height;u[7]=columns;u[30]=cells;
            emit("cells"+width+"x"+owned,u,new double[64],pixels,policy,new float[]{0},confidence,expected,new int[0]);
        }
    }
    static void roundingEdges()throws Exception {
        java.lang.reflect.Constructor<FaceRegions1934.Mask> constructor=FaceRegions1934.Mask.class.getDeclaredConstructor(int.class,int.class,int.class,int.class,byte[].class,byte[].class,double.class,double.class,double.class,double.class,double.class,double.class,boolean.class);
        constructor.setAccessible(true);
        double center=.5*255.0/256.0;
        for(double px:new double[]{Math.nextDown(center),center,Math.nextUp(center)}) {
            byte[] skin={0,0,0,0},detail={0,1,0,1};
            FaceRegions1934.Mask mask=constructor.newInstance(1,1,2,2,skin,detail,0.0,0.0,px,0.0,0.0,0.0,true);
            double[] m=new double[64];m[2]=px;m[7]=1.0;
            int[] u=new int[32];u[0]=1;u[1]=u[2]=u[4]=u[5]=1;u[6]=u[7]=2;u[8]=1;u[14]=1;u[26]=1;u[30]=1;
            emit("rounding"+Double.toHexString(px),u,m,new int[]{0},new int[]{0,0,0,1,0,0,0,1},new float[]{0},new int[]{0},new int[]{256,mask.detailQ8(0,0)},new int[]{0});
        }
    }
    public static void main(String[] args)throws Exception {out=new DataOutputStream(new FileOutputStream(args[0]));out.writeInt(1965001);faces();policies();cells();roundingEdges();out.writeUTF("");out.close();}
}
