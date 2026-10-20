package com.hiro.ulike;
import android.graphics.Bitmap;
import java.util.concurrent.atomic.AtomicInteger;
/** Host-only encoder-preparation surrogate; real scheduler, residual NR and sharp run. */
public final class ChromaPipeline177 {
 public static volatile boolean enabled=true;
 public static final AtomicInteger APPLIES=new AtomicInteger();
 public static final ThreadLocal<QualityPixels1932.Plan> LAST_PLAN=new ThreadLocal<QualityPixels1932.Plan>();
 public static final ThreadLocal<PhotoDetail.Settings> LAST_SETTINGS=new ThreadLocal<PhotoDetail.Settings>();
 public static boolean enabled1932(){return enabled;}
 public static Bitmap apply(Bitmap b,Bitmap original,PhotoDetail.Settings settings){
  APPLIES.incrementAndGet();LAST_PLAN.set(HostAudit1932.current());LAST_SETTINGS.set(settings);
  HostAudit1932.event("source:"+(settings.noiseOn?settings.noiseLevel:0)+":"+(AsyncSave1935.chroma(enabled)?1:0));
  if(!AsyncSave1935.chroma(enabled) && !settings.noiseOn && !settings.sharpOn)return b;
  ChromaPipeline186.State state=new ChromaPipeline186.State(b.snapshot(),b.getWidth(),b.getHeight(),37,settings.noiseOn?settings.noiseLevel:0);
  state.sharp=settings.sharpOn?settings.sharpLevel:0;
  state.texture=settings.texturePriority;state.halos=settings.haloSuppression;state.shadows=settings.shadowPriority;
  QualityPipeline1932.run(state,new ChromaPipeline186.Buffer(state));
  if(state.failed)throw new IllegalStateException("fixture scheduler failed");
  Bitmap out=b.copy(Bitmap.Config.ARGB_8888,true);
  try{out.setPixels(state.result,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());return out;}
  catch(RuntimeException e){out.recycle();throw e;}catch(OutOfMemoryError e){out.recycle();throw e;}
 }
}
