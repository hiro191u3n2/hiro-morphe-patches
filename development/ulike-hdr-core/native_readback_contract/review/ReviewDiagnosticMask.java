package review;
import com.hiro.ulike.binding.*;
import com.hiro.ulike.style.SampledMakeupPipeline.FrameTile;
import java.nio.file.*;import java.util.*;import java.io.*;
public final class ReviewDiagnosticMask {
 static long checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 interface Run{void go()throws Exception;}static void reject(Run r)throws Exception{try{r.go();throw new AssertionError("accepted invalid binding");}catch(IllegalArgumentException e){checks++;}}
 static final int[][] SHIFT={{16,8,0,24},{0,8,16,24},{24,16,8,0},{8,16,24,0}};
 static int[] raw(int w,int h,int layout,boolean bottomUp,boolean flipped){int[] p=new int[w*h];int[] s=SHIFT[layout];for(int y=0;y<h;y++)for(int x=0;x<w;x++){
  int nativeX=flipped?w-1-x:x,nativeY=bottomUp?h-1-y:y;
  int r=(nativeX*29+nativeY*71+3)&255,g=(int)Math.floor(255.0*(nativeX+.5)/w+.5),b=(int)Math.floor(255.0*(h-nativeY-.5)/h+.5);
  p[y*w+x]=r<<s[0]|g<<s[1]|b<<s[2]|255<<s[3];}return p;}
 public static void main(String[] a)throws Exception{
  int w=37,h=23;Object owner=new Object();FrameTile full=new FrameTile("capture",19,w,h,0,0,w,h),slice=new FrameTile("capture",19,w,h,9,4,7,6);
  for(int layout=0;layout<4;layout++)for(boolean bottom:new boolean[]{false,true})for(boolean flip:new boolean[]{false,true}){
   int[] p=raw(w,h,layout,bottom,flip);DiagnosticSkinMask.Channels channel=DiagnosticSkinMask.Channels.values()[layout];DiagnosticSkinMask.RowOrigin origin=bottom?DiagnosticSkinMask.RowOrigin.UV_V_ZERO:DiagnosticSkinMask.RowOrigin.UV_V_ONE;
   DiagnosticSkinMask mask=DiagnosticSkinMask.validate(owner,"capture",19,w,h,p,channel,origin,flip);check(!mask.exactNativeMask,"no exact precision claim");double[] values=mask.sampleTile(owner,full);
   for(int y=0;y<h;y++)for(int x=0;x<w;x++)check(values[y*w+x]==((x*29+y*71+3)&255)/255.,"analytic independently packed mask");
   double[] cut=mask.sampleTile(owner,slice);for(int y=0;y<6;y++)for(int x=0;x<7;x++)check(cut[y*7+x]==values[(y+4)*w+x+9],"tile correspondence");
   Arrays.fill(p,0);values[0]=-9;check(mask.sampleTile(owner,full)[0]==3/255.,"owned mask and returned copies");
   reject(()->mask.sampleTile(new Object(),full));reject(()->mask.sampleTile(owner,new FrameTile("capture",20,w,h,0,0,w,h)));reject(()->mask.sampleTile(owner,new FrameTile("foreign",19,w,h,0,0,w,h)));
   int[] alpha=raw(w,h,layout,bottom,flip);alpha[7]&=~(1<<SHIFT[layout][3]);reject(()->DiagnosticSkinMask.validate(owner,"capture",19,w,h,alpha,channel,origin,flip));
   int[] gamma=raw(w,h,layout,bottom,flip);for(int i=0;i<gamma.length;i++){int v=(gamma[i]>>>SHIFT[layout][1])&255;int changed=(int)Math.round(255*Math.sqrt(v/255.));gamma[i]=(gamma[i]&~(255<<SHIFT[layout][1]))|changed<<SHIFT[layout][1];}reject(()->DiagnosticSkinMask.validate(owner,"capture",19,w,h,gamma,channel,origin,flip));
   int[] correct=raw(w,h,layout,bottom,flip);reject(()->DiagnosticSkinMask.validate(owner,"capture",19,w,h,correct,channel,origin,!flip));
  }
  reject(()->DiagnosticSkinMask.validate(owner,"capture",19,16384,16384,new int[1],DiagnosticSkinMask.Channels.ARGB,DiagnosticSkinMask.RowOrigin.UV_V_ONE,false));
  Path parent=Paths.get(a[2]);for(int style=0;style<2;style++){
   String id=style==0?ShotStyleSettings.NATURAL:ShotStyleSettings.PURITY;
   StyleObserverInstaller.Installation before=StyleObserverInstaller.install(new File(a[style]),id,parent.toFile(),"baseline"+style,904);
   SkinMaskProbeInstaller.Probe after=SkinMaskProbeInstaller.install(new File(a[style]),id,parent.toFile(),"probe"+style,904);check(!after.deviceCallbackCalibrated&&!after.originalMaskPrecisionPreserved,"diagnostic truth flags");
   TreeSet<String> changed=new TreeSet<>();try(java.util.stream.Stream<Path> files=Files.walk(before.directory.toPath())){for(Path f:(Iterable<Path>)files.filter(Files::isRegularFile)::iterator){Path rel=before.directory.toPath().relativize(f);if(!Arrays.equals(Files.readAllBytes(f),Files.readAllBytes(after.observers.directory.toPath().resolve(rel))))changed.add(rel.toString());}}
   TreeSet<String> want=new TreeSet<>();want.add((style==0?"AmazingFeature2":"AmazingFeature8")+"/xshader/skinseg.frag");if(style==1)want.add("AmazingFeature9/xshader/pass0.frag");check(want.equals(changed),"only authorized final diagnostic shader modifications");
   check(after.observers.nonce==904&&after.observers.styleId.equals(id),"installation binding retained");
   reject(()->SkinMaskProbeInstaller.install(new File(a[0]),ShotStyleSettings.NATURAL,parent.toFile(),"../escape",904));
  }
  System.out.println("{\"status\":\"PASS\",\"independent_checks\":"+checks+",\"callback_interpretations\":16,\"actual_archive_installs\":2}");
 }
}
