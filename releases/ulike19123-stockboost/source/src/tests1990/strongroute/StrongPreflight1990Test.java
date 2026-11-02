package com.hiro.ulike;
import java.util.concurrent.CancellationException;

public final class StrongPreflight1990Test {
    static int assertions;
    static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void reset(){GpuCertificates1990.evidence=null;GpuCertificates1990.rejected=false;GpuCertificates1990.writes=0;GpuQualification1961.exactRejected=false;GpuStrong1960.fault=GpuStrong1960.submits=0;}
    public static void main(String[] args){
        reset();long pixels=0;
        for(int id=0;id<GpuStrongDefault1990.CASES;id++){
            GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.fixture(id);
            check(GpuStrongDefault1990.owns(f.u),"corpus case "+id+" inside declared geometry");
            check(f.source.length==(long)f.u[0]*f.u[1],"source geometry "+id);
            check(f.policy==null?f.u[11]==0:f.policy.length==2L*f.u[0]*(f.u[3]-f.u[2]),"policy geometry "+id);
            pixels+=(long)f.u[0]*(f.u[3]-f.u[2]);
        }
        check(GpuStrongDefault1990.qualify(GO),"complete corpus commits evidence");
        check(GpuCertificates1990.evidence.cases==GpuStrongDefault1990.CASES&&GpuCertificates1990.evidence.pixels==pixels&&GpuCertificates1990.writes==1,"all complete outputs counted once");
        check(GpuCertificates1990.evidence.cpuNanos>0&&GpuCertificates1990.evidence.gpuNanos>0,"timings are measured positive intervals");
        int submits=GpuStrong1960.submits;check(GpuStrongDefault1990.qualify(GO)&&GpuStrong1960.submits==submits,"restored exact-environment suite avoids re-execution");
        int[] u=GpuStrongDefault1990.fixture(64).u;check(GpuStrongDefault1990.accepts(u),"production full-width geometry accepted");
        u[6]++;check(!GpuStrongDefault1990.owns(u),"misaligned confidence origin is outside native/Java common domain");u[6]--;
        u[3]--;check(!GpuStrongDefault1990.owns(u),"partial confidence cell away from image end rejected");u[3]++;
        GpuQualification1961.exactRejected=true;check(!GpuStrongDefault1990.accepts(u),"existing per-program rejection remains authoritative");
        u[3]=u[2]+257;check(!GpuStrongDefault1990.owns(u),"unsupported core geometry rejected");
        reset();GpuStrong1960.fault=1;check(!GpuStrongDefault1990.qualify(GO)&&GpuCertificates1990.rejected&&GpuCertificates1990.writes==0,"one unequal ARGB rejects whole suite");
        GpuStrong1960.fault=0;submits=GpuStrong1960.submits;check(!GpuStrongDefault1990.qualify(GO)&&GpuStrong1960.submits==submits,"rejection never cleared by retry");
        reset();GpuStrong1960.fault=2;check(!GpuStrongDefault1990.qualify(GO)&&GpuCertificates1990.rejected&&GpuCertificates1990.writes==0,"unequal confidence also rejects suite");
        reset();GpuStrong1960.fault=3;check(!GpuStrongDefault1990.qualify(GO)&&!GpuCertificates1990.rejected&&GpuCertificates1990.writes==0,"allocation/transport failure does not become pixel mismatch");
        reset();boolean cancelled=false;try{GpuStrongDefault1990.qualify(new GpuQualification1961.Cancellation(){int calls;public boolean cancelled(){return ++calls>2;}});}catch(CancellationException e){cancelled=true;}
        check(cancelled&&GpuCertificates1990.writes==0&&!GpuCertificates1990.rejected,"cancelled corpus publishes no evidence");
        int[] expected={1,2,3,4};check(GpuStrongDefault1990.difference(new int[]{2,3},null,expected,null,1,2)==null,"comparison respects CPU core offset");
        check(GpuStrongDefault1990.difference(new int[]{2,2},null,expected,null,1,2).contains("index=1"),"diagnostic identifies first unequal pixel");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"physical_android_tested\":false,\"actual_jni\":false}");
    }
}
