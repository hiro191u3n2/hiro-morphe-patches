package com.hiro.ulike;

import java.lang.reflect.Method;
import java.util.Arrays;

/** Real JNI foreground readback timing; values are diagnostic, pixels stay exact. */
public final class StrongCollect19117Test {
    private static int assertions;
    private static void check(boolean condition,String label){assertions++;if(!condition)throw new AssertionError(label);}
    private static void observed(boolean injectFault)throws Exception {
        StrongTransport1995Test.store();
        check(StrongNoise1958.nativeAvailable(),"native CPU oracle present");
        check(GpuStrongDefault1990.qualify(StrongTransport1995Test.GO),"real startup qualification");
        check(GpuStrongDefault1990.ready(),"startup GPU route ready");
        GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.fixture(63);
        int[] expectedCf=StrongTransport1995Test.confidence(f);
        int[] expected=StrongTransport1995Test.expected(f,expectedCf);
        int[] original=f.source.clone();
        ProcessingTiming1947.Trace trace=new ProcessingTiming1947.Trace(19117L,System.currentTimeMillis(),System.nanoTime());
        ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(trace);
        Object stage;
        try{stage=StrongTransport1995Test.stage(f.model,false);}
        finally{ProcessingTiming1947.restore(scope);}
        check(StrongTransport1995Test.field(stage,"trace")==trace,"exact foreground trace bound to Stage");
        int calls=injectFault?1:2;
        try{
            if(injectFault)GpuTransportFault1995.arm(1);
            for(int i=0;i<calls;i++){
                int[] actual=StrongTransport1995Test.output(f);
                int[] confidence=StrongTransport1995Test.confidence(f);
                if(injectFault){
                    final int[] u=f.u;
                    StrongNoise1958.Protection protection=new StrongNoise1958.Protection(){
                        public int budgetQ8(int x,int y){return f.policy[((y-u[6]-u[2])*u[0]+x)*2];}
                        public int detailQ8(int x,int y){return f.policy[((y-u[6]-u[2])*u[0]+x)*2+1];}
                    };
                    StrongNoise1958.Workspace workspace=new StrongNoise1958.Workspace(true);
                    try{
                        StrongNoise1958.processRange(f.source,actual,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[8],u[9]!=0,
                            f.model,protection,workspace);
                        confidence=workspace.confidence().clone();
                    }finally{workspace.close();}
                    check(GpuTransportFault1995.fired==1,"post-JNI fault reaches observed route");
                }else check(StrongTransport1995Test.run(f,actual,confidence),"certified GPU run");
                check(Arrays.equals(expected,actual),"complete output, including halo and untouched tail");
                check(Arrays.equals(expectedCf,confidence),"confidence output");
                check(Arrays.equals(original,f.source),"source unchanged");
            }
        }finally{GpuTransportFault1995.clear();GpuStrong1960.endStage(f.model);}
        check(trace.strongCollectCount19117==calls,"one observed collect per Strong attempt");
        long[] metrics=trace.strongCollect19117;
        check(metrics[0]>=metrics[1]&&metrics[1]>=0,"API includes first readiness and polling");
        check(metrics[2]>=0&&metrics[2]<=metrics[0],"bounded private owner queue");
        check(metrics[3]>=0&&metrics[4]>=0&&metrics[5]>0,"native fence/map/JNI timings delivered");
        Method render=ProcessingTiming1947.class.getDeclaredMethod("render",ProcessingTiming1947.Trace.class);
        render.setAccessible(true);String detail=(String)render.invoke(null,trace);
        check(detail.contains("強ノイズGPU結果取得の内訳")&&detail.contains("最終フェンス待ち")&&
            detail.contains("GLマップ・解除")&&detail.contains("JNI配列確保・書込み"),"bounded diagnostic labels rendered");
        check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"GPU session drained");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+
            ",\"observed_collects\":"+trace.strongCollectCount19117+",\"fault\":"+injectFault+
            ",\"pixels_exact\":true,\"actual_jni\":true,\"physical_android_tested\":false}");
    }
    public static void main(String[] args)throws Exception{observed(args.length>0);}
}
