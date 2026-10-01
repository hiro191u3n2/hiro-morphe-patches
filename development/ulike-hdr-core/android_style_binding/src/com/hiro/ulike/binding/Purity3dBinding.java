package com.hiro.ulike.binding;
import com.hiro.ulike.style.SampledMakeupPipeline;

/** Binds observed 3D positions/MVP to the authored 1427-vertex UV/topology.
 * This is one material's explicit replacement rasterizer, NOT a complete
 * BindingProvider. Native 2D makeup and skin segmentation cannot be inferred
 * from these data. Original GPU culling/depth/upload conventions are unverified.
 */
public final class Purity3dBinding {
 public enum RasterPolicy { OPENGL_CLIP_TO_TOP_LEFT_NO_CULL_NO_DEPTH_LAST_TRIANGLE_SINGLE_CENTER }
 /** Samples the explicitly routed u_basic source in normalized shader UV.
  * It must belong to the identical still; the binding checks object identity.
  * Implementations must preserve floats and must not substitute a preview.
  */
 public interface ShaderBase {Object frameIdentity();void sample(double u,double v,double[] rgb);}
 private Purity3dBinding(){}
 private static void require(boolean b,String s){if(!b)throw new IllegalArgumentException(s);}
 private static double edge(double ax,double ay,double bx,double by,double px,double py){return (bx-ax)*(py-ay)-(by-ay)*(px-ax);}
 /** An explicit one-face material binding. Multi-face compositing/draw order,
  * renderer-level uniform overrides, segmentation and all 2D passes remain
  * separate requirements. base texture UV is perspective-interpolated exactly
  * as authored uv1; it is not silently equated with output pixel coordinates.
  */
 public static SampledMakeupPipeline.ResolvedPass resolve(ObservedStyleFrame observed,Object frameIdentity,
   SampledMakeupPipeline.FrameTile tile,int face,String component,AuthoredMesh authored,PinnedPngTexture texture,
   PinnedPngTexture.Rows textureRows,ShaderBase base,RasterPolicy policy){
  require(observed!=null&&frameIdentity!=null&&observed.frameIdentity==frameIdentity&&ShotStyleSettings.PURITY.equals(observed.styleId),"same-still Purity observations required");
  require(tile!=null&&authored!=null&&authored.sourceSha256.equals("ff70ea10d239851697a6f9f0f48d25d1be0e9534d32fc4aa4d7abda551d5d0c0")&&authored.vertexCount==1427&&authored.positionSemantic==0&&authored.positionComponents==3&&authored.submeshes.size()==1&&authored.submeshes.get(0).triangleCount()==2304,"pinned 3D mesh layout required");
  require(texture!=null&&texture.sourceSha256.equals("f91dbf4cc446b0db5d3af7eb6a314f0d706c087543357751011e2a04f48a5f47")&&texture.width==512&&texture.height==512&&textureRows!=null&&base!=null&&base.frameIdentity()==frameIdentity&&policy==RasterPolicy.OPENGL_CLIP_TO_TOP_LEFT_NO_CULL_NO_DEPTH_LAST_TRIANGLE_SINGLE_CENTER,"explicit same-frame sampler/raster policy required");
  ObservedStyleFrame.Mesh3d mesh=observed.mesh3d(face);double[] xyz=mesh.copyVertices(),m=mesh.copyMvpRows(),uv=authored.copyUv();int[] triangles=authored.submeshes.get(0).copyTriangleIndices();
  double intensity=observed.uniform("3dmakeup4",component,face,"intensity");require(intensity>=0&&intensity<=1,"observed material intensity outside supported range");
  double[] sx=new double[1427],sy=new double[1427],invW=new double[1427],backgroundU=new double[1427],backgroundV=new double[1427];
  for(int i=0;i<1427;i++){double x=xyz[i*3],y=xyz[i*3+1],z=xyz[i*3+2];double cx=m[0]*x+m[1]*y+m[2]*z+m[3],cy=m[4]*x+m[5]*y+m[6]*z+m[7],cz=m[8]*x+m[9]*y+m[10]*z+m[11],cw=m[12]*x+m[13]*y+m[14]*z+m[15];
   require(Double.isFinite(cx)&&Double.isFinite(cy)&&Double.isFinite(cz)&&Double.isFinite(cw)&&cw>1e-12&&Math.abs(cz)<=cw,"near/far clipping is unsupported; reject instead of inventing visible geometry");
   invW[i]=1/cw;backgroundU[i]=cx/cw*.5+.5;backgroundV[i]=cy/cw*.5+.5;sx[i]=backgroundU[i]*tile.imageWidth;sy[i]=(1-backgroundV[i])*tile.imageHeight;require(Double.isFinite(sx[i])&&Double.isFinite(sy[i]),"nonfinite projected geometry");
  }
  int n=tile.pixels();double[] rgba=new double[n*4],coverage=new double[n],shaderBase=new double[n*3],sample=new double[4],baseSample=new double[3];
  for(int t=0;t<triangles.length;t+=3){int a=triangles[t],b=triangles[t+1],c=triangles[t+2];double area=edge(sx[a],sy[a],sx[b],sy[b],sx[c],sy[c]);if(Math.abs(area)<1e-18)continue;
   int minX=Math.max(tile.x,(int)Math.ceil(Math.min(sx[a],Math.min(sx[b],sx[c]))-.5)),maxX=Math.min(tile.x+tile.width-1,(int)Math.floor(Math.max(sx[a],Math.max(sx[b],sx[c]))-.5));
   int minY=Math.max(tile.y,(int)Math.ceil(Math.min(sy[a],Math.min(sy[b],sy[c]))-.5)),maxY=Math.min(tile.y+tile.height-1,(int)Math.floor(Math.max(sy[a],Math.max(sy[b],sy[c]))-.5));
   for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++){
    double l0=edge(sx[b],sy[b],sx[c],sy[c],x+.5,y+.5)/area,l1=edge(sx[c],sy[c],sx[a],sy[a],x+.5,y+.5)/area,l2=1-l0-l1;if(l0<0||l1<0||l2<0)continue;
    double p0=l0*invW[a],p1=l1*invW[b],p2=l2*invW[c],sum=p0+p1+p2;require(sum>0&&Double.isFinite(sum),"invalid perspective interpolation");p0/=sum;p1/=sum;p2/=sum;
    double u=p0*uv[a*2]+p1*uv[b*2]+p2*uv[c*2],v=p0*uv[a*2+1]+p1*uv[b*2+1]+p2*uv[c*2+1]+.085;
    texture.sample(u,v,textureRows,true,sample);
    base.sample(p0*backgroundU[a]+p1*backgroundU[b]+p2*backgroundU[c],p0*backgroundV[a]+p1*backgroundV[b]+p2*backgroundV[c],baseSample);
    int i=(y-tile.y)*tile.width+x-tile.x;for(int ch=0;ch<3;ch++){require(Double.isFinite(baseSample[ch])&&baseSample[ch]>=0&&baseSample[ch]<=1,"invalid encoded SDR shader base");shaderBase[i*3+ch]=baseSample[ch];}System.arraycopy(sample,0,rgba,i*4,4);coverage[i]=1;
   }
  }
  return new SampledMakeupPipeline.ResolvedPass(SampledMakeupPipeline.Pass.PURITY_3D,tile,rgba,coverage,intensity,1,null,null,null,shaderBase);
 }
}
