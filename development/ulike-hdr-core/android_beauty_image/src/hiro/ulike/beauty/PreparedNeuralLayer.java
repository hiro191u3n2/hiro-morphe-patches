package hiro.ulike.beauty;

import hiro.ulike.model.PinnedModel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/** Immutable generated SDR appearance layer before compositing with any photo.
 * Buffer ownership is independent of the inference session; closing the engine
 * does not invalidate this object. Coordinates refer to the exact source raster.
 * It is not an HDR layer until an explicit external HDR appearance policy maps it.
 */
public final class PreparedNeuralLayer {
    public final int width,height;
    public final Object frameIdentity;
    public final PinnedModel.Style style;
    public final String domain=BeautyImageEngine.SDR_DOMAIN;
    public final String processingSha256;
    private final BeautyImageEngine.SourceRgbFloat preparedSource;
    private final double[] rgba,matrix;
    private final PinnedAssets.NeuralMask mask;
    private final double intensity;
    PreparedNeuralLayer(PinnedModel.Style style,int width,int height,Object identity,
                       BeautyImageEngine.SourceRgbFloat preparedSource,double[] rgba,
                       double[] matrix,PinnedAssets.NeuralMask mask,double intensity) {
        if(style==null || preparedSource==null || identity==null || mask==null || mask.style!=style
                || width<1 || height<1 || rgba==null || rgba.length!=256*256*4
                || !Double.isFinite(intensity) || intensity<0 || intensity>1)
            throw new IllegalArgumentException("valid pinned generated layer required");
        this.rgba=rgba.clone();for(double v:this.rgba)if(!Double.isFinite(v) || v<0 || v>1)
            throw new IllegalArgumentException("generated unit samples required");
        this.matrix=BeautyImageEngine.affine(matrix);this.style=style;this.width=width;this.height=height;
        this.frameIdentity=identity;this.preparedSource=preparedSource;this.mask=mask;this.intensity=intensity;
        this.processingSha256=fingerprint();
    }
    private String fingerprint(){
        try {MessageDigest digest=MessageDigest.getInstance("SHA-256");
            digest.update(("prepared-neural-layer-v1;"+style+";maskVerticalFlip="+mask.verticalFlip+";width="+width+";height="+height).getBytes(StandardCharsets.UTF_8));
            update(digest,intensity);for(double v:matrix)update(digest,v);for(double v:rgba)update(digest,v);
            StringBuilder s=new StringBuilder();for(byte v:digest.digest())s.append(Character.forDigit((v&255)>>>4,16)).append(Character.forDigit(v&15,16));return s.toString();
        }catch(NoSuchAlgorithmException e){throw new AssertionError(e);}
    }
    private static void update(MessageDigest d,double v){long bits=Double.doubleToRawLongBits(v);for(int shift=56;shift>=0;shift-=8)d.update((byte)(bits>>>shift));}
    /** Strong object identity permits an HDR policy to require its exact typed SDR rendition. */
    public boolean preparedFrom(BeautyImageEngine.SourceRgbFloat expected){return expected==preparedSource;}
    public double[] sourceToCrop(){return matrix.clone();}
    /** Encoded SDR generated RGB and actual projected mask/model-alpha/intensity weight.
     * At zero weight the first three values are zero; no source RGB is sampled.
     */
    public void sampleAt(int x,int y,double[] output) {
        if(x<0 || y<0 || x>=width || y>=height || output==null || output.length<4)
            throw new IllegalArgumentException("layer pixel/destination bounds");
        output[0]=output[1]=output[2]=output[3]=0;
        double cx=matrix[0]*x+matrix[1]*y+matrix[2],cy=matrix[3]*x+matrix[4]*y+matrix[5];
        if(!Double.isFinite(cx) || !Double.isFinite(cy))throw new IllegalArgumentException("nonfinite projected coordinates");
        if(cx<-.5 || cx>=255.5 || cy<-.5 || cy>=255.5 || intensity==0)return;
        double red=sampleMask((cx+.5)*1.25-.5,(cy+.5)*1.25-.5),alpha=sampleGenerated(cx,cy,3);
        double weight=(style==PinnedModel.Style.NATURAL_BLUSH?Math.min(red,alpha):red*alpha)*intensity;
        if(weight<=0)return;
        for(int c=0;c<3;c++)output[c]=sampleGenerated(cx,cy,c);output[3]=weight;
    }
    private double sampleGenerated(double x,double y,int c) {
        x=clamp(x,0,255);y=clamp(y,0,255);int ix=(int)Math.floor(x),iy=(int)Math.floor(y);
        double fx=x-ix,fy=y-iy,out=0;
        for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)
            out+=rgba[(Math.min(iy+dy,255)*256+Math.min(ix+dx,255))*4+c]*(dx==0?1-fx:fx)*(dy==0?1-fy:fy);
        return clamp(out,0,1);
    }
    private double sampleMask(double x,double y) {
        x=clamp(x,0,319);y=clamp(y,0,319);int ix=(int)Math.floor(x),iy=(int)Math.floor(y);
        double fx=x-ix,fy=y-iy,out=0;
        for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)
            out+=mask.at(Math.min(ix+dx,319),Math.min(iy+dy,319))*(dx==0?1-fx:fx)*(dy==0?1-fy:fy);
        return clamp(out,0,1);
    }
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
}
