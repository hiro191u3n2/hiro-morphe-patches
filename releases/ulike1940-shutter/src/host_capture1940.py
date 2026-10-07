#!/usr/bin/env python3
"""Production source regression: exact frame ownership, real four-photo FIFO,
separate Java/native/system budget, failure pressure and per-photo settings."""
from pathlib import Path
import importlib.util, json, subprocess

def test(root,work,android=None):
 root=Path(root);work=Path(work)/'host-capture1940';work.mkdir(parents=True,exist_ok=True)
 spec=importlib.util.spec_from_file_location('save1939',root/'host_save1939.py');old=importlib.util.module_from_spec(spec);spec.loader.exec_module(old)
 spec=importlib.util.spec_from_file_location('save1935',root/'host_save1935.py');base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
 fixtures=dict(base.FIXTURES)
 fixtures['android/os/Build.java']='package android.os;public class Build{public static class VERSION{public static int SDK_INT=34;}}'
 fixtures['android/os/Debug.java']='package android.os;public class Debug{public static int pss=102400;public static long nativeBytes=104857600L;public static boolean failPss;public static long getPss(){if(failPss)throw new OutOfMemoryError("metric allocation");return pss;}public static long getNativeHeapAllocatedSize(){return nativeBytes;}}'
 fixtures['android/content/Context.java']='package android.content;public class Context{public static final String ACTIVITY_SERVICE="activity";public Context getApplicationContext(){return this;}public Object getSystemService(String name){return android.app.ActivityManager.INSTANCE;}}'
 fixtures['android/app/ActivityManager.java']='package android.app;public class ActivityManager{public static final ActivityManager INSTANCE=new ActivityManager();public static long available=2L*1024*1024*1024,total=8L*1024*1024*1024,threshold=128L*1024*1024;public static boolean low;public static class MemoryInfo{public long availMem,totalMem,threshold;public boolean lowMemory;}public void getMemoryInfo(MemoryInfo m){m.availMem=available;m.totalMem=total;m.threshold=threshold;m.lowMemory=low;}}'
 fixtures['i/n/c/a/b/g.java']='package i.n.c.a.b;public class g{private static final g SELF=new g();public static g j(){return SELF;}public android.content.Context i(){return new android.content.Context();}}'
 fixtures['android/graphics/Bitmap.java']='''package android.graphics;public class Bitmap{public enum Config{ARGB_8888,RGB_565,HARDWARE,RGBA_F16,RGBA_1010102}public static volatile boolean failCopy,mismatchCopy;public int[] pixels;public int width=2,height=2;boolean recycled;public Config config=Config.ARGB_8888;public boolean gainmap,alpha=true,premul=true;public int density=420;public Object colourSpace=new Object();public Bitmap(int[]p){pixels=p;}public int getWidth(){return width;}public int getHeight(){return height;}public Config getConfig(){return config;}public boolean hasGainmap(){return gainmap;}public Object getColorSpace(){return colourSpace;}public int getDensity(){return density;}public boolean hasAlpha(){return alpha;}public boolean isPremultiplied(){return premul;}public Bitmap copy(Config c,boolean mutable){if(failCopy)throw new OutOfMemoryError("scripted copy");Bitmap b=new Bitmap(pixels.clone());b.width=width;b.height=height;b.config=c;b.colourSpace=mismatchCopy?new Object():colourSpace;b.gainmap=gainmap;b.density=density;b.alpha=alpha;b.premul=premul;return b;}public void recycle(){recycled=true;}public boolean isRecycled(){return recycled;}}'''
 fixtures['com/hiro/ulike/SaveQuality2.java']=fixtures['com/hiro/ulike/SaveQuality2.java'].replace('public static void showSaveFailure()', 'public static int[] output186(int w,int h,int r,boolean f){return new int[]{w,h};}public static void showSaveFailure()')
 harness=old.HARNESS
 harness=harness.replace('check(AsyncSave1935.captureBlocked(),"two actual photos bound the memory queue");check(SaveQueue1935.count()==2,"two photo ownership slots only");', '''check(!AsyncSave1935.captureBlocked(),"third actual tap admitted before first encoder starts");check(SaveQueue1935.count()==2,"two real photos retained");
  source(203);AsyncSave1935.submitAuto(manager,0,0);waitFor(()->finishes==3,"third manual photo ownership complete");check(!AsyncSave1935.captureBlocked(),"fourth manual tap admitted");
  source(204);AsyncSave1935.submitAuto(manager,0,0);waitFor(()->finishes==4,"fourth manual photo ownership complete");check(AsyncSave1935.captureBlocked(),"four actual photos bound memory FIFO");check(SaveQueue1935.count()==4,"four ownership slots only");''')
 harness=harness.replace('check(finishes==2,"reservation rejection', 'check(finishes==4,"reservation rejection').replace('check(SaveQueue1935.count()==2,"rejected request','check(SaveQueue1935.count()==4,"rejected request')
 harness=harness.replace('finish(second);idle();check(completed.equals(Arrays.asList("photo-101","photo-202")),', '''finish(second);Encoded extra1=next();check(extra1.id==203&&extra1.bitmap.pixels[0]==203,"third tap owns third independent frame");finish(extra1);Encoded extra2=next();check(extra2.id==204&&extra2.bitmap.pixels[0]==204,"fourth tap owns fourth independent frame");finish(extra2);idle();check(completed.equals(Arrays.asList("photo-101","photo-202","photo-203","photo-204")),''')
 harness=harness.replace('check(finishes==2,"completion cannot', 'check(finishes==4,"completion cannot').replace('completed.get(2).equals("failure")','completed.get(4).equals("failure")')
 harness=harness.replace('CaptureYuv.ready=false;', 'CaptureYuv.state.yuv.w=10000;CaptureYuv.state.yuv.h=2;check(AsyncSave1935.captureBlocked(),\"nonstandard dimensions preserve serial strip-memory ownership\");CaptureYuv.state.yuv.w=2;CaptureYuv.state.yuv.h=2;CaptureYuv.ready=false;')
 harness=harness.replace('for(int mode=0;mode<5;mode++)', 'for(int mode=0;mode<5;mode++)').replace('if(mode==4)s.config=Bitmap.Config.RGBA_F16;', 'if(mode==4)Bitmap.mismatchCopy=true;').replace('idle();Bitmap.failCopy=false;','idle();Bitmap.failCopy=false;Bitmap.mismatchCopy=false;')
 marker='  long boundary=SaveQueue1935.RESERVE+4*20+4*4;'
 harness=harness.replace(marker,'''  for(Bitmap.Config config:new Bitmap.Config[]{Bitmap.Config.RGB_565,Bitmap.Config.RGBA_F16,Bitmap.Config.RGBA_1010102}){
   int prior=finishes;Bitmap s=source(450);s.config=config;AsyncSave1935.submitAuto(manager,0,0);Encoded e=next();
   waitFor(()->finishes==prior+1,"exact precision snapshot releases shutter "+config);
   check(e.bitmap!=s&&e.bitmap.getConfig()==config,"software high precision config retained "+config);
   check(e.bitmap.colourSpace==s.colourSpace&&e.bitmap.density==s.density&&e.bitmap.premul==s.premul&&e.bitmap.alpha==s.alpha,"all rendering attributes retained "+config);
   finish(e);idle();check(!s.isRecycled(),"original renderer-owned pixels remain valid "+config);
  }
  long p=4080L*3060L,m=MemoryBudget1940.MIB;
  check(SaveQueue1935.processingMemoryAllows(256*m,p,p)==false,"previous Java-only guard rejects 12.5 MP even entirely empty 256 MiB heap");
  check(MemoryBudget1940.permits(220*m,800*m,p,p,4),"same full resolution can overlap with separate native Bitmap budget");
  check(!MemoryBudget1940.permits(100*m,800*m,p,p,4),"insufficient Java NV21/fusion buffers still refuse overlap");
  check(!MemoryBudget1940.permits(220*m,200*m,p,p,4),"insufficient native encoder/full bitmap workspace refuses overlap");
  check(MemoryBudget1940.nativeHeadroom(2*1024*m,128*m,8*1024*m,100*m,100*m,false)==924*m,"system/process/native cap uses restrictive measurement");
  check(MemoryBudget1940.nativeHeadroom(2*1024*m,128*m,8*1024*m,100*m,100*m,true)==0,"OS low-memory signal prevents overlap");
  check(MemoryBudget1940.nativeHeadroom(300*m,128*m,8*1024*m,100*m,100*m,false)==44*m,"system reserve independently bounds native pressure");
  check(MemoryBudget1940.nativeHeadroom(2*1024*m,128*m,8*1024*m,100*m,1025*m,false)==0,"unresident native allocations also block admission");
  check(MemoryBudget1940.nativeHeadroom(2*1024*m,128*m,8*1024*m,0,100*m,false)==0,"missing process measure cannot guess abundant memory");
  for(int mode=0;mode<3;mode++){
   int prior=finishes;Bitmap s=source(460+mode);if(mode==0)android.app.ActivityManager.low=true;if(mode==1)android.app.ActivityManager.available=300*m;if(mode==2)android.os.Debug.nativeBytes=1025*m;
   AsyncSave1935.submitAuto(manager,0,0);Encoded e=next();check(e.bitmap==s&&AsyncSave1935.captureBlocked(),"real pressure uses exact serial source "+mode);finish(e);idle();
   check(finishes==prior+1&&!s.isRecycled(),"pressure completion balances native readiness and source ownership "+mode);
   android.app.ActivityManager.low=false;android.app.ActivityManager.available=2*1024*m;android.os.Debug.nativeBytes=100*m;
  }
  int largePrior=finishes;Bitmap large=source(470);ShotContext1932.Snapshot largeContext=ShotContext1932.forBitmap(large);largeContext.sourceWidth=4080;largeContext.sourceHeight=3060;
  AsyncSave1935.submitAuto(manager,0,0);Encoded largeSave=next();waitFor(()->finishes==largePrior+1,"larger older actual source owns full quality metadata");
  source(471);AsyncSave1935.submitAuto(manager,0,0);waitFor(()->finishes==largePrior+2,"newer smaller source copied independently");
  android.os.Debug.nativeBytes=800*m;
  check(AsyncSave1935.captureBlocked(),"new smaller photo cannot discard older large pending workspace from admission budget");
  android.os.Debug.nativeBytes=100*m;finish(largeSave);Encoded smallerSave=next();check(smallerSave.id==471,"older large save drains before smaller receipt");finish(smallerSave);idle();
  long floor256=MemoryBudget1940.qualityFloor(4080,3060,4080,3060,256*m,4);
  long floor768=MemoryBudget1940.qualityFloor(4080,3060,4080,3060,768*m,4);
  check(MemoryBudget1940.chromaRadius(4080,3060)==96,"quality radius comes from inherited dimensions, not requested NR level");
  check(floor256==229884416L,"inherited 256 MiB plan guard is exactly four workers/core256/nativeWork0");
  check(floor768==437931776L,"inherited ample-heap plan guard is exactly four workers/core512/nativeWork1");
  check(MemoryBudget1940.javaAllows(220*m,p),"old split-memory admission would permit unsafe normalization overlap");
  check(!MemoryBudget1940.permits(220*m,800*m,p,p,4,floor256),"full-resolution quality floor rejects overlap that would downgrade original plan");
  long sameQualityBoundary=floor768+p*9;
  check(!MemoryBudget1940.permits(sameQualityBoundary-1,800*m,p,p,4,floor768),"one byte under inherited full-quality plus nextYUV budget holds shutter");
  check(MemoryBudget1940.permits(sameQualityBoundary,800*m,p,p,4,floor768),"exact full-quality plus nextYUV boundary permits manual capture");
  check(MemoryBudget1940.selectedChromaFloor(4080,3060,sameQualityBoundary-p*9,4)==floor768,"after next full NV21/fusion work, original worker/core/native plan still fits unchanged");
  check(MemoryBudget1940.permits(768*m,800*m,p,p,4,floor768),"adequate real memory permits unchanged full-resolution processing overlap");
  check(!MemoryBudget1940.javaAllows(Long.MAX_VALUE,p,Long.MAX_VALUE),"unsupported quality plan sentinel cannot overflow into admission");
  int fullPrior=finishes;Bitmap full=source(480);full.width=4080;full.height=3060;ShotContext1932.Snapshot fullContext=ShotContext1932.forBitmap(full);fullContext.sourceWidth=4080;fullContext.sourceHeight=3060;
  AsyncSave1935.submitAuto(manager,0,0);Encoded fullSave=next();
  check(fullSave.bitmap!=full&&fullSave.bitmap.getWidth()==4080&&fullSave.bitmap.getHeight()==3060,"full-resolution source is independently owned with same dimensions");
  check(finishes==fullPrior&&AsyncSave1935.captureBlocked(),"256 MiB heap holds next shutter while same-quality original normalizer still needs guard capacity");
  fullSave.startEncoding.countDown();check(fullSave.encoderStarted.await(5,TimeUnit.SECONDS),"full-resolution quality output reaches existing encoder handoff");
  waitFor(()->finishes==fullPrior+1,"same-quality completed pixels reopen shutter during encoder, before original file save ends");
  check(!AsyncSave1935.captureBlocked()&&fullSave.release.getCount()==1,"next actual capture allowed while independent full-resolution encode is still unfinished");
  fullSave.release.countDown();idle();
  check(!full.isRecycled(),"full-resolution SDK source survives quality-safe queued save");
  int metricPrior=finishes,completedBeforeMetric=completed.size();Bitmap metricFull=source(481);metricFull.width=4080;metricFull.height=3060;ShotContext1932.Snapshot metricContext=ShotContext1932.forBitmap(metricFull);metricContext.sourceWidth=4080;metricContext.sourceHeight=3060;
  AsyncSave1935.submitAuto(manager,0,0);Encoded metricSave=next();android.os.Debug.failPss=true;
  metricSave.startEncoding.countDown();check(metricSave.encoderStarted.await(5,TimeUnit.SECONDS),"metrics allocation failure at encoder handoff cannot fail an already-owned save");
  check(SaveQueue1935.count()==1&&finishes==metricPrior,"post-submit budget failure does not release or duplicate actual-photo reservation");
  metricSave.release.countDown();idle();android.os.Debug.failPss=false;
  check(completed.size()==completedBeforeMetric+1&&completed.get(completedBeforeMetric).equals("photo-481"),"temporary metrics OOM cannot convert exact normal output into save failure");
  check(finishes==metricPrior+1,"temporary metrics OOM retains one capture finish per real save");
  long javaBoundary=MemoryBudget1940.JAVA_RESERVE+p*9;
  check(!MemoryBudget1940.javaAllows(javaBoundary-1,p)&&MemoryBudget1940.javaAllows(javaBoundary,p),"real full-resolution Java workspace boundary");
  long nativeBoundary=MemoryBudget1940.NATIVE_RESERVE+p*20+p*8;
  check(!MemoryBudget1940.nativeAllows(nativeBoundary-1,p,p,8)&&MemoryBudget1940.nativeAllows(nativeBoundary,p,p,8),"full precision native workspace boundary");
  check(!MemoryBudget1940.permits(Long.MAX_VALUE,Long.MAX_VALUE,32000001,1,4),"dimensions remain bounded");
  long boundary=SaveQueue1935.RESERVE+4*20+4*4;''')
 harness=harness.replace('PASS manual-save-response','PASS rapid-manual-capture1940').replace('scenarios=21','scenarios=38')
 fixtures['com/hiro/ulike/SaveHarness1935.java']=harness
 source=work/'fixtures';classes=work/'classes';classes.mkdir(exist_ok=True)
 for name,value in fixtures.items():
  f=source/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(value)
 command=['javac','-encoding','UTF-8','-source','8','-target','8','-d',str(classes)]+[str(root/name) for name in ['AsyncSave1935.java','SaveQueue1935.java','CaptureMemory1940.java','MemoryBudget1940.java']]+[str(p) for p in sorted(source.rglob('*.java'))]
 compiled=subprocess.run(command,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=120);(work/'compile.log').write_text(compiled.stdout)
 if compiled.returncode:raise RuntimeError(compiled.stdout)
 ran=subprocess.run(['java','-Xmx256m','-cp',str(classes),'com.hiro.ulike.SaveHarness1935'],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60);(work/'result.txt').write_text(ran.stdout)
 if ran.returncode:raise RuntimeError(ran.stdout)
 count=int(ran.stdout.split('assertions=')[1].split()[0]);result={'suite':'rapid_manual_capture','status':'passed','assertions':count,'device_tested':False,'scenarios':38};(work/'result.json').write_text(json.dumps(result,indent=2));return result
if __name__=='__main__':
 import sys;print(json.dumps(test(Path(__file__).parent,Path(sys.argv[1])),indent=2))
