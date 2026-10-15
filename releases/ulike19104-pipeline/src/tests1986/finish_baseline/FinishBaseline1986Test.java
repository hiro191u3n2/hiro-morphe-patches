package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Execute the real qualification control flow, complete ResidentProof rows,
 * ownership and key handoff. Renderer results/times are explicit host inputs. */
public final class FinishBaseline1986Test {
    static int assertions;static boolean baseline;
    static final int WIDTH=37,HEIGHT=23;
    static final Map<String,Integer> cases=new LinkedHashMap<String,Integer>();
    static final GpuQualification1961.Cancellation cancellation=()->FinishControl1986.cancelled();
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static void section(String name,int start){cases.put(name,assertions-start);}
    static QualityPixels1932.Plan plan(){
        return QualityPixels1932.plan(null,800,16666666L,0,0,4,4,true,true,1.2f).withHaloSuppression(true);
    }
    static String key(Bitmap source,QualityPixels1932.Plan plan)throws Exception{
        Method method=GpuChain1961.class.getDeclaredMethod("finishKey1976",Bitmap.class,int.class,int.class,int.class,QualityPixels1932.Plan.class,boolean.class);
        method.setAccessible(true);return (String)method.invoke(null,source,0,WIDTH,HEIGHT,plan,true);
    }
    static String legacy(String key){return key.replace("|finish-chain1976:v2|","|finish-chain1962:v1|");}
    static Bitmap source(){return FinishControl1986.image(29,19,false);}
    static void run(Bitmap source,QualityPixels1932.Plan plan){
        try{GpuChain1961.qualifyFinish1978(source,0,WIDTH,HEIGHT,plan,true,cancellation);}
        catch(java.util.concurrent.CancellationException expected){if(FinishControl1986.cancelMode==0&&!hasCancelFault())throw expected;}
        catch(RuntimeException|LinkageError|OutOfMemoryError oldAbort){if(!baseline)throw oldAbort;}
    }
    static boolean hasCancelFault(){for(int[] a:FinishControl1986.compareFault)for(int n:a)if(n==6)return true;for(int[] a:FinishControl1986.outputFault)for(int n:a)if(n==6)return true;return false;}
    static void clean(Bitmap original){
        check(!original.isRecycled(),"qualification leaves the caller-owned snapshot live");
        int[] expected=new int[29*19];for(int y=0;y<19;y++)for(int x=0;x<29;x++)expected[y*29+x]=FinishControl1986.pixel(x,y);
        check(Arrays.equals(expected,original.snapshot()),"qualification does not mutate source pixels");
        for(Bitmap b:Bitmap.ALL)if(b!=original)check(b.isRecycled(),"every temporary CPU/GPU Bitmap is recycled");
        original.recycle();Bitmap.ALL.clear();Thread.interrupted();
    }
    static void legacyLoss()throws Exception{
        int start=assertions;
        for(int fault=1;fault<=5;fault++){
            FinishControl1986.reset();Bitmap b=source();QualityPixels1932.Plan p=plan();String key=key(b,p);
            FinishControl1986.compareFault[0][0]=fault;run(b,p);
            FinishControl1986.Proof proof=FinishControl1986.proofs.get(key);
            check((proof!=null)!=baseline,"CPU-faster exact candidates survive legacy failure mode "+fault);
            if(!baseline){
                check(proof.variant==1&&proof.cpu==1000&&proof.gpu==700,"fallback records the measured CPU and selected real output times");
                for(int i=1;i<4;i++){check(FinishControl1986.comparisons[i]==2&&FinishControl1986.outputs[i]==2,"both candidate comparisons and output runs execute");
                    check(FinishControl1986.comparedPixels[i]==2L*WIDTH*HEIGHT,"both complete candidate outputs are compared");}
                check(FinishControl1986.last(1)[4]==1,"CPU fallback reference is explicit");
            }
            if(fault==2)check(FinishControl1986.exactRejections.contains(legacy(key)),"real legacy mismatch remains permanently rejected");
            clean(b);
        }
        section(baseline?"published85_legacy_failure_strands_exact_candidates":"legacy_failure_uses_two_complete_cpu_proofs",start);
    }
    static void selectors()throws Exception{
        int start=assertions;boolean[] all={true,true,true};
        check(GpuChain1961.chooseFinish1976(1000,800,new long[]{850,900,950},all,true)==-1,"old dual-baseline gate remains strict");
        check(GpuChain1961.chooseFinish1976(1000,800,new long[]{1,1,1},all,false)==-1,"old missing-legacy helper contract stays unchanged");
        if(!baseline){
            Method method=GpuChain1961.class.getDeclaredMethod("chooseFinish1986",long.class,long.class,long[].class,boolean[].class,boolean.class);
            long[] cpus={0,1,19,20,1000,Long.MAX_VALUE/4};
            for(long cpu:cpus)for(long gpu:new long[]{0,1,19,20,950,951}){
                int actual=(Integer)method.invoke(null,cpu,0,new long[]{gpu,0,0},new boolean[]{true,false,false},false);
                check((actual==0)==(cpu>0&&gpu>0&&gpu<=cpu-cpu/20),"exact integer five-percent CPU boundary");
            }
            check((Integer)method.invoke(null,1000L,800L,new long[]{850,900,950},all,true)==-1,"new entry delegates valid legacy comparison");
            check((Integer)method.invoke(null,1000L,0L,new long[]{1,1,1},all,true)==-1,"a declared usable but invalid legacy time cannot fail open");
        }
        section("selectors_and_unchanged_legacy_contract",start);
    }
    static void validLegacy()throws Exception{
        int start=assertions;
        for(int mode=0;mode<3;mode++){
            FinishControl1986.reset();Bitmap b=source();QualityPixels1932.Plan p=plan();String key=key(b,p);
            if(mode==0)FinishControl1986.gpuTimes=new long[][]{{800,800},{850,850},{900,900},{950,950}};
            if(mode==2)FinishControl1986.gpuTimes=new long[][]{{2000,2000},{950,950},{1000,1000},{1200,1200}};
            run(b,p);FinishControl1986.Proof proof=FinishControl1986.proofs.get(key);
            if(mode==0){check(proof==null,"CPU advantage cannot bypass an available faster legacy route");check(FinishControl1986.proofs.get(legacy(key))!=null,"valid legacy fallback remains usable");}
            else {check(proof!=null&&proof.variant==(mode==1?1:0),"valid legacy keeps the same selected candidate");check(proof.cpu==(mode==1?800:1000),"stored reference remains min actual CPU and legacy");}
            if(!baseline)check(FinishControl1986.last(0)[4]==2,"new candidates preserve both-reference diagnostic");
            clean(b);
        }
        section("valid_legacy_comparison_and_fallback_preserved",start);
    }
    static void currentFailures()throws Exception{
        int start=assertions;
        for(int mode=0;mode<9;mode++){
            FinishControl1986.reset();Bitmap b=source();QualityPixels1932.Plan p=plan();String key=key(b,p);
            FinishControl1986.compareFault[0][0]=1;
            if(mode==0){FinishControl1986.exactRejections.add(legacy(key));FinishControl1986.compareFault[0][0]=0;}
            if(mode==1)FinishControl1986.compareFault[1][0]=3;
            if(mode==2)FinishControl1986.compareFault[2][1]=2;
            if(mode==3)FinishControl1986.outputFault[2][1]=1;
            if(mode==4){FinishControl1986.cpuTimes=new long[]{1000,900};FinishControl1986.gpuTimes[2]=new long[]{800,860};FinishControl1986.gpuTimes[1]=new long[]{990,990};}
            if(mode==5)FinishControl1986.cpuTimes=new long[]{0,1000};
            if(mode==6)FinishControl1986.unstable=true;
            if(mode==7)FinishControl1986.workspace=false;
            if(mode==8){FinishControl1986.exactRejections.add(key);}
            run(b,p);FinishControl1986.Proof proof=FinishControl1986.proofs.get(key);
            if(mode<=1)check(proof!=null&&proof.variant==1,"independent rejection/unavailability leaves another eligible candidate");
            if(mode==0){check(FinishControl1986.comparisons[0]==0,"known legacy mismatch is never probed again");check(FinishControl1986.exactRejections.contains(legacy(key)),"known mismatch remains intact");check(FinishControl1986.last(-1)[1]==8,"saved exact rejection is distinct from current unavailable");}
            if(mode==1)check(FinishControl1986.last(0)[1]==3&&FinishControl1986.comparisons[2]==2,"one pipeline exception cannot stop an independent candidate");
            if(mode==2||mode==3){check(proof!=null&&proof.variant==0,"failed fastest candidate is excluded, slower exact candidate remains eligible");check(FinishControl1986.last(1)[1]==(mode==2?2:3),"second-trial failure has its actual cause");}
            if(mode>=4)check(proof==null,"unsafe or insufficient evidence never certifies mode "+mode);
            if(mode==4)check(FinishControl1986.last(1)[5]==900&&FinishControl1986.last(1)[7]==860&&FinishControl1986.last(1)[1]==5,"fastest CPU versus worst GPU is the measured speed gate");
            if(mode==5||mode==6)check(FinishControl1986.last(1)[1]==10,"invalid or unstable CPU reference is explicit");
            if(mode==6)check(FinishControl1986.cpuCalls==2&&FinishControl1986.comparisons[2]==1,"unstable second CPU output aborts before candidate readmission");
            if(mode==7)check(FinishControl1986.last(1)[1]==4&&FinishControl1986.comparisons[2]==0,"fixed digest workspace is admitted before allocation");
            if(mode==8)check(FinishControl1986.cpuCalls==0&&FinishControl1986.exactRejections.contains(key),"existing whole-finish exact rejection cannot be bypassed");
            clean(b);
        }
        section("independent_failures_stability_timing_and_rejections",start);
    }
    static void cancellationAndDiagnostics()throws Exception{
        int start=assertions;
        for(int mode=1;mode<=7;mode++){
            FinishControl1986.reset();Bitmap b=source();QualityPixels1932.Plan p=plan();FinishControl1986.cancelMode=mode;run(b,p);
            check(FinishControl1986.proofs.isEmpty(),"cancellation cannot create a certificate at boundary "+mode);
            check(FinishControl1986.exactRejections.isEmpty(),"cancellation never manufactures a mismatch");
            check(FinishControl1986.last(0)[1]==6,"cancelled candidate remains explicitly cancelled");clean(b);
        }
        for(int fault=1;fault<=3;fault++){
            FinishControl1986.reset();Bitmap b=source();QualityPixels1932.Plan p=plan();String key=key(b,p);
            FinishControl1986.compareFault[0][0]=1;FinishControl1986.diagnosticFault=fault;run(b,p);
            FinishControl1986.Proof proof=FinishControl1986.proofs.get(key);
            check(proof!=null&&proof.variant==1&&proof.cpu==1000&&proof.gpu==700,"optional diagnostic failure cannot change proof or time "+fault);
            check(FinishControl1986.comparedPixels[2]==2L*WIDTH*HEIGHT,"diagnostic failure cannot skip full comparison");clean(b);
        }
        section("cancellation_and_optional_diagnostics_do_not_admit_or_corrupt",start);
    }
    static void residentHandoff()throws Exception{
        int start=assertions;FinishControl1986.reset();Bitmap b=source();QualityPixels1932.Plan output=plan();
        QualityPixels1932.Plan masked=output.withSmoothedRegions(new QualityPixels1932.SmoothMask(){public int smoothingQ8(int x,int y){return 0;}});
        check(GpuChain1961.residentBaseline1978(b,0,WIDTH,HEIGHT,output,false)==null,"resident initially has no usable ordinary baseline");
        FinishControl1986.compareFault[0][0]=1;run(b,masked);
        String baseline=GpuChain1961.residentBaseline1978(b,0,WIDTH,HEIGHT,output,false);
        check("pipeline:1:1000:700".equals(baseline),"CPU-qualified ordinary finish becomes the existing resident baseline identity");
        check(GpuChain1961.residentBaseline1978(b,0,WIDTH+1,HEIGHT,output,false)==null,"proof does not cross geometry");
        check(GpuChain1961.residentBaseline1978(b,0,WIDTH,HEIGHT,output,true)==null,"proof does not cross identity/noise-refresh condition");
        Bitmap expected=FinishControl1986.image(WIDTH,HEIGHT,false);
        FinishControl1986.comparisons[2]=0;
        check(GpuChain1961.compareBenchmark1978(b,0,WIDTH,HEIGHT,masked,true,expected,cancellation)==ResidentProof1978.EXACT,"existing resident comparison consumes the same fully qualified ordinary route");
        expected.recycle();FinishControl1986.outputs[2]=0;
        Bitmap benchmark=GpuChain1961.benchmarkFinish1978(b,0,WIDTH,HEIGHT,masked,true);
        check(benchmark!=null&&benchmark.getWidth()==WIDTH&&benchmark.getHeight()==HEIGHT,"existing resident speed baseline produces a real complete destination");benchmark.recycle();
        check(FinishControl1986.proofs.size()==1,"handoff does not invent a resident certificate");
        clean(b);section("ordinary_cpu_proof_handoff_preserves_resident_full_proof_requirement",start);
    }
    public static void main(String[] args)throws Exception{
        baseline=args.length>0&&args[0].equals("published85");selectors();legacyLoss();validLegacy();
        if(!baseline){currentFailures();cancellationAndDiagnostics();residentHandoff();}
        StringBuilder text=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(assertions).append(",\"cases\":{");boolean comma=false;
        for(Map.Entry<String,Integer> entry:cases.entrySet()){if(comma)text.append(',');comma=true;text.append('"').append(entry.getKey()).append("\":").append(entry.getValue());}
        java.lang.System.out.println(text.append("}}").toString());
    }
}
