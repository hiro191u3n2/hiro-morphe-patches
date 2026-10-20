package com.hiro.ulike;
import java.util.Arrays;

/** Actual JNI/GLES queue and integer verdict tests; no physical speed assertions. */
final class Batch1961Test {
    private static long assertions;
    private static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private static native long[] facts();
    private static native void setFault(int mode);
    private static GpuNoise1960.Batch comparison(int first,int second,int verdict,int[] a,int[] b,int shader){
        int[] u=new int[32];u[0]=a.length;
        return new GpuNoise1960.Batch().upload(first,a).upload(second,b).upload(verdict,new int[]{0})
            .dispatch(shader,new int[]{first,second,verdict},u,null,a.length);
    }
    private static void fault(String mode){
        check(GpuNoise1960.available()&&GpuNoise1960.supports(GpuNoise1960.COMPARE1961),"fault actual GPU program");
        long beforeDispatch=facts()[3];int[] pixels={11,22,33,44};GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"fault owner session");
        try{
            if(mode.equals("unmap")){
                long beforeMaps=facts()[1];setFault(4);
                int[][] candidate=s.execute(comparison(0,1,2,pixels,pixels,GpuNoise1960.COMPARE1961),new int[]{0,1,2},new int[]{4,4,1});
                setFault(0);check(candidate==null,"late second-output unmap discards whole candidate");
                check(facts()[1]-beforeMaps==2,"first output copied and second output failed");
                s.close();check(GpuNoise1960.retainedBytes()==0,"known-complete failed batch releases pool");
                GpuNoise1960.Session fresh=GpuNoise1960.open();check(fresh!=null,"owner reusable after drained failure");fresh.close();
            }else if(mode.equals("timeout")){
                GpuNoise1960.Ticket a=s.submit(comparison(0,1,3,pixels,pixels,GpuNoise1960.COMPARE1961),0);
                GpuNoise1960.Ticket b=s.submit(comparison(14,15,16,pixels,pixels,GpuNoise1960.COMPARE1961),1);
                check(a!=null&&b!=null,"pending two-bank GPU work");long retained=GpuNoise1960.retainedBytes(),maps=facts()[1];
                setFault(1);s.close();setFault(0);
                check(GpuNoise1960.retainedBytes()==retained&&retained>0,"unknown two-bank completion keeps exact memory accounting");
                check(facts()[1]==maps,"unknown completion cannot map output");check(GpuNoise1960.open()==null,"quarantined GPU owner never reused");
                GpuNoise1960.trimIdle();check(GpuNoise1960.retainedBytes()==retained,"trim cannot free unproven bank leases");
            }else throw new IllegalArgumentException("batch fault mode");
        }finally{setFault(0);s.close();}
        long dispatches=facts()[3]-beforeDispatch;check(dispatches>0,"fault exercised actual compute dispatch");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"mode\":\""+mode+"\",\"actualDispatches\":"+dispatches+",\"physicalAndroidTested\":false}");
    }
    public static void main(String[] args){
        if(args.length==2&&args[0].equals("fault")){fault(args[1]);return;}
        check(GpuNoise1960.available(),"actual native library");
        int n=257;int[] expected=new int[n];for(int i=0;i<n;i++)expected[i]=i*16777619^0x12345678;
        long[] start=facts();
        for(int v=0;v<3;v++){
            int shader=GpuNoise1960.variant(GpuNoise1960.COMPARE1961,v);
            check(GpuNoise1960.supports(shader),"actual compare variant compile "+v);
            check(GpuNoise1960.workgroup(shader)==(v==0?64:v==1?32:128),"linked workgroup "+v);
            GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"variant session");
            try{
                long before=facts()[0];
                int[][] result=s.execute(comparison(0,1,2,expected,expected,shader),new int[]{0,1,2},new int[]{n,n,1});
                check(result!=null&&result[2][0]==0,"every word matched");
                check(Arrays.equals(expected,result[0])&&Arrays.equals(expected,result[1]),"all private readbacks exact");
                check(facts()[0]-before==1,"one GPU completion wait for three outputs");
                for(int mismatch:new int[]{0,n/2,n-1}){
                    int[] changed=expected.clone();changed[mismatch]^=1;
                    result=s.execute(comparison(0,1,2,expected,changed,shader),new int[]{2},new int[]{1});
                    check(result!=null&&result[0][0]==1,"mismatch word covered "+mismatch);
                }
                long beforeClose=facts()[0];s.close();check(facts()[0]==beforeClose,"known completion skips duplicate close wait");
            }finally{s.close();}
            check(GpuNoise1960.retainedBytes()>0&&GpuNoise1960.retainedBytes()<=128L*1024*1024,"bounded known-complete pool counted");
            GpuNoise1960.Session fresh=GpuNoise1960.open();check(fresh!=null,"cached capacity new session");
            try{int[] untouched=new int[32];untouched[0]=n;
                check(!fresh.dispatch(shader,new int[]{0,1,2},untouched,null,n),"new photo cannot bind unallocated previous-photo cache");
            }finally{fresh.close();}
            GpuNoise1960.trimIdle();check(GpuNoise1960.retainedBytes()==0,"idle pool released");
        }
        GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"two-bank session");
        try{
            int[] a=expected.clone(),b=expected.clone(),bad=expected.clone();bad[n-1]^=1;
            GpuNoise1960.Ticket t0=session.submit(comparison(0,1,3,a,b,GpuNoise1960.COMPARE1961),0);
            GpuNoise1960.Ticket t1=session.submit(comparison(14,15,16,expected,bad,GpuNoise1960.COMPARE1961),1);
            check(t0!=null&&t1!=null,"two banks submitted before any collect");
            check(session.submit(comparison(0,1,3,a,b,GpuNoise1960.COMPARE1961),0)==null,"busy bank cannot be reused");
            Arrays.fill(a,-1);Arrays.fill(b,0);Arrays.fill(bad,0); // JNI snapshots already own exact bytes.
            int[][] r0=session.collect(t0,new int[]{0,1,3},new int[]{n,n,1});
            int[][] r1=session.collect(t1,new int[]{16},new int[]{1});
            check(r0!=null&&r0[2][0]==0&&Arrays.equals(expected,r0[0])&&Arrays.equals(expected,r0[1]),"submission snapshots survive caller reuse");
            check(r1!=null&&r1[0][0]==1,"second bank independent result");
            check(session.collect(t0,new int[]{3},new int[]{1})==null,"ticket can only be collected once");
            GpuNoise1960.Ticket next=session.submit(comparison(0,1,3,expected,expected,GpuNoise1960.COMPARE1961),0);
            check(next!=null,"completed bank reusable");
            Thread.currentThread().interrupt();try{session.close();check(Thread.currentThread().isInterrupted(),"close drains pending bank and preserves interrupt");}finally{Thread.interrupted();}
        }finally{session.close();}
        check(!GpuNoise1960.sessionBusy(),"closed owner released");GpuNoise1960.trimIdle();check(GpuNoise1960.retainedBytes()==0,"all idle retention cleared");
        long[] finish=facts();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actualVariants\":[64,32,128],\"asyncBanks\":2,\"observedCompletionWaits\":"+(finish[0]-start[0])+",\"physicalAndroidTested\":false}");
    }
}
