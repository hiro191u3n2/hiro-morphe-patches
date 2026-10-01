package com.hiro.ulike.binding;
import com.hiro.ulike.style.SampledMakeupPipeline;import java.util.*;import java.nio.file.*;import java.io.*;
public class SkinProbeContracts {
 static int checks;static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}interface Work{void run()throws Exception;}static void fails(Work w)throws Exception{try{w.run();}catch(Exception expected){checks++;return;}throw new AssertionError("accepted invalid diagnostic");}
 static void interpretation()throws Exception{
  int w=31,h=19;Object id=new Object();SampledMakeupPipeline.FrameTile tile=new SampledMakeupPipeline.FrameTile("probe",333,w,h,0,0,w,h);SampledMakeupPipeline.FrameTile part=new SampledMakeupPipeline.FrameTile("probe",333,w,h,3,5,11,7);
  for(DiagnosticSkinMask.Channels c:DiagnosticSkinMask.Channels.values())for(DiagnosticSkinMask.RowOrigin row:DiagnosticSkinMask.RowOrigin.values())for(boolean mirror:new boolean[]{false,true}){
   int[] raw=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int cx=mirror?w-1-x:x,cy=row==DiagnosticSkinMask.RowOrigin.UV_V_ZERO?h-1-y:y;int r=(cx*17+cy*13)&255,g=(int)Math.round((cx+.5)/w*255),b=(int)Math.round((1-(cy+.5)/h)*255);raw[y*w+x]=r<<c.r|g<<c.g|b<<c.b|255<<c.a;}
   DiagnosticSkinMask mask=DiagnosticSkinMask.validate(id,"probe",333,w,h,raw,c,row,mirror);check(!mask.exactNativeMask&&mask.maximumRampError<=.5/255+1e-15);double[] all=mask.sampleTile(id,tile);for(int y=0;y<h;y++)for(int x=0;x<w;x++)check(all[y*w+x]==((x*17+y*13)&255)/255.0);double[] cut=mask.sampleTile(id,part);for(int y=0;y<part.height;y++)for(int x=0;x<part.width;x++)check(cut[y*part.width+x]==all[(y+part.y)*w+x+part.x]);
   raw[0]=0;check(mask.sampleTile(id,tile)[0]==all[0]);fails(()->DiagnosticSkinMask.validate(id,"probe",333,w,h,raw,c,row,mirror));fails(()->mask.sampleTile(new Object(),tile));fails(()->mask.sampleTile(id,new SampledMakeupPipeline.FrameTile("other",333,w,h,0,0,w,h)));
  }
 }
 static void install(String[] args)throws Exception{
  File parent=new File(args[2]);String[] styles={ShotStyleSettings.NATURAL,ShotStyleSettings.PURITY};for(int k=0;k<2;k++){
   StyleObserverInstaller.Installation original=StyleObserverInstaller.install(new File(args[k]),styles[k],parent,"mask_baseline"+k,119);
   SkinMaskProbeInstaller.Probe probe=SkinMaskProbeInstaller.install(new File(args[k]),styles[k],parent,"mask_probe"+k,119);check(!probe.originalMaskPrecisionPreserved&&!probe.deviceCallbackCalibrated);Set<String> changed=new HashSet<>();
   try(java.util.stream.Stream<Path> files=Files.walk(original.directory.toPath())){for(Path p:(Iterable<Path>)files.filter(Files::isRegularFile)::iterator){Path relative=original.directory.toPath().relativize(p);if(!Arrays.equals(Files.readAllBytes(p),Files.readAllBytes(probe.observers.directory.toPath().resolve(relative))))changed.add(relative.toString());}}
   check(changed.equals(k==0?Set.of("AmazingFeature2/xshader/skinseg.frag"):Set.of("AmazingFeature8/xshader/skinseg.frag","AmazingFeature9/xshader/pass0.frag")));
   String shader=Files.readString(probe.observers.directory.toPath().resolve((k==0?"AmazingFeature2":"AmazingFeature8")+"/xshader/skinseg.frag"));check(shader.contains("vec4(mask, texcoord1.x, texcoord1.y, 1.0)"));check(shader.contains("1.0 - texcoord1.y"));
  }
 }
 public static void main(String[] args)throws Exception{interpretation();install(args);System.out.println("Skin diagnostic contracts PASS "+checks);}
}
