package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.Image;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

/** Independent layout/geometry oracles, not camera-hardware claims. */
public final class YuvGeometry1934Test {
    static int assertions,scenarios;
    static void check(boolean condition,String label){assertions++;if(!condition)throw new AssertionError(label);}
    static void same(byte[] a,byte[] b,String label){check(Arrays.equals(a,b),label);}
    static Image planeImage(int w,int h,int pixel,int padding,int position,boolean direct) {
        Image.Plane[] p=new Image.Plane[3];
        for(int channel=0;channel<3;channel++) {
            int width=channel==0?w:w/2,height=channel==0?h:h/2,step=channel==0?1:pixel;
            int row=width*step+padding;
            int size=position+(height-1)*row+(width-1)*step+1;
            ByteBuffer b=direct?ByteBuffer.allocateDirect(size):ByteBuffer.allocate(size);
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)b.put(position+y*row+x*step,(byte)value(channel,x,y));
            b.position(position);p[channel]=new Image.Plane(b.asReadOnlyBuffer(),row,step);
        }
        return new Image(w,h,35,1,p);
    }
    static int value(int plane,int x,int y){return (plane==0?29:plane==1?55:173)+x*3+y*7;}
    static byte[] oracle(int w,int h) {
        byte[] p=new byte[w*h*3/2];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)p[y*w+x]=(byte)value(0,x,y);
        for(int y=0;y<h/2;y++)for(int x=0;x<w/2;x++){
            p[w*h+y*w+2*x]=(byte)value(2,x,y);p[w*h+y*w+2*x+1]=(byte)value(1,x,y);
        }
        return p;
    }
    static void layout() {
        for(int pixel:new int[]{1,2,3})for(int padding:new int[]{0,7})for(int position:new int[]{0,5})for(boolean direct:new boolean[]{false,true}) {
            Image image=planeImage(10,6,pixel,padding,position,direct);
            byte[] actual=new byte[90],expected=oracle(10,6);
            check(YuvPlanes1934.copy(image,actual),"valid independent planes");same(actual,expected,"planar/interleaved/padded exact VU");
            same(YuvPlanes1934.owned(image),expected,"burst and ordinary packing identical");
            for(Image.Plane p:image.planes)check(p.getBuffer().position()==position,"borrowed buffer position preserved");
            check(image.closes==0,"borrowed image not closed");scenarios++;
        }
        // True shared NV21 and NV12 ByteBuffer views, each with its own position.
        for(boolean vu:new boolean[]{false,true}) {
            int w=8,h=4,base=3,row=12;ByteBuffer shared=ByteBuffer.allocate(base+row+(w-1)+1);
            for(int y=0;y<h/2;y++)for(int x=0;x<w/2;x++) {
                shared.put(base+y*row+2*x,(byte)value(vu?2:1,x,y));
                shared.put(base+y*row+2*x+1,(byte)value(vu?1:2,x,y));
            }
            ByteBuffer u=shared.duplicate(),v=shared.duplicate();u.position(base+(vu?1:0));v.position(base+(vu?0:1));
            Image image=planeImage(w,h,1,0,0,false);
            image.planes[1]=new Image.Plane(u,row,2);image.planes[2]=new Image.Plane(v,row,2);
            same(YuvPlanes1934.owned(image),oracle(w,h),"shared UV/NV21 shortened last row");
            check(u.position()==base+(vu?1:0)&&v.position()==base+(vu?0:1),"shared buffer positions preserved");scenarios++;
        }
    }
    static int clamp(int x){return Math.max(0,Math.min(255,x));}
    // Independent standard limited-range BT.601 display oracle, used only to
    // make a U/V mixup visible. Production preserves SDK's own colour matrix.
    static int rgb601(int y,int u,int v) {
        double l=(y-16)*255.0/219.0,cb=(u-128)*255.0/224.0,cr=(v-128)*255.0/224.0;
        return 0xff000000|clamp((int)Math.round(l+1.402*cr))<<16|clamp((int)Math.round(l-.344136*cb-.714136*cr))<<8|clamp((int)Math.round(l+1.772*cb));
    }
    static void colors() {
        int[][] colors={{16,128,128,0x000000},{235,128,128,0xffffff},{81,90,240,0xff0000},{145,54,34,0x00ff00},{41,240,110,0x0000ff},{126,104,149,0xa17e51}};
        for(int[] c:colors) {
            Image image=planeImage(2,2,1,0,0,false);
            byte[] ys={(byte)c[0],(byte)c[0],(byte)c[0],(byte)c[0]},us={(byte)c[1]},vs={(byte)c[2]};
            image.planes=new Image.Plane[]{new Image.Plane(ByteBuffer.wrap(ys),2,1),new Image.Plane(ByteBuffer.wrap(us),1,1),new Image.Plane(ByteBuffer.wrap(vs),1,1)};
            byte[] p=YuvPlanes1934.owned(image);int out=rgb601(p[0]&255,p[5]&255,p[4]&255);
            check(out==rgb601(c[0],c[1],c[2]),"packing preserves color-patch RGB oracle");
            if(c!=colors[5])for(int shift:new int[]{0,8,16})check(Math.abs(((out>>>shift)&255)-((c[3]>>>shift)&255))<=1,"primary/neutral color patch");
            scenarios++;
        }
    }
    static void rejected() {
        for(int mode=0;mode<8;mode++) {
            Image image=planeImage(8,4,1,0,0,false);byte[] output=new byte[48];Arrays.fill(output,(byte)77);
            if(mode==0)image.crop=new Rect(2,0,8,4);
            if(mode==1)image.planes[1].getBuffer().limit(1);
            if(mode==2)image.planes[2]=new Image.Plane(ByteBuffer.allocate(16),2,2);
            if(mode==3)image.planes[2]=new Image.Plane(ByteBuffer.allocate(16),0,1);
            if(mode==4)image.planes[1]=null;
            if(mode==5)image.planes=new Image.Plane[]{image.planes[0]};
            if(mode==6)image=new Image(7,4,35,1,image.planes);
            if(mode==7)image=new Image(8,4,256,1,image.planes);
            check(!YuvPlanes1934.copy(image,output),"invalid plane fails through stock false return");
            for(byte b:output)check(b==77,"invalid input validated before output mutation");
            check(image.closes==0,"invalid input does not transfer ownership");scenarios++;
        }
        check(!YuvPlanes1934.copy(null,new byte[20]),"null input");
        check(!YuvPlanes1934.copy(planeImage(8,4,1,0,0,false),new byte[47]),"short destination");
    }
    static int sourceIndex(int sw,int sh,int rotation,int x,int y) {
        if(rotation==90)return (sh-1-x)*sw+y;
        if(rotation==180)return (sh-1-y)*sw+sw-1-x;
        if(rotation==270)return x*sw+sw-1-y;
        return y*sw+x;
    }
    static void geometry() {
        for(int rotation:new int[]{0,90,180,270})for(int crop:new int[]{0,2,4}) {
            int sw=18,sh=12,rw=rotation==90||rotation==270?sh:sw,rh=rotation==90||rotation==270?sw:sh;
            // Leave one dimension unchanged, matching the production center-crop
            // scale policy without introducing an up/down-scale.
            int w=rw-crop,h=rh;
            int[] data=new int[sw*sh];Random random=new Random(19);
            for(int i=0;i<data.length;i++)data[i]=0xff000000|random.nextInt(0xffffff);
            Bitmap input=Bitmap.from(sw,sh,data),output=FastResize1933.resample(input,rotation,w,h);
            int[] result=output.pixels();
            for(int y=0;y<h;y++)for(int x=0;x<w;x++)check(result[y*w+x]==data[sourceIndex(sw,sh,rotation,x+crop/2,y)],"one-pass exact rotate+crop");
            check(Arrays.equals(input.pixels(),data),"geometry source unchanged");
            check(output.getWidth()==w&&output.getHeight()==h,"geometry exact dimensions");
            if(output!=input)output.recycle();input.recycle();scenarios++;
        }
        FastPixels1933.Plan integral=FastPixels1933.prepare(10,8,6,6,2,1,6,6);
        check(integral.exactCrop&&integral.horizontal==null&&integral.vertical==null,"no interpolation axes for integer crop");
        check(!FastPixels1933.prepare(10,8,6,6,1.5,1,6,6).exactCrop,"fractional crop keeps interpolation");
        check(!FastPixels1933.prepare(10,8,6,6,1,1,8,6).exactCrop,"actual resize keeps antialias filter");
    }
    public static void main(String[] args) {
        layout();colors();rejected();geometry();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+",\"device_tested\":false}");
    }
}
