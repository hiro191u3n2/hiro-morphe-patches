package com.hiro.ulike;

import java.lang.reflect.Method;

/** Same state/lifecycle scenario executed against the shipped .84 and .85.
 * This is not a shader or physical-device performance test. */
public final class QualificationPublished84ReproTest {
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void selection(boolean current,String key,int profile,int variant,boolean invalidated){
        if(!current)return;
        try{GpuQualification1961.class.getMethod("selection1985",String.class,int.class,int.class,boolean.class)
            .invoke(null,key,profile,variant,invalidated);}catch(Exception failure){throw new AssertionError(failure);}
    }
    static GpuQualification1961.Reservation1984 reserve(boolean current,String key,long bytes)throws Exception{
        Method method=GpuQualification1961.class.getMethod(current?"reserve1985":"reserve1984",String.class,long.class);
        return (GpuQualification1961.Reservation1984)method.invoke(null,key,bytes);
    }
    public static void main(String[] args)throws Exception{
        final boolean current=args.length==1&&"current85".equals(args[0]);
        QualificationProgress1984Test.reset();
        final String group=QualificationProgress1984Test.group("published-observation");
        final String child="strong-gx1973-ieee-div-policy-bank-v1:3:published-observation";
        QualificationProgress1984Test.Probe proof=new QualificationProgress1984Test.Probe(c->{
            for(int i=0;i<8;i++)GpuQualification1961.qualifiedStrongProfile1975(group,child,100,80,0);
            GpuQualification1961.qualifiedStrongPreferred1970(group,100,80,0);
            GpuQualification1961.progress1984("strong_profile",8,8);
            GpuQualification1961.outcome1984("strong_qualified");
            selection(current,child,0,0,false);
            GpuQualification1961.rejectExact(child);
            // Deliberately omit the second selection callback: worker finish
            // must independently observe that the selected child disappeared.
        });
        check(GpuQualification1961.schedule(group,1,proof),"proof scenario admitted before certificates exist");
        QualificationProgress1984Test.quiet();
        QualificationProgress1984Test.await(()->proof.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"proof ended");
        check(proof.runs.get()==1&&GpuQualification1961.restore(child)==null,"later exact rejection really removed the selected child");
        check(GpuQualification1961.restore(group)!=null,"stale aggregate alone survives this scenario");
        if(current){
            String text=(String)GpuQualification1961.class.getMethod("attemptSummary1985").invoke(null);
            check(text.contains("候補確認 8/8（確認一巡）"),"scan completion has a distinct label");
            check(text.contains("認定書込 9回"),"nine writes remain nine operations, including repeats");
            check(text.contains("終了時採用候補: 失効"),"end recheck exposes lost child despite nine writes");
        }else{
            String text=GpuQualification1961.attemptSummary1984();
            check(text.contains("認定記録を作成"),"published ledger retained the earlier success label");
            check(text.contains("Strong候補の比較 8/8"),"published scan counter is reproduced");
            check(text.contains("認定記録 9件")&&!text.contains("終了時採用候補"),"published count does not expose final selection loss");
        }
        QualificationProgress1984Test.reset();
        long bytes=64L*1024*1024;
        GpuQualification1961.Reservation1984 old=reserve(current,"strong-resident1978:3:unstarted-old",bytes);
        check(old.accepted()&&GpuQualification1961.retainedBytes()==bytes,"unstarted reservation owns budget");
        GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
        check(!old.current(),"old capture is cancelled");
        check(GpuQualification1961.retainedBytes()==(current?0:bytes),"published retention or new pre-begin revocation reproduced");
        GpuQualification1961.Reservation1984 next=reserve(current,"strong-resident1978:3:unstarted-new",bytes);
        check(next.accepted()==current,"only revoked pre-begin reservation frees capacity for next capture");
        old.close();
        check(GpuQualification1961.retainedBytes()==(current?bytes:0),"late old close never releases the new owner's budget");
        next.close();old.close();
        check(GpuQualification1961.retainedBytes()==0,"all owners close without underflow");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"mode\":\""+(current?"current85":"published84")+"\",\"qualification_published84_ambiguity_reproduced1985\":"+(!current)+",\"qualification_same_scenario_corrected1985\":"+current+"}");
    }
}
