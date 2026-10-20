package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.Field;
import java.util.Map;
/** Real proof/corpus caches; only Android persistence and the device identity are controlled. */
public final class CacheIsolation19107Test {
    static int assertions;
    static void check(boolean pass,String why){assertions++;if(!pass)throw new AssertionError(why);}
    static void cold()throws Exception {
        for(String n:new String[]{"RECORDS","FAILURES"}){
            Field f=GpuQualification1961.class.getDeclaredField(n);f.setAccessible(true);((Map<?,?>)f.get(null)).clear();
        }
    }
    public static void main(String[] args)throws Exception {
        Context context=new Context();GpuQualification1961.initialize(context);GpuCertificates1990.initialize(context);
        String old="GX1961:host-driver|source="+args[0],fresh="GX1961:host-driver|source="+args[1];
        check(!old.equals(fresh),"actual native source identity changed");
        String[] keys={"strong-gx1978-policy-direct-ieee-v1:3:shape", "strong-gx1978-policy-direct-pow2-v1:3:shape",
            "strong-gx1978-tuning-policy-bank-v1:3:shape", "strong-resident1978:3:v2-deployed:shape",
            "resident1991:exactprep51-div-v2:shape", "finish-chain1976:v2:shape", "regions1961:v2:shape", "single1961:shape"};
        String[] suites={"strong1990-direct-ieee39-aligned-confidence-corpus68-v2", "model1991-exact-prep51-div-corpus32-v2", "finish1993-exact-axis-policy32-corpus-v1:128-rows-geometry1994-v2", "strong1994-tile8-pow2-68cases-256rows-exact-v1"};
        GpuNoise1960.identity=old;
        for(String key:keys){GpuQualification1961.qualified(key,1000,800,0);check(GpuQualification1961.restore(key)!=null,"old proof retained "+key);GpuQualification1961.reject(key+":reject");check(GpuQualification1961.exactRejected(key+":reject"),"old exact rejection retained "+key);}
        for(String suite:suites){check(GpuCertificates1990.passed(suite,68,12345,1000,800),"old corpus written");check(GpuCertificates1990.restore(suite)!=null,"old corpus warm cache");GpuCertificates1990.reject(suite+":reject","actual fixture mismatch");check(GpuCertificates1990.rejected(suite+":reject"),"old corpus rejection retained");}
        int retained=context.getSharedPreferences("ignored",0).getAll().size();check(retained>0,"persistent proof records exist");
        GpuNoise1960.identity=fresh;
        for(int pass=0;pass<2;pass++){
            if(pass!=0)cold();
            for(String key:keys){check(GpuQualification1961.restore(key)==null,"old proof cannot admit new shader");check(!GpuQualification1961.exactRejected(key+":reject"),"old mismatch cannot poison new shader");}
            for(String suite:suites){check(GpuCertificates1990.restore(suite)==null,"old startup corpus cannot skip new preflight");check(!GpuCertificates1990.rejected(suite+":reject"),"old corpus rejection remains bound to old shader");}
        }
        check(context.getSharedPreferences("ignored",0).getAll().size()==retained,"identity switch never deletes old evidence");
        for(String key:keys){GpuQualification1961.qualified(key,1000,800,0);check(GpuQualification1961.restore(key)!=null,"new measured proof accepted");GpuQualification1961.qualified(key+":slow",1000,960,0);check(GpuQualification1961.restore(key+":slow")==null,"five percent speed gate unchanged");GpuQualification1961.reject(key+":new-reject");check(GpuQualification1961.exactRejected(key+":new-reject"),"new exact mismatch is sticky");}
        for(String suite:suites){check(GpuCertificates1990.passed(suite,68,12345,1000,800),"new corpus writes independently");check(GpuCertificates1990.restore(suite)!=null,"new corpus restores");GpuCertificates1990.reject(suite,"new actual mismatch");check(GpuCertificates1990.restore(suite)==null&&GpuCertificates1990.rejected(suite),"new corpus mismatch revokes its own pass");}
        GpuNoise1960.identity=old;cold();
        for(String key:keys){check(GpuQualification1961.restore(key)!=null,"old proof unchanged");check(GpuQualification1961.exactRejected(key+":reject"),"old rejection unchanged");check(!GpuQualification1961.exactRejected(key+":new-reject"),"new rejection never crosses backwards");}
        for(String suite:suites)check(GpuCertificates1990.restore(suite)!=null&&!GpuCertificates1990.rejected(suite),"new rejection did not corrupt old corpus");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"proof_families\":"+keys.length+",\"corpus_families\":"+suites.length+"}");
    }
}
