package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/** Published .82 and current .83 execute in separate JVMs. Android Bitmap is
 * an explicit host holder; geometry planning, GPU ledger, JNI allocations and
 * CPU uploads are actual production code. No Android timing claim is made. */
public final class MemoryBudget1983Test {
    static final long M=1024L*1024;
    static final int SW=3060,SH=4080,W=4284,H=5712;
    static final long N=(long)SW*SH,O=(long)W*H;
    static int assertions;
    static long oldBudget,newBudget;
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static int leases()throws Exception{return ((List<?>)field(GpuNoise1960.class,"leases1971").get(null)).size();}
    static long rounded(long bytes){return bytes==0?0:(bytes+4095L)&~4095L;}
    static long budget(long[] capacities,long[] uploads,long javaBytes){
        long nativeBytes=0,staging=0,temporary=0;
        for(int i=0;i<capacities.length;i++){
            nativeBytes+=rounded(capacities[i]);long upload=uploads[i];
            if(upload<=64*M)staging=Math.max(staging,rounded(upload));else temporary=Math.max(temporary,upload);
        }
        return nativeBytes+staging+temporary+javaBytes;
    }
    static long freePhysical(){
        Runtime r=Runtime.getRuntime();return r.maxMemory()-(r.totalMemory()-r.freeMemory())-
            GpuNoise1960.retainedBytes()-SpeedWorkers1935.nativeRetainedBytes1956()-64*M;
    }
    static GpuNoise1960.Session open(){GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"actual production JNI session opens");return s;}
    static void empty(String label)throws Exception{check(leases()==0&&GpuNoise1960.reservedBytes1971()==0,label);}
    static Bitmap image(){return Bitmap.createBitmap(SW,SH,Bitmap.Config.ARGB_8888);}
    static GpuChain1961.Preflight1981 plan(Bitmap image){return GpuChain1961.preflightResident1981(image,0,W,H,(int)(N/4));}

    static void baseline()throws Exception{
        Bitmap input=image();
        GpuNoise1960.Session s=open();check(s.capacity1976()[26]>=4*O,"host driver can hold the real 24.47MP output");s.close();
        GpuPolicy1960.GeometryData data=GpuPolicy1960.geometry(SW,SH,0,W,H);
        long band=(long)W*64;
        long[] lower={4*N,4*N,4*O,16*band,16*band,4*band,4*band,4,4,4L*data.weights.length,4L*data.tables.length};
        long javaPeak=8*O+48*band+16*M+4L*(data.weights.length+data.tables.length);
        oldBudget=budget(lower,lower,javaPeak);
        check(oldBudget>GpuNoise1960.MAX_BYTES,"published plan exceeds fixed 512MiB even with one-word geometry scratch");
        check(!GpuNoise1960.planFits1981(lower,javaPeak),"actual published forecast refuses this lower bound");
        check(plan(input)==null,"actual published Strong-to-finish preflight refuses logged dimensions");
        empty("published refusal owns no lease");input.recycle();
    }

    static void forecast()throws Exception{
        Bitmap input=image();GpuChain1961.Preflight1981 p=plan(input);
        check(p!=null,"actual new preflight admits logged dimensions with sufficient real budget");
        try{
            oldBudget=budget(p.capacities,p.capacities,p.javaPeak);
            newBudget=budget(p.capacities,p.uploadBytes1983,p.javaPeak);
            check(oldBudget>GpuNoise1960.MAX_BYTES&&newBudget<GpuNoise1960.MAX_BYTES,"only transfer-aware ledger crosses the fixed ceiling");
            check(oldBudget-newBudget==4*O,"exactly the phantom 24.47MP CPU upload is absent");
            check(!GpuNoise1960.planFits1981(p.capacities,p.javaPeak),"unchanged legacy API retains conservative rejection");
            check(GpuNoise1960.planFits1983(p.capacities,p.uploadBytes1983,p.javaPeak),"explicit generated-output forecast passes");
            long[] realUpload=p.uploadBytes1983.clone();
            for(int i=0;i<p.slots.length;i++)if(p.slots[i]==2)realUpload[i]=p.capacities[i];
            check(!GpuNoise1960.planFits1983(p.capacities,realUpload,p.javaPeak),"a real full output upload must still fail the fixed combined budget");
            for(int i=0;i<p.slots.length;i++){
                int slot=p.slots[i];
                if(slot==24)check(p.uploadBytes1983[i]==4*N,"CPU Strong fallback rows remain reserved");
                if(slot==1||slot==2||slot==17||slot==18)check(p.uploadBytes1983[i]==0,"GPU-generated storage has no fictional CPU transfer");
                if(slot==3||slot==8||slot==6||slot==7)check(p.uploadBytes1983[i]==p.capacities[i],"real policy/table transfers retain staged fallback budget");
            }
            WholeRoute1953.retained=Math.max(0,freePhysical()-newBudget+8*M);
            check(!GpuNoise1960.planFits1983(p.capacities,p.uploadBytes1983,p.javaPeak),"actual other-owner memory pressure still rejects the new forecast");
            WholeRoute1953.retained=0;
            GpuNoise1960.Session s=open();
            try{
                check(p.bind(s),"actual session lease agrees with admitted forecast");
                check(p.lease!=null&&p.lease.temporary==0&&p.lease.staging==rounded(4*N),"runtime lease retains true source staging and no output upload");
                check(GpuNoise1960.reservedBytes1971()==newBudget,"runtime scalar reservation equals independently computed forecast");
                check(!p.bind(s),"one preflight cannot bind twice");
                check(s.reserveCapacity1983(p.slots,p.capacities,realUpload,0)==null,"a later real output upload cannot evade the already-owned Java peak");
                check(leases()==1&&p.lease.revalidate1971(),"declined upload lease leaves existing complete transaction intact");
                WholeRoute1953.retained=Long.MAX_VALUE;
                check(!p.lease.revalidate1971(),"physical memory is rechecked while route owns its forecast");
                WholeRoute1953.retained=0;
                check(p.lease.revalidate1971(),"same lease can revalidate after unrelated owner releases memory");
                p.finishAdmission(s);empty("joined Strong phase releases only forecast before finish leases take ownership");
                check(p.lease==null,"phase transfer removes stale forecast reference");
            }finally{WholeRoute1953.retained=0;s.close();}
        }finally{WholeRoute1953.retained=0;p.close();p.close();}
        empty("preflight and session close are idempotent");
        GpuChain1961.Preflight1981 fresh=plan(input);check(fresh!=null,"closed preflight clears capture-local ThreadLocal");fresh.close();
        input.recycle();
    }

    static void invalidAndRollback()throws Exception{
        check(!GpuNoise1960.planFits1983(new long[]{4},null,0),"null upload declaration rejected");
        check(!GpuNoise1960.planFits1983(new long[]{4},new long[0],0),"missing slot upload declaration rejected");
        check(!GpuNoise1960.planFits1983(new long[]{4},new long[]{-1},0),"negative upload rejected");
        check(!GpuNoise1960.planFits1983(new long[]{4},new long[]{8},0),"upload cannot exceed its capacity");
        GpuNoise1960.Session s=open();
        try{
            check(s.reserveCapacity1983(new int[]{0},new long[]{4},new long[]{8},0)==null,"runtime rejects upload beyond target");
            check(s.reserveCapacity1983(new int[]{0,0},new long[]{4,4},new long[]{0,0},0)==null,"explicit API preserves duplicate-slot rejection");
            check(s.reserveCapacity1983(new int[]{0},new long[]{4},new long[]{0},-1)==null,"future Java bytes cannot be negative");
            check(s.reserveCapacity1983(new int[]{0},new long[]{GpuNoise1960.MAX_BYTES+1},new long[]{0},0)==null,"GPU-generated output cannot evade fixed capacity ceiling");
            empty("invalid declarations leave no ownership");
            for(int kind=0;kind<2;kind++){
                WholeRoute1953.fail=kind==0;WholeRoute1953.oom=kind==1;
                check(s.reserveCapacity1983(new int[]{0},new long[]{4},new long[]{0},0)==null,"injected physical query failure rejects precise lease");
                WholeRoute1953.fail=false;WholeRoute1953.oom=false;
                empty("failed admission rolls back unreachable precise lease");
            }
        }finally{WholeRoute1953.fail=false;WholeRoute1953.oom=false;s.close();}
        Bitmap input=image();GpuChain1961.Preflight1981 p=plan(input);check(p!=null,"plan available for cancellation");
        s=open();check(p.bind(s),"cancellation case has live precise forecast");
        Thread.currentThread().interrupt();
        try{check(!p.lease.revalidate1971(),"interrupted caller cannot admit dispatch");s.close();p.close();}
        finally{check(Thread.interrupted(),"close preserves caller cancellation status");}
        empty("cancelled session closes all precise leases");
        check(GpuNoise1960.MAX_BYTES==512*M,"fixed 512MiB limit remains unchanged");input.recycle();
    }

    static void actualMaterialization()throws Exception{
        GpuNoise1960.trimIdle();GpuNoise1960.Session s=open();
        final int words=1024*1024;final long bytes=4L*words,output=80*M,headers=512;
        int[] slots={0,2};long[] capacities={bytes,output},uploads={bytes,0};
        GpuNoise1960.Lease1971 lease=s.reserveCapacity1983(slots,capacities,uploads,2*bytes+headers);
        check(lease!=null,"actual input plus large generated output has precise ownership");
        try{
            capacities[0]=0;uploads[0]=0;slots[0]=24;
            int[] source=new int[words];source[0]=17;source[words-1]=91;
            check(s.upload(0,source),"reservation snapshots survive caller descriptor mutation");
            long staging=s.capacity1976()[25];check(staging==bytes,"actual JNI upload materializes true input staging");
            check(s.allocate(2,output),"large GPU-output capacity materializes without CPU upload");
            long[] actual=s.capacity1976();check(actual[2]==output&&actual[25]==staging,"native allocation creates no output-sized staging buffer");
            check(GpuNoise1960.reservedBytes1971()==2*bytes+headers,"materialized native output replaces capacity debt without erasing future Java readbacks");
            int[] first=s.readInts(0,words);check(first!=null&&first[0]==17&&first[words-1]==91,"real JNI readback preserves complete boundary values");
            check(lease.consumeJava1974(bytes)&&GpuNoise1960.reservedBytes1971()==bytes+headers,"first real Java readback is counted once and second stays reserved");
            int[] second=s.readInts(0,words);check(second!=null&&second[0]==17&&second[words-1]==91,"second real readback succeeds under original quality-independent contract");
            check(lease.consumeJava1974(bytes)&&GpuNoise1960.reservedBytes1971()==headers,"materialized readback retires only its future bytes");
            check(!lease.consumeJava1974(bytes),"duplicate consumption cannot erase residual allowance");
            check(lease.revalidate1971(),"actual native allocations and Java results still fit real heap");
        }finally{lease.close();s.close();}
        empty("actual allocation/upload/readback lifetime releases every lease");
    }

    static void uploadContract()throws Exception{
        for(int mode=0;mode<9;mode++){
            GpuNoise1960.trimIdle();GpuNoise1960.Session s=open();
            GpuNoise1960.Lease1971 lease=s.reserveCapacity1983(new int[]{0},new long[]{4096},new long[]{0},32);
            check(lease!=null&&s.allocate(0,4096),"generated destination exists before forbidden transfer mode "+mode);
            long staging=s.capacity1976()[25];int[] target={123};boolean accepted;
            if(mode==0)accepted=s.upload(0,new int[]{7});
            else if(mode==1)accepted=s.upload(0,new float[]{7});
            else if(mode==2)accepted=s.uploadRange1976(0,0,new int[]{7},0,1);
            else{
                GpuNoise1960.Batch batch=new GpuNoise1960.Batch();
                if(mode==7)batch.uploadDirect(0,new int[]{7});
                else if(mode==8)batch.uploadDirect(0,ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()));
                else batch.upload(0,new int[]{7});
                if(mode==4)accepted=s.execute(batch,new int[]{0},new int[]{1})!=null;
                else if(mode==5)accepted=s.executeInto(batch,0,1,target,0);
                else if(mode==6)accepted=s.submit(batch,0)!=null;
                else accepted=s.run(batch);
            }
            check(!accepted,"undeclared CPU transfer is rejected before JNI in mode "+mode);
            check(s.capacity1976()[25]==staging&&target[0]==123,"rejected transfer creates no staging and publishes no output");
            check(s.failureCode1971()==GpuNoise1960.FAILURE_ALLOCATION1971,"missing transfer allowance records memory-admission failure");
            s.close();lease.close();empty("rejected transfer closes its precise ownership");
        }
        GpuNoise1960.trimIdle();GpuNoise1960.Session s=open();
        GpuNoise1960.Lease1971 future=s.reserveCapacity1983(new int[]{0},new long[]{4096},new long[]{0},0);
        GpuNoise1960.Lease1971 model=s.reserveCapacity1971(new int[]{0},new long[]{16},0);
        try{check(future!=null&&model!=null&&s.upload(0,new int[]{3,5,7,11}),"separate legacy model lease authorizes its real transfer into a future reused slot");}
        finally{if(model!=null)model.close();if(future!=null)future.close();s.close();}
        empty("legacy and explicit overlapping ownership close independently");
    }

    public static void main(String[] args)throws Exception{
        boolean baseline=args.length>0&&"baseline82".equals(args[0]);
        try{
            if(baseline)baseline();else{forecast();invalidAndRollback();actualMaterialization();uploadContract();}
            System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"baseline82\":"+baseline+
                ",\"old_budget_bytes\":"+oldBudget+",\"new_budget_bytes\":"+newBudget+
                ",\"phantom_upload_bytes\":"+(4*O)+",\"fixed_limit_bytes\":"+GpuNoise1960.MAX_BYTES+"}");
        }finally{
            WholeRoute1953.retained=0;WholeRoute1953.fail=false;WholeRoute1953.oom=false;
            ((java.util.concurrent.ExecutorService)field(GpuNoise1960.class,"OWNER").get(null)).shutdownNow();
        }
    }
}
