package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import java.io.*;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;

/** State-machine and disk-persistence tests of the actual production service.
 * A qualification call represents the existing caller's completed pixel proof;
 * these fixtures make no claim to execute a GPU or measure Android performance. */
public final class Recovery1977Test {
    static final String LEGACY_SCHEMA="gx1964-full-output-parallel-2wins5-v1";
    static final String PROOF_SCHEMA=LEGACY_SCHEMA+"-per-key-recovery-v1";
    static final String PREFERRED_SCHEMA=PROOF_SCHEMA+"-strong-exact2-preferred-v1";
    static final String OLD_ENV="recovery1977-original-driver-and-source";
    static final String NEW_ENV="recovery1977-updated-driver-or-source";
    static final String PREFS="ulike_gx1961_proofs";
    static final String[] CAUSES={"argb_mismatch","confidence_mismatch","policy_failure"};
    static final String[] PROFILE={"strong-gx1973-ieee-div-policy-bank-v1", "strong-gx1964-parallel-policy-bank-v1", "strong-gx1971-generic-policy-bank-v1", "strong-gx1976-ieee-tile8-policy-bank-v1"};
    static final int FAMILIES=28, JOURNAL_COUNT=655, UNKNOWN_COUNT=1000;
    static int assertions;
    static SharedPreferences preferences;
    static Context context;
    static final Map<String,Integer> tests=new LinkedHashMap<String,Integer>();
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void report(String name,int start){tests.put(name,assertions-start);}
    static Field field(String name)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f;}
    static String environment()throws Exception{Method m=GpuQualification1961.class.getDeclaredMethod("environment");m.setAccessible(true);return (String)m.invoke(null);}
    static void reload()throws Exception{synchronized(field("LOCK").get(null)){((Map<?,?>)field("RECORDS").get(null)).clear();((Map<?,?>)field("FAILURES").get(null)).clear();}}
    static String digest(String text)throws Exception{
        byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8"));StringBuilder out=new StringBuilder();
        for(byte b:bytes)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();
    }
    static void select(String fingerprint)throws Exception{Build.FINGERPRINT=fingerprint;GpuQualification1961.initialize(context);reload();}
    static String strong(int profile,int mode,String suffix){return PROFILE[profile]+":"+mode+":128:272:256:0:272:0:0:4284:4:1:1:1:192:256:"+suffix;}
    static String colour(int kind,String suffix){return "cpu-colour1976-exact2-time5-v1:"+kind+":1:[128, 272, 0, 272, 0, 272, 0, 4, 1, 128, 272, 128, 272, 128, 272]:"+suffix;}
    static String finish(String suffix){return "cpu-finish1976-exact2-time5-v1:128:272:0:272:1:1:1:256:0:255:1:1:"+suffix;}
    static String family(int family,String suffix){
        if(family<16)return strong(family/4,family%4,suffix);
        if(family<19)return colour(family-16,suffix);
        switch(family){
            case 19:return finish(suffix);
            case 20:return "unchanged-pixel-shader-v1|finish-chain1962:v1|128:272:"+suffix;
            case 21:return "unchanged-pixel-shader-v1|finish-chain1976:v2|128:272:"+suffix;
            case 22:return "unchanged-pixel-shader-v1|moire-geometry|128:272:"+suffix;
            case 23:return "strong-gx1975-tuning-policy-bank-v1:3:128:272:256:0:272:0:0:4284:4:1:1:1:192:256:"+suffix;
            case 24:return "strong-gx1976-tuning-policy-bank-v1:3:128:272:256:0:272:0:0:4284:4:1:1:1:192:256:"+suffix;
            case 25:return "strong-layout1976:3:128:272:"+suffix;
            case 26:return "strong-resident1976:3:v1:128:272:"+suffix;
            case 27:return "residual-v1:128:272:"+suffix;
            default:throw new AssertionError("unknown fixture family");
        }
    }
    static String journalKey(int ordinal){
        // Three families individually exceed the old limit; all other real
        // routes are represented too. A replacement family cap cannot pass.
        int f=ordinal<300?16:ordinal<500?7:ordinal<600?19:(ordinal-600)%FAMILIES;
        return family(f,"actual-journal-"+ordinal);
    }
    static String cached(String cause){return "argb_mismatch".equals(cause)?"cached_argb_negative":"confidence_mismatch".equals(cause)?"cached_confidence_negative":"policy_failure".equals(cause)?"cached_policy_negative":"cached_legacy_negative";}
    static void unknown(String key,String label){
        check(!GpuQualification1961.exactRejected(key),label+": no exact evidence may be invented");
        check(GpuQualification1961.exactFailure1971(key)==null,label+": no synthetic failure cause");
        check(GpuQualification1961.restore(key)==null,label+": no automatic positive certificate");
        check(GpuQualification1961.maySchedule(key),label+": fresh proof may be considered");
        check(GpuQualification1961.canQueue(key,1),label+": fresh proof can reserve admission");
    }
    static void bad(String key,String cause,String label){
        check(GpuQualification1961.exactRejected(key),label+": actual exact evidence retained");
        check(cached(cause).equals(GpuQualification1961.exactFailure1971(key)),label+": actual typed cause retained");
        check(!GpuQualification1961.maySchedule(key),label+": actual failure cannot retry");
        check(!GpuQualification1961.canQueue(key,1),label+": actual failure cannot queue");
        check(GpuQualification1961.restore(key)==null,label+": actual failure has no usable certificate");
    }
    static void allFamiliesUnknown(String suffix,String label){for(int i=0;i<FAMILIES;i++)unknown(family(i,suffix),label+" family "+i);}
    static void unknownReads(String suffix)throws Exception{
        Map<String,?> before=preferences.getAll();
        for(int i=0;i<UNKNOWN_COUNT;i++)unknown(family(i%FAMILIES,suffix+"-"+i),"read-only unknown "+i);
        check(before.equals(preferences.getAll()),"1000 unknown keys never enlarge or mutate persisted state");
        check(((Map<?,?>)field("FAILURES").get(null)).size()<=64,"recent failure RAM cache remains bounded at 64");
    }
    static int entries(String prefix){int result=0;for(String key:preferences.getAll().keySet())if(key.startsWith(prefix))result++;return result;}
    static String oldPositive(){return strong(0,3,"legacy-success");}
    static String oldClassic(){return family(21,"legacy-classic-success");}
    static String oldBad(int i){return i<3?strong(i,3,"legacy-actual-"+i):colour(0,"legacy-actual-"+i);}
    static String legacyOverflow(){return finish("legacy-65th-not-individually-stored");}
    static void legacySeed()throws Exception{
        int n=assertions;select(OLD_ENV);
        GpuQualification1961.qualifiedStrongPreferred1970(oldPositive(),100,160,1);
        GpuQualification1961.qualified(oldClassic(),100,95,2);
        check(GpuQualification1961.restore(oldPositive())!=null&&GpuQualification1961.restore(oldClassic())!=null,"real .76 service persists accepted old positive schemas");
        for(int i=0;i<64;i++)GpuQualification1961.rejectExact1971(oldBad(i),CAUSES[i%3]);
        check(!GpuQualification1961.exactRejected(oldPositive()),"old positive remains usable at exactly 64 unrelated rejects");
        GpuQualification1961.rejectExact1971(legacyOverflow(),"confidence_mismatch");
        check(entries("reject-")-entries("reject-all-")==64,"real .76 retains exactly 64 individual legacy records");
        check(LEGACY_SCHEMA.equals(preferences.getString("reject-all-"+environment(),"")),"real .76 overflow writes legacy blanket marker");
        check(entries("proof-")==2,"legacy blanket coexists with two signed old success records");
        report("legacy_1976_service_generates_real_overflow_and_signed_proofs",n);
    }
    static void legacyControl()throws Exception{
        int n=assertions;select(OLD_ENV);
        unknown(strong(0,3,"legacy-recovery-control"),"legacy blanket recovery control");
        report("legacy_blanket_control_recovers_only_to_unknown",n);
    }
    static void familyControl()throws Exception{
        int n=assertions;select(NEW_ENV);
        for(int i=0;i<65;i++)GpuQualification1961.rejectExact1971(colour(0,"family-control-"+i),"argb_mismatch");
        unknown(strong(0,3,"unrelated-full-strong"),"CPU family overflow control");
        unknown(colour(0,"same-family-unseen-shape"),"same CPU family unknown shape control");
        report("65_real_cpu_failures_do_not_reject_unknown_shapes_or_strong",n);
    }
    static void migrateSame(boolean restarted)throws Exception{
        int n=assertions;select(OLD_ENV);
        for(int i=0;i<64;i++)bad(oldBad(i),CAUSES[i%3],"same-environment legacy individual "+i);
        check(LEGACY_SCHEMA.equals(preferences.getString("reject-all-"+environment(),"")),"migration leaves legacy marker intact while ignoring it as key evidence");
        unknown(legacyOverflow(),"legacy overflow key must obtain fresh proof");
        check(GpuQualification1961.restore(oldClassic())==null,"old classic success cannot cross recovery positive epoch");
        if(!restarted){
            unknown(oldPositive(),"old preferred success requires fresh recovery epoch proof");
            allFamiliesUnknown("after-legacy-blanket","legacy marker recovery");
            unknownReads("same-environment-legacy-recovery");
            GpuQualification1961.rejectExact1971(colour(1,"new-actual-after-upgrade"),"policy_failure");
            unknown(strong(0,3,"after-new-CPU-mismatch"),"new unrelated CPU mismatch cannot reinstate blanket");
            GpuQualification1961.qualified(oldPositive(),100,96,1);
            check(GpuQualification1961.restore(oldPositive())==null,"fresh ordinary proof still needs inclusive five percent improvement");
            GpuQualification1961.qualifiedStrongPreferred1970(oldPositive(),100,160,1);reload();
            check(GpuQualification1961.restore(oldPositive())!=null,"only a fresh caller-supplied full proof writes the new preferred epoch");
        } else {
            GpuQualification1961.Record accepted=GpuQualification1961.restore(oldPositive());
            check(accepted!=null&&accepted.variant==1&&accepted.gpuNanos==160,"fresh recovery preferred proof survives separate JVM restart");
            unknownReads("same-environment-restart");
        }
        check(preferences.getString("proof-"+digest(environment()+"|"+oldPositive()),"").startsWith(PREFERRED_SCHEMA+":"),"new preferred success really persists the declared recovery schema");
        bad(colour(1,"new-actual-after-upgrade"),"policy_failure","new exact journal coexists with legacy direct records");
        for(int i=0;i<3;i++)bad(oldBad(i),CAUSES[i],"new epoch proof never erases legacy actual failure "+i);
        report(restarted?"legacy_and_new_exact_evidence_survive_fresh_process":"legacy_blanket_ignored_old_positives_invalidated_individual_failures_preserved",n);
    }
    static void migrateEnvironment(boolean restarted)throws Exception{
        int n=assertions;select(NEW_ENV);
        unknown(oldPositive(),"old environment success is not borrowed");
        for(int i=0;i<3;i++)unknown(oldBad(i),"old environment failure cannot be borrowed "+i);
        if(!restarted){
            GpuQualification1961.rejectExact1971(colour(2,"new-environment-actual"),"argb_mismatch");
            GpuQualification1961.rejectExact1971(strong(2,3,"new-environment-actual"),"confidence_mismatch");
        }
        bad(colour(2,"new-environment-actual"),"argb_mismatch","current environment CPU actual evidence");
        bad(strong(2,3,"new-environment-actual"),"confidence_mismatch","current environment Strong actual evidence");
        allFamiliesUnknown("new-environment-unseen","old 64 plus current failures are independent");
        unknownReads(restarted?"environment-restart":"environment-upgrade");
        select(OLD_ENV);for(int i=0;i<3;i++)bad(oldBad(i),CAUSES[i],"switching environment never deletes old signed failure "+i);
        unknown(colour(2,"new-environment-actual"),"current CPU failure is bound to new environment");
        unknown(strong(2,3,"new-environment-actual"),"current Strong failure is bound to new environment");
        select(NEW_ENV);bad(strong(2,3,"new-environment-actual"),"confidence_mismatch","returning environment restores its own failure");
        report(restarted?"environment_journal_binding_survives_fresh_process":"old_environment_capacity_cannot_disable_new_environment",n);
    }
    static void journal(boolean restarted)throws Exception{
        int n=assertions;select(NEW_ENV);
        if(!restarted)for(int i=0;i<JOURNAL_COUNT;i++)GpuQualification1961.rejectExact1971(journalKey(i),CAUSES[i%3]);
        reload();
        for(int i=0;i<JOURNAL_COUNT;i++)bad(journalKey(i),CAUSES[i%3],"individual exact ordinal "+i);
        check(entries("reject1977-")==JOURNAL_COUNT,"655 actual failures remain individual persistent records");
        check(entries("reject-all-")==0,"no global negative marker is created by actual-failure volume");
        allFamiliesUnknown("journal-unseen","journal contains only actual keys");
        unknownReads(restarted?"journal-restart":"journal-write");
        Map<String,?> before=preferences.getAll();
        for(int i:new int[]{0,63,64,65,299,300,499,500,599,654}){
            String key=journalKey(i);GpuQualification1961.rejectSpeed(key);GpuQualification1961.qualified(key,100,80,0);GpuQualification1961.qualifiedStrongPreferred1970(key,100,160,0);reload();
            bad(key,CAUSES[i%3],"speed or positive attempt cannot erase actual ordinal "+i);
        }
        check(before.equals(preferences.getAll()),"positive and speed attempts never rewrite or delete exact journal");
        String goodCpu=colour(0,"same-family-fresh-success"),goodStrong=strong(1,3,"same-family-fresh-success");
        if(!restarted){
            unknown(goodCpu,"same CPU family requires independent proof");unknown(goodStrong,"same Strong family requires independent proof");
            GpuQualification1961.qualified(goodCpu,100,95,2);GpuQualification1961.qualifiedStrongPreferred1970(goodStrong,100,160,1);reload();
        }
        check(GpuQualification1961.restore(goodCpu)!=null&&!GpuQualification1961.exactRejected(goodCpu),"new exact fast CPU proof works beside 300 failed CPU keys");
        check(GpuQualification1961.restore(goodStrong)!=null&&!GpuQualification1961.exactRejected(goodStrong),"new preferred Strong proof works beside 200 failed profile keys");
        check(preferences.getString("proof-"+digest(environment()+"|"+goodCpu),"").startsWith(PROOF_SCHEMA+":"),"new classic success really persists the declared recovery schema");
        for(int i=0;i<JOURNAL_COUNT;i++)bad(journalKey(i),CAUSES[i%3],"success does not retire actual failure "+i);
        check(GpuQualification1961.status1967().contains("個別拒否（直近照会）64件"),"diagnostic explicitly counts 64 hot lookups, not all 655 persisted failures");
        report(restarted?"655_individual_failures_and_independent_successes_survive_restart":"655_individual_failures_retained_1000_unknown_reads_do_not_write",n);
    }
    static void speed(boolean restarted)throws Exception{
        int n=assertions;select(NEW_ENV);
        String actual=strong(3,3,"speed-does-not-erase-actual"),ordinary=colour(0,"ordinary-speed-only"),preferred=strong(0,3,"preferred-speed-only");
        if(!restarted){
            GpuQualification1961.rejectExact1971(actual,"confidence_mismatch");
            Map<String,?> before=preferences.getAll();
            for(int i=0;i<JOURNAL_COUNT;i++){
                String key=family(i%FAMILIES,"speed-only-"+i);GpuQualification1961.rejectSpeed(key);
                check(!GpuQualification1961.exactRejected(key)&&GpuQualification1961.exactFailure1971(key)==null,"speed failure is not exact evidence "+i);
                check(GpuQualification1961.restore(key)==null,"speed failure is never a positive proof "+i);
            }
            check(before.equals(preferences.getAll()),"655 speed-only outcomes do not grow permanent exact journal");
            GpuQualification1961.rejectSpeed(ordinary);
            check(!GpuQualification1961.maySchedule(ordinary)&&!GpuQualification1961.canQueue(ordinary,1),"ordinary speed cooldown remains enforced");
            GpuQualification1961.rejectSpeed(preferred);unknown(preferred,"preferred speed failure permits only fresh proof");
            GpuQualification1961.qualifiedStrongPreferred1970(preferred,100,160,0);check(GpuQualification1961.restore(preferred)!=null,"existing preferred policy may accept a newly supplied full proof after speed failure");
            GpuQualification1961.rejectSpeed(preferred);check(GpuQualification1961.restore(preferred)==null,"later speed failure retires its success without fabricating exact rejection");
            GpuQualification1961.rejectSpeed(actual);reload();
        } else {
            unknown(ordinary,"nonpersistent speed cooldown resets on fresh process");unknown(preferred,"speed history supplies no positive certificate after restart");
        }
        bad(actual,"confidence_mismatch","real exact failure survives speed traffic and cache eviction");
        check(entries("reject1977-")==1,"only the one actual mismatch has a persistent journal entry");
        allFamiliesUnknown("unrelated-to-speed","speed and exact evidence are per key");
        report(restarted?"speed_history_is_distinct_from_persisted_actual_failure_after_restart":"speed_only_655_outcomes_never_become_exact_evidence",n);
    }
    static String signedKey(String key)throws Exception{
        int colon=key.indexOf(':');String family=colon<0?key:key.substring(0,colon);
        return "reject1977-"+environment()+"-"+digest(family)+"-"+digest(key);
    }
    static void integrity()throws Exception{
        int n=assertions;select(NEW_ENV);
        String a=strong(0,3,"signed-actual"),b=strong(1,3,"signed-actual"),shape=strong(0,3,"signed-other-shape");
        GpuQualification1961.rejectExact1971(a,"confidence_mismatch");reload();bad(a,"confidence_mismatch","signed new exact journal");
        String raw=preferences.getString(signedKey(a),"");check(raw.startsWith("gx1977-exact-per-key-v1:confidence_mismatch:"),"new journal payload explicitly identifies exact evidence and typed cause");
        preferences.edit().putString(signedKey(b),raw).putString(signedKey(shape),raw).apply();reload();
        unknown(b,"journal signature cannot be transplanted across arithmetic family");unknown(shape,"journal signature cannot be transplanted across exact shape");
        preferences.edit().putString(signedKey(b),raw.replace("confidence_mismatch","argb_mismatch")).apply();reload();unknown(b,"journal cause cannot be forged without a matching full signature");
        String fromEnvironment=environment();select(OLD_ENV);
        preferences.edit().putString(signedKey(a),raw).apply();reload();unknown(a,"journal signature cannot be transplanted to another environment");
        check(!fromEnvironment.equals(environment()),"test used genuinely distinct environment digests");select(NEW_ENV);
        bad(a,"confidence_mismatch","original journal survives malformed and transplanted entries");
        String signedLegacy="reject-"+digest(environment()+"|"+a);
        preferences.edit().putString(signedLegacy,LEGACY_SCHEMA+":"+digest(environment()+"|"+a+"|exact")).putString(signedKey(a),"malformed-new-journal").apply();reload();
        bad(a,null,"valid signed legacy rejection survives malformed new diagnostic journal");
        // Malformed cause metadata is not permission to discard an otherwise
        // signed old exact failure, including when a stale blanket coexists.
        preferences.edit().putString("failure-cause-"+digest(environment()+"|"+a),"malformed").putString("reject-all-"+environment(),LEGACY_SCHEMA).apply();reload();
        bad(a,null,"legacy direct remains authoritative with malformed cause and old global marker");
        unknown(strong(2,3,"integrity-unknown"),"legacy global remains non-evidence after malformed metadata");
        report("journal_signature_binds_environment_family_key_and_cause_legacy_failures_remain_authoritative",n);
    }
    static final class DiskContext extends Context {
        final SharedPreferences disk;DiskContext(File file){disk=new DiskPreferences(file);}
        public SharedPreferences getSharedPreferences(String name,int mode){return disk;}
    }
    static final class DiskPreferences implements SharedPreferences {
        final File file;final Properties values=new Properties();
        DiskPreferences(File file){this.file=file;if(file.isFile())try(InputStream in=new FileInputStream(file)){values.load(in);}catch(IOException e){throw new IllegalStateException(e);}}
        public synchronized String getString(String name,String fallback){return values.getProperty(name,fallback);}
        public synchronized Map<String,?> getAll(){Map<String,String> out=new HashMap<String,String>();for(String name:values.stringPropertyNames())out.put(name,values.getProperty(name));return out;}
        public Editor edit(){return new Editor(){final Map<String,String> changes=new HashMap<String,String>();public Editor putString(String name,String value){changes.put(name,value);return this;}public Editor remove(String name){changes.put(name,null);return this;}public void apply(){synchronized(DiskPreferences.this){for(Map.Entry<String,String> change:changes.entrySet())if(change.getValue()==null)values.remove(change.getKey());else values.setProperty(change.getKey(),change.getValue());try{file.getParentFile().mkdirs();try(OutputStream out=new FileOutputStream(file)){values.store(out,"production-service recovery host prefs");}}catch(IOException e){throw new IllegalStateException(e);}}}};}
    }
    public static void main(String[] args)throws Exception{
        String mode=args[0];context=new DiskContext(new File(args[1]));preferences=context.getSharedPreferences(PREFS,0);SaveQueue1935.idle=false;
        if("legacy-seed".equals(mode))legacySeed();
        else if("legacy-control".equals(mode))legacyControl();
        else if("family-control".equals(mode))familyControl();
        else if("migrate-same".equals(mode)||"restart-same".equals(mode))migrateSame(mode.startsWith("restart"));
        else if("migrate-environment".equals(mode)||"restart-environment".equals(mode))migrateEnvironment(mode.startsWith("restart"));
        else if("journal-write".equals(mode)||"journal-restart".equals(mode))journal(mode.endsWith("restart"));
        else if("speed-write".equals(mode)||"speed-restart".equals(mode))speed(mode.endsWith("restart"));
        else if("integrity".equals(mode))integrity();else throw new IllegalArgumentException(mode);
        StringBuilder out=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(assertions).append(",\"tests\":{");boolean first=true;
        for(Map.Entry<String,Integer> e:tests.entrySet()){if(!first)out.append(',');first=false;out.append('"').append(e.getKey()).append("\":{\"status\":\"passed\",\"assertions\":").append(e.getValue()).append('}');}
        System.out.println(out.append("}}").toString());
    }
}
