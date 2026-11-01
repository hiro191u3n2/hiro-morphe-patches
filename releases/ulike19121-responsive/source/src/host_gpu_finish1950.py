#!/usr/bin/env python3
"""Exercise unchanged fused GLES/JNI with scalar sums and original Java finish.

Mesa host execution establishes pixel equivalence and bounded success/failure
semantics. It is not an Android device speed measurement.
"""
import argparse, hashlib, importlib.util, json, os, pathlib, shutil, subprocess

JAVA=r'''
package com.hiro.ulike;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
public final class GpuFinish1950Test {
  static final Random R=new Random(1950);static int assertions,cases;
  static native void scalarAggregate(int[] input,int[] meta,int[] output,int width,int begin,int end,int lo,int hi,int radius,int[] range);
  static native void forceTimeout(boolean on);
  static native long[] fenceFacts();
  static native boolean beginExternal();static native boolean externalUnchanged();static native boolean endExternal();
  static Method finish,finishInto;
  static void req(boolean b,String message){assertions++;if(!b)throw new AssertionError(message);}
  static int y(int p){return (77*((p>>>16)&255)+150*((p>>>8)&255)+29*(p&255)+128)>>>8;}
  static int clamp(int x){return Math.max(0,Math.min(255,x));}
  static int roundDiv(int x,int d){return x<0?-((-x+d/2)/d):(x+d/2)/d;}
  static int tone(int yc){int t=Math.max(0,Math.min(256,(144-yc)*256/112));return (t*t*(768-2*t)+32768)>>>16;}
  static int budgetQ8(int range,int global,int beauty,int skin,int limit){
    if(global==0||beauty==0||skin==0)return global;
    int smooth=Math.max(0,Math.min(256,(limit-Math.max(0,range))*256/Math.max(1,limit)));
    int reduction=(int)((long)skin*smooth*beauty*112>>24);return global*(256-reduction)>>8;
  }
  // Exact v1949 Java finalization, using the unchanged scalar aggregate oracle.
  static void reference(int[] input,int[] meta,int[] policy,int[] out,int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,boolean shadows,int global,int beauty,int smoothLimit){
    int count=(end-begin)*w;int[] summary=new int[count*3];
    scalarAggregate(input,meta,summary,w,begin,end,lo,hi,radius,range);
    int fineBase=128+noise*30,coarseBase=96+noise*34;
    for(int index=0;index<count;index++) {
      int stats=summary[index*3+2];if(stats>=0)continue;
      int tolerance=(meta[index]>>>12)&63,edge=(stats>>>8)&255,periodic=(stats>>>16)&511;
      int flat=Math.max(0,Math.min(256,(tolerance*3-edge)*256/Math.max(1,tolerance*2)));
      flat=flat*(256-periodic)>>8;if(flat==0)continue;
      int yc=(meta[index]>>>18)&255,dark=shadows?tone(yc):0;
      int fine=(fineBase+(dark*8>>8))*flat>>8;
      int coarse=(coarseBase+(dark*8>>8))*flat*flat>>16;
      int row=begin+index/w,col=index%w,at=row*w+col,center=input[at];
      int budget=policy[index*3],base=policy[index*3+1];
      int coordinated=budgetQ8(stats&255,global,beauty,policy[index*3+2],smoothLimit);
      if(base>0)budget=Math.min(256,budget*coordinated/base);
      fine=Math.min(248,fine)*budget>>8;coarse=Math.min(232,coarse)*budget>>8;
      if(fine==0 && coarse==0)continue;
      int near=summary[index*3],wide=summary[index*3+1];
      int r1=(near>>>16)&255,g1=(near>>>8)&255,b1=near&255;
      int r2=(wide>>>16)&255,g2=(wide>>>8)&255,b2=wide&255;
      int fy=y(near),cy=y(wide);
      int newY=yc+roundDiv((fy-yc)*fine+(cy-fy)*coarse,256);
      newY=Math.max(Math.min(yc,Math.min(fy,cy)),Math.min(Math.max(yc,Math.max(fy,cy)),newY));
      int colorFine=fine*7/8,colorCoarse=coarse*7/8;
      int cr=(center>>>16)&255,cg=(center>>>8)&255,cb=center&255;
      int or=clamp(cr+roundDiv((r1-cr)*colorFine+(r2-r1)*colorCoarse,256));
      int og=clamp(cg+roundDiv((g1-cg)*colorFine+(g2-g1)*colorCoarse,256));
      int ob=clamp(cb+roundDiv((b1-cb)*colorFine+(b2-b1)*colorCoarse,256));
      int delta=newY-y(0xff000000|(or<<16)|(og<<8)|ob);
      delta=Math.max(-Math.min(or,Math.min(og,ob)),Math.min(255-Math.max(or,Math.max(og,ob)),delta));
      out[index]=(center&0xff000000)|((or+delta)<<16)|((og+delta)<<8)|(ob+delta);
    }
  }
  static boolean gpu(int[] input,int[] meta,int[] policy,int[] out,int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,boolean shadows,int global,int beauty,int smoothLimit)throws Exception{
    return (Boolean)finish.invoke(null,input,meta,policy,out,w,rows,begin,end,lo,hi,radius,range,noise,shadows,global,beauty,smoothLimit);
  }
  static boolean gpuInto(int[] input,int[] meta,int[] policy,int[] out,int offset,int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,boolean shadows,int global,int beauty,int smoothLimit)throws Exception{
    return (Boolean)finishInto.invoke(null,input,meta,policy,out,offset,w,rows,begin,end,lo,hi,radius,range,noise,shadows,global,beauty,smoothLimit);
  }
  static void same(int[] a,int[] b,String message){req(a.length==b.length,message+" length");for(int i=0;i<a.length;i++)req(a[i]==b[i],message+" index="+i+" "+Integer.toHexString(a[i])+" != "+Integer.toHexString(b[i]));}
  static int[] ranges(){int[] out=new int[33*256];for(int s=1;s<=32;s++)for(int d=0;d<256;d++)out[s*256+d]=(int)Math.round(256.0*Math.exp(-(double)d*d/(2.0*s*s)));return out;}
  static void fixture(int w,int rows,int radius,int mode,int noise,boolean shadows,int global,int beauty)throws Exception{
    int lo=rows>5?1:0,hi=rows>5?rows-1:rows,begin=lo,end=hi;
    if(mode==4&&rows>12){begin+=4;end-=4;}
    int n=(end-begin)*w;int[] input=new int[w*rows],meta=new int[n],policy=new int[n*3],out=new int[n+13];
    int[] range=ranges();int limit=10+R.nextInt(25);
    for(int row=0;row<rows;row++)for(int col=0;col<w;col++){
      int g=mode==0?105+R.nextInt(41)-20:mode==1?((col+row)&1)*35+60:mode==2?R.nextInt(256):mode==3?255:45+R.nextInt(31);
      int r=mode==3?255:clamp(g+R.nextInt(25)-12),b=mode==3?255:clamp(g+R.nextInt(25)-12);
      int alpha=mode==2&&R.nextInt(4)==0?R.nextInt(255):255;input[row*w+col]=(alpha<<24)|(r<<16)|(g<<8)|b;
    }
    if(mode==5)Arrays.fill(range,0);
    for(int i=0;i<n;i++){
      int center=input[begin*w+i],sigma=2+R.nextInt(31),threshold=3+R.nextInt(40),tolerance=3+R.nextInt(55);
      meta[i]=(i%11==0?0:0x80000000)|sigma|(threshold<<6)|(tolerance<<12)|(y(center)<<18);
      policy[i*3]=R.nextInt(257);policy[i*3+1]=R.nextInt(257);policy[i*3+2]=R.nextInt(257);
    }
    for(int i=0;i<out.length;i++)out[i]=R.nextInt();
    int[] seedOut=out.clone(),expected=out.clone(),beforeInput=input.clone(),beforeMeta=meta.clone(),beforePolicy=policy.clone(),beforeRange=range.clone();
    reference(input,meta,policy,expected,w,rows,begin,end,lo,hi,radius,range,noise,shadows,global,beauty,limit);
    req(gpu(input,meta,policy,out,w,rows,begin,end,lo,hi,radius,range,noise,shadows,global,beauty,limit),"fused JNI failed "+w+"x"+rows+" radius="+radius+" mode="+mode);
    same(out,expected,"final pixel + inactive destination + tail");
    int offset=11;int[] full=new int[n+41];for(int i=0;i<full.length;i++)full[i]=R.nextInt();
    System.arraycopy(seedOut,0,full,offset,n);int[] expectedFull=full.clone();System.arraycopy(expected,0,expectedFull,offset,n);
    req(gpuInto(input,meta,policy,full,offset,w,rows,begin,end,lo,hi,radius,range,noise,shadows,global,beauty,limit),"direct destination JNI failed");
    same(full,expectedFull,"direct final pixels + inactive seed + prefix/suffix");
    same(input,beforeInput,"immutable source");same(meta,beforeMeta,"immutable meta");same(policy,beforePolicy,"immutable policy");same(range,beforeRange,"immutable range");cases++;
  }
  static void validation()throws Exception{
    int w=3,h=5,n=w*h;int[] src=new int[n],meta=new int[n],policy=new int[n*3],out=new int[n+7],range=ranges();Arrays.fill(src,0xff668899);Arrays.fill(out,0x31571944);
    int[] saved=out.clone();
    req(!gpu(src,meta,policy,out,w,h,0,h,0,h,4,range,0,true,256,256,24),"invalid noise accepted");same(out,saved,"invalid destination untouched");
    req(!gpu(src,meta,policy,src,w,h,0,h,0,h,4,range,4,true,256,256,24),"aliased destination accepted");
    req(beginExternal(),"external context setup");
    try{req(!gpu(src,meta,policy,out,w,h,0,h,0,h,4,range,4,true,256,256,24),"foreign EGL accepted");req(externalUnchanged(),"foreign EGL changed");same(out,saved,"foreign destination untouched");}finally{req(endExternal(),"external EGL cleanup");}
    req(!gpuInto(src,meta,policy,out,-1,w,h,0,h,0,h,4,range,4,true,256,256,24),"negative destination offset accepted");
    req(!gpuInto(src,meta,policy,out,8,w,h,0,h,0,h,4,range,4,true,256,256,24),"destination overflow accepted");same(out,saved,"invalid offset untouched");
    forceTimeout(true);req(!gpuInto(src,meta,policy,out,3,w,h,0,h,0,h,4,range,4,true,256,256,24),"timeout accepted");
    long[] facts=fenceFacts();req(facts[0]==200000000L&&facts[1]==1&&facts[2]==0,"bounded fence before map");same(out,saved,"timeout destination untouched");
    forceTimeout(false);req(!gpu(src,meta,policy,out,w,h,0,h,0,h,4,range,4,true,256,256,24),"disabled backend resumed");same(out,saved,"disabled destination untouched");
  }
  public static void main(String[] args)throws Exception{
    System.loadLibrary("ulike_gpu1949");finish=GpuInteger1949.class.getDeclaredMethod("finishNative",int[].class,int[].class,int[].class,int[].class,int.class,int.class,int.class,int.class,int.class,int.class,int.class,int[].class,int.class,boolean.class,int.class,int.class,int.class);finish.setAccessible(true);
    finishInto=GpuInteger1949.class.getDeclaredMethod("finishIntoNative",int[].class,int[].class,int[].class,int[].class,int.class,int.class,int.class,int.class,int.class,int.class,int.class,int.class,int[].class,int.class,boolean.class,int.class,int.class,int.class);finishInto.setAccessible(true);
    for(int[] dim:new int[][]{{1,1},{3,5},{9,7},{17,19},{127,137}})for(int radius=1;radius<=4;radius++)for(int mode=0;mode<6;mode++)fixture(dim[0],dim[1],radius,mode,1+mode%4,(mode&1)==0,mode==5?0:mode==2?137:256,mode==1?0:mode==3?256:R.nextInt(257));
    fixture(4080,136,4,4,4,true,256,256);validation();
    System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"fused_finish_cases\":"+cases+"}");
  }
}
'''

def run(args,env=None):
    result=subprocess.run([str(x) for x in args],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,env=env)
    if result.returncode:raise RuntimeError(result.stdout)
    return result.stdout

def fallback_test(root,work,jdk):
    """Instrument only generated fixtures to prove disabled-GPU policy is once."""
    folder=work/'fallback-fixture';folder.mkdir(exist_ok=True);classes=folder/'classes';classes.mkdir(exist_ok=True)
    shadow=(root/'QualityShadow1932.java').read_text().replace('private QualityShadow1932() {}','private QualityShadow1932() {}\n    static int TEST_META;')
    shadow=shadow.replace('float measured=plan==null?','TEST_META++;float measured=plan==null?')
    (folder/'QualityShadow1932.java').write_text(shadow)
    (folder/'GpuInteger1949.java').write_text(r"""package com.hiro.ulike;
final class GpuInteger1949 {
 static int calls;static boolean available(){return true;}
 interface CpuFinish{boolean run(int[] out);}interface CpuFinishInto{boolean run(int[] out,int offset);}interface CpuAggregate{boolean run(int[] out);}
 static boolean finish(int[] input,int[] meta,int[] policy,int[] out,int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,boolean shadows,int global,int beauty,int smoothLimit,CpuFinish cpu){calls++;return false;}
 static boolean finishInto(int[] input,int[] meta,int[] policy,int[] out,int offset,int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,boolean shadows,int global,int beauty,int smoothLimit,CpuFinishInto cpu){calls++;return false;}
 static boolean finishSavedInto(int[] input,int[] meta,int[] policy,int[] out,int offset,int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,boolean shadows,int global,int beauty,int smoothLimit,CpuFinishInto cpu){calls++;return false;}
 static boolean aggregateAvailable(int w,int rows,int begin,int end,int lo,int hi,int radius){return false;}
 static boolean aggregate(int[] input,int[] meta,int[] out,int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,CpuAggregate cpu){return false;}
}""")
    (folder/'GpuFallback1950Test.java').write_text(r"""package com.hiro.ulike;
import java.util.Arrays;import java.util.Random;
public final class GpuFallback1950Test {
 public static void main(String[] args){
  int checks=0,w=53,h=270;Random rng=new Random(1950);int[] input=new int[w*h];
  for(int i=0;i<input.length;i++){int c=80+rng.nextInt(40);input[i]=0xff000000|(c<<16)|(c<<8)|c;}
  QualityPixels1932.Plan plan=QualityPixels1932.plan(new QualityPixels1932.NoiseStats(7,7,128,0,4096),400,10000000L,2,0,4,0,true,true,1).withLocalNoise(null,4);
  for(boolean nullPlan:new boolean[]{false,true}){
   int[] expected=input.clone(),actual=input.clone();for(int i=0;i<actual.length;i++)if(i%31==0)expected[i]=actual[i]=0x31415926;
   QualityPixels1932.Plan p=nullPlan?null:plan;
   QualityShadowReference1944.smoothRange(input,expected,w,h,0,h,0,h,4,true,4,p,0);
   QualityShadow1932.TEST_META=0;GpuInteger1949.calls=0;
   QualityShadow1932.smoothRange(input,actual,w,h,0,h,0,h,4,true,4,p,0);
   if(!Arrays.equals(actual,expected))throw new AssertionError("disabled-GPU fallback pixel mismatch");
   if(QualityShadow1932.TEST_META!=input.length)throw new AssertionError("metadata repeated: "+QualityShadow1932.TEST_META+" vs "+input.length);
   if(GpuInteger1949.calls!=(nullPlan?0:3))throw new AssertionError("batch count "+GpuInteger1949.calls);
   checks+=input.length+2;
  }
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"metadata_prepared_once\":true,\"null_plan_cpu_path_verified\":true}");
 }
}""")
    native=root/'native1944'
    run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),native/'kernels1935.c',native/'jni1935.c',native/'residual1944.c',native/'jni1944.c','-o',folder/'libulike_speed1935.so'])
    names=['QualityPixels1932.java','NativeMoire1951.java','PolicyCache1945.java','NoiseCache1944.java','NativeSpeed1944.java','NativeSpeed1935.java','SpatialNoise1934.java','LongMoire1934.java','SpeedWorkers1935.java','cache1945-reference/QualityShadowReference1944.java','tests/noise-fixtures/com/hiro/ulike/QualityPipeline1932.java']
    if (root/'NativeCore1950.java').exists():names.append('NativeCore1950.java')
    run([jdk/'bin/javac','-d',classes,*[folder/name for name in ['QualityShadow1932.java','GpuInteger1949.java','GpuFallback1950Test.java']],*[root/name for name in names]])
    output=run([jdk/'bin/java','-Xcheck:jni','-Djava.library.path='+str(folder),'-cp',classes,'com.hiro.ulike.GpuFallback1950Test'])
    (folder/'gpu-fallback1950-java.txt').write_text(output)
    if 'WARNING' in output or 'FATAL ERROR' in output:raise RuntimeError('Fallback JNI checker rejected:\n'+output)
    return json.loads(output.strip())

def test(root,work,android=None):
    root=pathlib.Path(root).resolve();work=pathlib.Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    toolroot=pathlib.Path('/workspace/scratch/2537a200dd65')
    jdk=pathlib.Path(os.environ.get('ULIKE_JDK_HOME') or os.environ.get('JAVA_HOME') or toolroot/'jdk21').resolve()
    ndk=pathlib.Path(os.environ.get('ULIKE_NDK_HOME') or toolroot/'ndk27c').resolve();source=root/'native1949'
    if not (jdk/'bin/javac').is_file() or not (jdk/'include/jni.h').is_file():raise RuntimeError('Pinned JDK path required for GPU host JNI test')
    if not (ndk/'source.properties').is_file() or 'Pkg.Revision = 27.2.12479018' not in (ndk/'source.properties').read_text():raise RuntimeError('Pinned Android NDK r27c path/revision required for GPU host test')
    spec=importlib.util.spec_from_file_location('gpu_jni_oracle1949',root/'host_gpu_jni1949.py');old=importlib.util.module_from_spec(spec);spec.loader.exec_module(old)
    spec=importlib.util.spec_from_file_location('gpu_builder1949',source/'build_native1949.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(source,work)
    headers=work/'headers';headers.mkdir(exist_ok=True)
    for name in ['EGL','GLES3','KHR']:shutil.copytree(ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'/name,headers/name,dirs_exist_ok=True)
    adapter=work/'gpu-finish-host-adapter.c';adapter.write_text(old.ADAPTER.replace('GpuNative1949Test','GpuFinish1950Test'))
    java=work/'GpuFinish1950Test.java';java.write_text(JAVA);classes=work/'classes';classes.mkdir(exist_ok=True)
    run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-DULIKE_RESIDUAL_SCALAR','-DANDROID','-Wl,--no-undefined','-Wl,-Bsymbolic-functions','-pthread','-I'+str(headers),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),'-I'+str(root/'native1944'),source/'gpu1949.c',root/'native1944/residual1944.c',adapter,'-l:libEGL.so.1','-l:libGL.so.1','-o',work/'libulike_gpu1949.so'])
    run([jdk/'bin/javac','-d',classes,root/'GpuInteger1949.java',java])
    env=os.environ.copy();env['EGL_PLATFORM']='surfaceless';env['LIBGL_ALWAYS_SOFTWARE']='1'
    output=run([jdk/'bin/java','-Xcheck:jni','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.GpuFinish1950Test'],env)
    (work/'gpu-finish1950-java.txt').write_text(output)
    if 'WARNING' in output or 'FATAL ERROR' in output:raise RuntimeError('JNI checker rejected:\n'+output)
    report=json.loads(output.strip());fallback=fallback_test(root,work,jdk);report['assertions']+=fallback['assertions'];report['disabled_gpu_fallback']=fallback;report.update(direct_destination_slice_commit=True,destination_region_commit=True,extra_java_seed_and_copyback_removed=True,cpu_summary_reused_per_strip=True,schema='ulike-fused-finish1950-host-v1',pixel_equivalence_to_baseline=True,fused_pixel_output=True,summary_readback_removed=True,batching_rows=128,native_workspace_reuse=True,immutable_lut_value_reuse=True,inactive_destination_and_pooled_tail_preserved=True,foreign_egl_context_preserved=True,bounded_timeout_without_readback=True,jni_runtime_checker_passed=True,software_egl_shaders_executed=True,physical_android_tested=False,device_speed_measured=False)
    report['sources']={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in [root/'QualityShadow1932.java',root/'GpuInteger1949.java',source/'gpu1949.c',source/'residual_finish1949.comp',pathlib.Path(__file__).resolve()]}
    (work/'host-gpu-finish1950.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--work',required=True);p.add_argument('--android');args=p.parse_args();print(json.dumps(test(pathlib.Path(__file__).resolve().parent,args.work,args.android)))
