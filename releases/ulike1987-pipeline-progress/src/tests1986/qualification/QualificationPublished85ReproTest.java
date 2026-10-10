package com.hiro.ulike;

import java.lang.reflect.Method;

/** Same optional observer call as production, with published .85 and current
 * qualification: a baseline refusal must no longer erase candidate reasons. */
public final class QualificationPublished85ReproTest {
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        final boolean old="published85".equals(args[0]);QualificationProgress1984Test.reset();
        QualificationProgress1984Test.Probe probe=new QualificationProgress1984Test.Probe(c->{
            GpuQualification1961.progress1984("resident_baseline",0,1);
            try{Method observer=GpuQualification1961.class.getMethod("finishCandidate1986",int.class,int.class,int.class,int.class,int.class,long.class,long.class,long.class);
                observer.invoke(null,-1,8,0,0,1,1000000000L,0L,0L);
                observer.invoke(null,0,5,2,2,1,1000000000L,0L,960000000L);
            }catch(NoSuchMethodException optional){}catch(Exception failure){throw new AssertionError(failure);}
            GpuQualification1961.outcome1984("baseline_unavailable");
        });
        check(GpuQualification1961.schedule("strong-resident1978:3:same-private-repro",1,probe),"same resident dependency scheduled");
        QualificationProgress1984Test.quiet();QualificationProgress1984Test.await(()->probe.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"same job completed");
        String text=GpuQualification1961.attemptSummary1985();
        try{text=(String)GpuQualification1961.class.getMethod("attemptSummary1986").invoke(null);}catch(NoSuchMethodException optional){}
        check(text.contains("前提となる認定が未成立")&&text.contains("連結の前提認定 0/1")&&text.contains("認定書込 0回"),"same parent result and no qualification writes on both versions");
        check(text.contains("保存済み画質不一致により対象外")==!old,"new display distinguishes old-baseline exact rejection");
        check(text.contains("速度条件を満たさず")==!old,"new display distinguishes new candidate speed miss");
        check(text.contains("CPU 1000ms・旧GPU 未取得・候補GPU 960ms")==!old,"new display records actual compared times without invented legacy measurement");
        check(!text.contains("same-private-repro")&&probe.runs.get()==1,"same private job identity stays out of display");
        QualificationProgress1984Test.reset();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"mode\":\""+args[0]+"\",\"qualification_published85_finish_ambiguity_reproduced1986\":"+old+",\"qualification_same_finish_reason_corrected1986\":"+!old+"}");
    }
}
