package com.hiro.ulike;

/** Pure, bounded masks from detected eye anchors. Not a semantic segmentation model. */
public final class FaceRegions1934Pixels {
    private FaceRegions1934Pixels() { }
    public static final class Anchor {
        final float x,y,eyes,roll,yaw,confidence;
        public Anchor(float x,float y,float eyes,float roll,float yaw,float confidence) {
            this.x=x;this.y=y;this.eyes=eyes;this.roll=roll;this.yaw=yaw;this.confidence=confidence;
        }
    }
    public static final class Raster {
        final byte[] skin,detail;
        public final boolean reliable;
        Raster(byte[] skin,byte[] detail,boolean reliable){this.skin=skin;this.detail=detail;this.reliable=reliable;}
    }
    public static Raster build(int[] pixels,int width,int height,Anchor[] faces) {
        if(width<2||height<2||width>640||height>640||pixels==null||pixels.length!=(long)width*height||
                faces==null||faces.length==0||faces.length>8||!GpuProtection1961.available())
            return buildCpu1961(pixels,width,height,faces);
        if(!GpuProtection1961.mayTryFace(width,height,acceptedCount1961(width,height,faces)))
            return buildCpu1961(pixels,width,height,faces);
        try {
            int[][] geometry=geometry1961(width,height,faces);
            if(geometry.length==0)return new Raster(new byte[pixels.length],new byte[pixels.length],false);
            int[] exact=GpuProtection1961.faceRaster(pixels,width,height,geometry);
            if(exact!=null) {
                byte[] skin=new byte[pixels.length],detail=new byte[pixels.length];
                for(int i=0;i<pixels.length;i++){skin[i]=(byte)exact[i*2];detail[i]=(byte)exact[i*2+1];}
                return new Raster(skin,detail,true);
            }
        } catch(RuntimeException optionalFailure) {
            if(Thread.currentThread().isInterrupted())throw new IllegalStateException("face protection interrupted");
        } catch(LinkageError optionalFailure) {} catch(OutOfMemoryError optionalFailure) {}
        return buildCpu1961(pixels,width,height,faces);
    }
    /** Independent published CPU mask remains the fallback and test authority. */
    static Raster buildCpu1961(int[] pixels,int width,int height,Anchor[] faces) {
        if(width<2||height<2||width>640||height>640||pixels==null||pixels.length!=(long)width*height)
            return new Raster(null,null,false);
        byte[] skin=new byte[pixels.length],detail=new byte[pixels.length];
        if(faces==null||faces.length==0||faces.length>8) return new Raster(skin,detail,false);
        int accepted=0;
        for(Anchor face:faces) {
            if(!valid(face,width,height)) continue;
            // Overlapping uncertain detections never get a skin-prior mask.
            boolean overlap=false;
            for(Anchor other:faces) if(other!=face&&valid(other,width,height)) {
                double distance=Math.hypot(face.x-other.x,face.y-other.y);
                if(distance<(face.eyes+other.eyes)*1.05){overlap=true;break;}
            }
            if(overlap) continue;
            accepted++;
            double angle=face.roll*Math.PI/180.0,co=Math.cos(angle),si=Math.sin(angle),ed=face.eyes;
            int x0=Math.max(0,(int)(face.x-1.55*ed)),x1=Math.min(width-1,(int)(face.x+1.55*ed));
            int y0=Math.max(0,(int)(face.y-1.1*ed)),y1=Math.min(height-1,(int)(face.y+1.95*ed));
            for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++) {
                double dx=(x-face.x)/ed,dy=(y-face.y)/ed;
                double fx=co*dx+si*dy,fy=-si*dx+co*dy;
                // Smooth interiors rather than an inflated generic face box.
                double cheeks=Math.max(ellipse(fx,fy,-.49,.59,.35,.43),ellipse(fx,fy,.49,.59,.35,.43));
                double forehead=ellipse(fx,fy,0,-.42,.46,.24);
                int eligibility=(int)Math.round(Math.max(cheeks,forehead)*255);
                double eyes=Math.max(ellipse(fx,fy,-.5,0,.37,.28),ellipse(fx,fy,.5,0,.37,.28));
                double brows=Math.max(ellipse(fx,fy,-.5,-.25,.42,.17),ellipse(fx,fy,.5,-.25,.42,.17));
                double mouth=ellipse(fx,fy,0,1.0,.49,.36);
                // Do not blur the face outline/hairline. This is a margin, NOT hair classification.
                double outline=Math.sqrt(fx*fx/(.98*.98)+(fy-.30)*(fy-.30)/(1.24*1.24));
                double margin=outline>.82&&outline<1.12?Math.max(0,1-Math.abs(outline-.97)/.15):0;
                int protect=(int)Math.round(Math.max(Math.max(eyes,brows),Math.max(mouth,margin))*255);
                int i=y*width+x,p=pixels[i];
                int edge=localRange(pixels,width,height,x,y);
                // Pixel-based veto protects dark fringe, dense fine detail, and non-skin colors.
                // It only REDUCES a geometrically detected face region; cannot promote background.
                if(!skinCompatible(p)) eligibility=0;
                else eligibility=eligibility*Math.max(0,48-edge)/48;
                eligibility=eligibility*(255-protect)/255;
                skin[i]=(byte)Math.max(skin[i]&255,eligibility);
                detail[i]=(byte)Math.max(detail[i]&255,protect);
            }
        }
        return new Raster(skin,detail,accepted>0);
    }
    private static int acceptedCount1961(int width,int height,Anchor[] faces) {
        int accepted=0;
        for(Anchor face:faces) {
            if(!valid(face,width,height))continue;
            boolean overlap=false;
            for(Anchor other:faces)if(other!=face&&valid(other,width,height)) {
                double distance=Math.hypot(face.x-other.x,face.y-other.y);
                if(distance<(face.eyes+other.eyes)*1.05){overlap=true;break;}
            }
            if(!overlap)accepted++;
        }
        return accepted;
    }
    /** Preserve every original double expression and visited rectangle. GPU
     * receives only already-rounded byte eligibility/protection, not geometry. */
    static int[][] geometry1961(int width,int height,Anchor[] faces) {
        java.util.ArrayList<int[]> planes=new java.util.ArrayList<int[]>();
        for(Anchor face:faces) {
            if(!valid(face,width,height))continue;
            boolean overlap=false;
            for(Anchor other:faces)if(other!=face&&valid(other,width,height)) {
                double distance=Math.hypot(face.x-other.x,face.y-other.y);
                if(distance<(face.eyes+other.eyes)*1.05){overlap=true;break;}
            }
            if(overlap)continue;
            int[] plane=new int[Math.multiplyExact(width,height)];
            double angle=face.roll*Math.PI/180.0,co=Math.cos(angle),si=Math.sin(angle),ed=face.eyes;
            int x0=Math.max(0,(int)(face.x-1.55*ed)),x1=Math.min(width-1,(int)(face.x+1.55*ed));
            int y0=Math.max(0,(int)(face.y-1.1*ed)),y1=Math.min(height-1,(int)(face.y+1.95*ed));
            for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++) {
                double dx=(x-face.x)/ed,dy=(y-face.y)/ed;
                double fx=co*dx+si*dy,fy=-si*dx+co*dy;
                double cheeks=Math.max(ellipse(fx,fy,-.49,.59,.35,.43),ellipse(fx,fy,.49,.59,.35,.43));
                double forehead=ellipse(fx,fy,0,-.42,.46,.24);
                int eligibility=(int)Math.round(Math.max(cheeks,forehead)*255);
                double eyes=Math.max(ellipse(fx,fy,-.5,0,.37,.28),ellipse(fx,fy,.5,0,.37,.28));
                double brows=Math.max(ellipse(fx,fy,-.5,-.25,.42,.17),ellipse(fx,fy,.5,-.25,.42,.17));
                double mouth=ellipse(fx,fy,0,1.0,.49,.36);
                double outline=Math.sqrt(fx*fx/(.98*.98)+(fy-.30)*(fy-.30)/(1.24*1.24));
                double margin=outline>.82&&outline<1.12?Math.max(0,1-Math.abs(outline-.97)/.15):0;
                int protect=(int)Math.round(Math.max(Math.max(eyes,brows),Math.max(mouth,margin))*255);
                plane[y*width+x]=65536|eligibility|(protect<<8);
            }
            planes.add(plane);
        }
        return planes.toArray(new int[planes.size()][]);
    }
    private static boolean valid(Anchor f,int w,int h) {
        return f!=null&&f.confidence>=.60f&&f.confidence<=1&&f.eyes>=16&&f.eyes<=Math.min(w,h)*.48f
            &&Math.abs(f.roll)<=25&&Math.abs(f.yaw)<=20&&f.x>=f.eyes&&f.x<w-f.eyes
            &&f.y>=f.eyes*.65f&&f.y<h-f.eyes*1.42f;
    }
    /** Cubic feathering suppresses visible changes at the conservative mask edge. */
    private static double ellipse(double x,double y,double cx,double cy,double rx,double ry) {
        double dx=(x-cx)/rx,dy=(y-cy)/ry,r=dx*dx+dy*dy;
        if(r>=1)return 0;
        double t=Math.min(1,(1-r)*3);return t*t*(3-2*t);
    }
    private static boolean skinCompatible(int p) {
        int r=(p>>>16)&255,g=(p>>>8)&255,b=p&255;
        int l=(77*r+150*g+29*b+128)>>8;
        // Broad chromatic veto, not a skin detector: works across a wide luminance range.
        return l>=28&&r>=g-10&&r>b+2&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))>6;
    }
    private static int localRange(int[]p,int w,int h,int x,int y) {
        int lo=255,hi=0;
        for(int yy=Math.max(0,y-1);yy<=Math.min(h-1,y+1);yy++)for(int xx=Math.max(0,x-1);xx<=Math.min(w-1,x+1);xx++) {
            int c=p[yy*w+xx],l=(77*((c>>>16)&255)+150*((c>>>8)&255)+29*(c&255)+128)>>8;
            lo=Math.min(lo,l);hi=Math.max(hi,l);
        }return hi-lo;
    }
}
