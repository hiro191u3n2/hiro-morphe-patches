package com.hiro.ulike;import android.graphics.Bitmap;
public final class ChromaPipeline177 {
 public static boolean enabled=true; public static int applies;
 public static boolean enabled1932(){return enabled;}
 @SuppressWarnings("unchecked") static QualityPixels1932.Plan referencePlan(){try{
  java.lang.reflect.Field f=ReferencePipeline1934.class.getDeclaredField("CURRENT");f.setAccessible(true);return ((ThreadLocal<QualityPixels1932.Plan>)f.get(null)).get();
 }catch(Exception e){throw new RuntimeException(e);}}
 public static Bitmap apply(Bitmap b,Bitmap original,PhotoDetail.Settings settings){
  applies++; if(!enabled && !settings.noiseOn)return b;
  ThreadLocal<QualityPixels1932.Plan> local=HostAudit1932.<QualityPixels1932.Plan>local("CURRENT");QualityPixels1932.Plan old=local.get(),plan=old;
  if(plan==null)plan=referencePlan();local.set(plan);
  try {
   ChromaPipeline186.State state=new ChromaPipeline186.State(b.snapshot(),b.getWidth(),b.getHeight(),37,settings.noiseOn?settings.noiseLevel:0);
   state.texture=settings.texturePriority;state.halos=settings.haloSuppression;state.shadows=settings.shadowPriority;
   QualityPipeline1932.run(state,new ChromaPipeline186.Buffer(state));if(state.failed)throw new IllegalStateException("fixture scheduler failed");
   Bitmap out=b.copy(Bitmap.Config.ARGB_8888,true);out.setPixels(state.result,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());return out;
  } finally {if(old==null)local.remove();else local.set(old);}
 }
}
