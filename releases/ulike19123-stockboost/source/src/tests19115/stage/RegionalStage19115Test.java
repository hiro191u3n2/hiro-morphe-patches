package com.hiro.ulike;

import java.lang.reflect.*;
import java.util.*;

/** Actual production Stage + JNI/software GLES. Only certificates/admission
 * refusal are controlled here; pixel arithmetic and native pools are real. */
public final class RegionalStage19115Test {
    static int assertions,cases,lifecycles,revocations,readRevocations;static long pixels;
    static final int[][] BANKS={{0,1,3,7,8,9,10,11,12,13},{14,15,16,17,18,19,20,21,22,23}};
    static native long[] probeNative();
    static void check(boolean ok,String label){assertions++;if(!ok)throw new AssertionError(label);}
    static Object field(Object o,String name)throws Exception{return StrongTransport1995Test.field(o,name);}
    static int[] cf(GpuStrongDefault1990.Fixture f){return StrongTransport1995Test.confidence(f);}
    static int[] expected(GpuStrongDefault1990.Fixture f,int[] confidence){return StrongTransport1995Test.expected(f,confidence);}
    static void same(int[] a,int[] b,String label){check(Arrays.equals(a,b),label);}
    static void checkRead(GpuStrongDefault1990.Fixture f,GpuStrong1960.Read1971 read,int[] want,int[] confidence,boolean regional,String label){
        check(read!=null&&read.failure==null&&read.result!=null,label+" complete");
        check(read.regional19115==regional,label+" actual route flag");
        int count=f.u[0]*(f.u[3]-f.u[2]);same(Arrays.copyOfRange(want,f.u[0]*f.u[2],f.u[0]*f.u[3]),read.result.pixels,label+" every pixel");
        same(confidence,read.result.confidence,label+" every confidence");pixels+=count;cases++;
    }
    static GpuStrong1960.Read1971 read(GpuStrongDefault1990.Fixture f,boolean required){return GpuStrong1960.ephemeralRegional19115(f.source,
        StrongNoise1958.gpuEvidence1960(f.model),StrongNoise1958.gpuMaps1960(f.model),f.policy,f.u,null,0,4,required);}
    static void certifyForRouting(){
        check(GpuCertificates1990.passed(GpuRegionalDefault19115.EXACT,70,100000,200000,100000),"controlled exact certificate stored");
        check(GpuCertificates1990.passed(GpuRegionalDefault19115.FAST,70,100000,200000,100000),"controlled fast certificate stored");
        check(GpuRegionalDefault19115.faster(),"real helper and real readiness authorize candidate");
    }
    static void oldStage()throws Exception{
        GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.fixture(63);int[] confidence=cf(f),wantCf=cf(f),want=expected(f,wantCf),actual=StrongTransport1995Test.output(f);
        Object stage=StrongTransport1995Test.stage(f.model,false);
        try{check(!((Boolean)field(stage,"regional19115")),"ordinary stage has no optional certificate");
            check(StrongTransport1995Test.run(f,actual,confidence),"ordinary stage GPU success");same(want,actual,"ordinary output");same(wantCf,confidence,"ordinary confidence");
            check(((Integer)field(stage,"regionalStrips19115"))==0,"ordinary output never new count");
        }finally{GpuStrong1960.endStage(f.model);}
        long[] probe=probeNative();check(probe[0]==0&&probe[1]==0,"ordinary startup/support/Stage never compiles optional programs");
        check(probe[2]==0&&probe[3]==0,"ordinary Stage no prepass storage");
    }
    static void corpus(){
        for(int id=0;id<68;id++){
            GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.fixture(id);int[] wantCf=cf(f),want=expected(f,wantCf),u=f.u.clone(),source=f.source.clone();
            checkRead(f,read(f,true),want,wantCf,true,"required case "+id);
            same(u,f.u,"caller uniforms preserved "+id);same(source,f.source,"caller source preserved "+id);
            long[] probe=probeNative();check(probe[2]==0&&probe[3]==0,"ephemeral scratch freed "+id);
            check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"ephemeral ownership closed "+id);
        }
    }
    static void fullStage(int id,boolean refused)throws Exception{
        GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.Adaptive1994.benchmark19112(id,StrongTransport1995Test.GO);
        check(GpuRegionalDefault19115.eligible(f.u),"full-size fixture eligible "+id);
        int[] confidence=cf(f),wantCf=cf(f),want=expected(f,wantCf),actual=StrongTransport1995Test.output(f);
        Object stage=StrongTransport1995Test.stage(f.model,false);
        try{check(((Boolean)field(stage,"regional19115")),"actual reflected helper selects candidate "+id);
            check(StrongTransport1995Test.run(f,actual,confidence),"full Stage succeeds "+id);
            same(want,actual,"full Stage exact private destination "+id);same(wantCf,confidence,"full Stage exact confidence "+id);
            check(((Integer)field(stage,"regionalStrips19115"))==(refused?0:1),"only actual candidate commit counted "+id);
            check(((Long)field(stage,"regionalPixels19115"))==(refused?0L:(long)f.u[0]*256),"actual candidate pixels "+id);
            long[] probe=probeNative();check(refused?probe[2]+probe[3]==0:probe[2]+probe[3]>0,"native table actual presence "+id);
        }finally{GpuStrong1960.endStage(f.model);}
        check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"full Stage owner released "+id);
        long[] probe=probeNative();check(probe[2]==0&&probe[3]==0,"full Stage scratch absent from next budget "+id);
        pixels+=(long)f.u[0]*256;cases++;
    }
    static boolean afterRead(boolean complete){
        if(complete&&Boolean.getBoolean("ulike.test.revokeRegionalRead")){
            System.clearProperty("ulike.test.revokeRegionalRead");readRevocations++;
            GpuCertificates1990.reject(GpuRegionalDefault19115.EXACT,"controlled after-real-read revocation");
        }
        return complete;
    }
    static void revoke(boolean afterRead)throws Exception{
        Map<String,String> saved=new HashMap<String,String>(StrongTransport1995Test.preferences);
        GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.Adaptive1994.benchmark19112(64,StrongTransport1995Test.GO);
        int[] actual=StrongTransport1995Test.output(f),confidence=cf(f);Object stage=StrongTransport1995Test.stage(f.model,false);
        try{check(((Boolean)field(stage,"regional19115")),"revocation starts from real qualified snapshot");
            if(afterRead)System.setProperty("ulike.test.revokeRegionalRead","true");
            else GpuCertificates1990.reject(GpuRegionalDefault19115.EXACT,"controlled before-submit revocation");
            check(!StrongTransport1995Test.run(f,actual,confidence),"revoked new certificate cannot commit");
            check(((Integer)field(stage,"regionalStrips19115"))==0&&((Integer)field(stage,"gpuStrips"))==0,"revoked output is not counted as adopted");
            if(afterRead)check(readRevocations==1,"revocation really occurred after native readback");
            else check(probeNative()[2]+probeNative()[3]==0,"pre-submit revocation allocates no native table");
        }finally{System.clearProperty("ulike.test.revokeRegionalRead");GpuStrong1960.endStage(f.model);
            StrongTransport1995Test.preferences.clear();StrongTransport1995Test.preferences.putAll(saved);}
        check(GpuRegionalDefault19115.faster(),"original controlled records restored after test");
        check(probeNative()[2]+probeNative()[3]==0&&GpuNoise1960.reservedBytes1971()==0&&!GpuNoise1960.sessionBusy(),"revocation closes native table and lease");revocations++;
    }
    static void pool(int mode)throws Exception{
        // mode0=normal collect;1=uncollected cancellation;2=Java upload refusal.
        GpuNoise1960.trimIdle();
        GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.Adaptive1994.benchmark19112(64,StrongTransport1995Test.GO);
        float[] evidence=StrongNoise1958.gpuEvidence1960(f.model);int[][] maps=StrongNoise1958.gpuMaps1960(f.model);int[] u=f.u.clone();u[21]=-1;u[22]=3*u[0]*256;u[23]=1;
        GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"pool session "+mode);
        GpuNoise1960.Lease1971 model=null;GpuNoise1960.Lease1971[] leases=new GpuNoise1960.Lease1971[2];long before=0,scratch=0;
        try{
            model=session.reserveCapacity1971(new int[]{2,4,5,6},new long[]{4L*evidence.length,4L*maps[0].length,4L*maps[1].length,4L*maps[2].length},0);check(model!=null,"pool model lease");
            check(session.run(new GpuNoise1960.Batch().upload(2,evidence).upload(4,maps[0]).upload(5,maps[1]).upload(6,maps[2])),"pool upload");model.close();model=null;
            GpuNoise1960.Ticket[] ticket=new GpuNoise1960.Ticket[2];
            for(int bank=0;bank<2;bank++){leases[bank]=GpuStrong1960.reserveRange1971(session,f.source,f.policy,u,null,BANKS[bank]);check(leases[bank]!=null,"pool range lease");
                ticket[bank]=session.submit(GpuStrong1960.commands1973(f.source,f.policy,u,null,BANKS[bank],0,4),bank);check(ticket[bank]!=null,"pool real prepass submit");}
            if(mode!=1)for(int bank=0;bank<2;bank++){GpuStrong1960.Read1971 r=GpuStrong1960.read1971(session,ticket[bank],u,BANKS[bank]);check(r.failure==null,"pool real result collected");}
            long[] probe=probeNative();scratch=probe[2]+probe[3];before=probe[4]+probe[5];check(probe[2]>0&&probe[3]>0,"both real tables allocated");
            check(scratch>=2L*12*u[0]*256,"table capacities include both banks");
            if(mode==2)check(!session.upload(BANKS[0][8],new int[]{1}),"explicit zero-upload GPU table rejects CPU upload");
        }finally{for(GpuNoise1960.Lease1971 l:leases)if(l!=null)l.close();if(model!=null)model.close();session.close();}
        long[] after=probeNative();check(after[2]==0&&after[3]==0,"close removed both new tables "+mode);
        check(after[4]+after[5]==before-scratch,"only new table bytes removed; old pool retained "+mode);
        check(after[4]>0&&GpuNoise1960.reservedBytes1971()==0&&!GpuNoise1960.sessionBusy(),"old pool and owner state "+mode);lifecycles++;
    }
    static void reservationOnly(){
        GpuNoise1960.trimIdle();GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"reservation session");
        GpuNoise1960.Lease1971 l=s.reserveCapacity1983(new int[]{12,22},new long[]{9400320,9400320},new long[]{0,0},0);check(l!=null,"reservation allowed");
        try{long[] p=probeNative();check(p[2]==0&&p[3]==0,"reservation does not allocate native storage");}finally{l.close();s.close();}
        check(probeNative()[4]==0&&GpuNoise1960.reservedBytes1971()==0,"reservation-only close retains no allocations");lifecycles++;
    }
    static void malformed(){
        GpuNoise1960.trimIdle();GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"malformed session");
        GpuNoise1960.Lease1971 l=s.reserveCapacity1983(new int[]{12},new long[]{9400320},new long[]{0},0);check(l!=null,"malformed lease");
        try{int[] u=new int[32];u[0]=1;u[1]=1;u[3]=1;u[7]=1;u[10]=3;u[22]=3;
            check(s.submit(new GpuNoise1960.Batch().allocate(12,9400320).dispatch(61,new int[]{12},u,null,1),0)==null,"allocate then malformed prepass rejected");
            check(probeNative()[2]>0,"failure followed actual allocation");
        }finally{l.close();s.close();}
        check(probeNative()[2]==0&&probeNative()[4]==0,"failed native batch releases prior allocation");lifecycles++;
    }
    public static void main(String[] args)throws Exception{
        StrongTransport1995Test.store();check(StrongNoise1958.nativeAvailable(),"actual native CPU oracle");
        check(GpuStrongDefault1990.qualify(StrongTransport1995Test.GO),"actual startup whole corpus passes");
        oldStage();
        GpuStrongDefault1990.Fixture first=GpuStrongDefault1990.fixture(63);
        check(!GpuNoise1960.regionalReady19115(),"optional readiness starts false");
        check(read(first,true).result==null,"required proof refuses unprepared candidate");
        check(!GpuNoise1960.regionalReady19115(),"readiness and failed proof do not compile");
        check(GpuNoise1960.prepareRegional19115(),"explicit optional compilation succeeds");
        long[] p=probeNative();check(p[0]==1&&p[1]==1,"both optional programs really prepared");
        if(args.length>0){
            certifyForRouting();System.setProperty("ulike.test.refuseRegional","true");fullStage(64,true);
            check(read(first,true).result==null,"required prepass cannot certify fallback");
            int[] confidence=cf(first),want=expected(first,confidence);checkRead(first,read(first,false),want,confidence,false,"explicit optional refusal fallback");
            System.clearProperty("ulike.test.refuseRegional");revoke(true);
        }else{
            corpus();certifyForRouting();fullStage(64,false);fullStage(65,false);revoke(false);
            pool(0);pool(1);pool(2);reservationOnly();malformed();pool(0);
        }
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"pixels\":"+pixels+",\"revocation_cases\":"+revocations+",\"lifecycle_cases\":"+lifecycles+",\"actual_jni\":true,\"actual_native_cpu\":true,\"physical_android_tested\":false}");
    }
}
