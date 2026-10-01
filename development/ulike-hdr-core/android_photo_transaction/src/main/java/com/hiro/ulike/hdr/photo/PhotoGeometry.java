package com.hiro.ulike.hdr.photo;

/** Integer rotation/mirror/crop only. Both paired renditions use this exact mapping; no resizing. */
public final class PhotoGeometry {
    public final int sourceWidth,sourceHeight,rotationClockwise,left,top,width,height;
    public final boolean mirrorUpright;
    public PhotoGeometry(int sourceWidth,int sourceHeight,int rotationClockwise,boolean mirrorUpright,
            int left,int top,int width,int height) {
        if(sourceWidth<1 || sourceHeight<1 || (long)sourceWidth*sourceHeight>32_000_000
                || (rotationClockwise!=0 && rotationClockwise!=90 && rotationClockwise!=180 && rotationClockwise!=270))
            throw new IllegalArgumentException("bounded capture and right-angle rotation required");
        int uprightWidth=rotationClockwise%180==0?sourceWidth:sourceHeight;
        int uprightHeight=rotationClockwise%180==0?sourceHeight:sourceWidth;
        if(left<0 || top<0 || width<2 || height<2 || (width&1)!=0 || (height&1)!=0
                || (long)left+width>uprightWidth || (long)top+height>uprightHeight)
            throw new IllegalArgumentException("explicit even output crop within upright image required");
        this.sourceWidth=sourceWidth;this.sourceHeight=sourceHeight;this.rotationClockwise=rotationClockwise;
        this.mirrorUpright=mirrorUpright;this.left=left;this.top=top;this.width=width;this.height=height;
    }
    public void sourcePixel(int x,int y,int[] pair) {
        if(pair==null || pair.length<2) throw new IllegalArgumentException("XY destination");
        if(x<0 || y<0 || x>=width || y>=height) throw new IllegalArgumentException("output pixel outside crop");
        int u=x+left,v=y+top,uw=rotationClockwise%180==0?sourceWidth:sourceHeight;
        if(mirrorUpright) u=uw-1-u;
        switch(rotationClockwise) {
            case 0:pair[0]=u;pair[1]=v;break;
            case 90:pair[0]=v;pair[1]=sourceHeight-1-u;break;
            case 180:pair[0]=sourceWidth-1-u;pair[1]=sourceHeight-1-v;break;
            case 270:pair[0]=sourceWidth-1-v;pair[1]=u;break;
            default:throw new AssertionError();
        }
    }
    public String id() { return PhotoIdentity.hash(PhotoIdentity.fields("integer-upright-mirror-crop-v1",Integer.toString(sourceWidth),
            Integer.toString(sourceHeight),Integer.toString(rotationClockwise),Boolean.toString(mirrorUpright),
            Integer.toString(left),Integer.toString(top),Integer.toString(width),Integer.toString(height))); }
}
