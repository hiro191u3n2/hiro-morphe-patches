package com.hiro.ulike;

/** Immutable reasons from the actual admission operation, without live reads. */
public final class SnapshotDecision1985Test {
    static final long M=1024L*1024;
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void capture(){ProcessingTiming1947.epoch++;}
    static GpuSnapshotBudget1981.Decision1985 decide(long bytes,int family){return GpuSnapshotBudget1981.tryCopy1985(bytes,family);}
    static void reason(GpuSnapshotBudget1981.Decision1985 d,int reason){
        check(d.reason==reason,"the returned decision contains the exact refusal reason "+reason);
        check((d.copy!=null)==(reason==0),"only READY decisions own a copy token");
    }
    public static void main(String[] args){
        capture();GpuSnapshotBudget1981.Copy tentative=GpuSnapshotBudget1981.tryCopy(M,0);
        check(tentative!=null,"unnamed strip holds the same-copy exclusion token");
        GpuSnapshotBudget1981.Decision1985 reserved=decide(M,1);reason(reserved,9);
        check(reserved.activeFamily==0&&!reserved.activeStarted,"a tentative token is identified separately from an active copy");
        check(reserved.copyEpoch==ProcessingTiming1947.epoch,"decision belongs to the admission's capture");
        check(tentative.begin(),"the prior owner can begin its single copy");
        GpuSnapshotBudget1981.Decision1985 active=decide(M,1);reason(active,8);
        check(active.activeFamily==0&&active.activeStarted,"active allocation is distinguishable from a reservation");
        tentative.close();reason(reserved,9);reason(active,8);
        check(!reserved.activeStarted&&active.activeStarted,"saved decisions cannot be rewritten by the live owner's progress");
        GpuSnapshotBudget1981.Decision1985 ready=decide(M,1);reason(ready,0);
        check(ready.activeFamily==-1&&!ready.activeStarted,"READY snapshot observes no prior copy owner");
        check(ready.copy.begin(),"a READY ticket still requires explicit begin before allocation");ready.copy.close();
        GpuSnapshotBudget1981.Decision1985 used=decide(M,1);reason(used,4);
        check(used.activeFamily==-1,"used whole-image allowance is not represented as an in-flight copy");
        capture();GpuSnapshotBudget1981.Decision1985 turn=decide(M,2);reason(turn,5);
        check(turn.preferredFamily==1&&turn.activeFamily==-1,"family rotation deferral records which family owns this turn");
        GpuSnapshotBudget1981.Decision1985 chosen=decide(M,1);reason(chosen,0);chosen.copy.close();
        reason(turn,5);check(turn.preferredFamily==1,"later admission cannot rewrite the previous rotation reason");

        reason(decide(0,1),1);reason(decide(-1,1),1);reason(decide(M,3),1);reason(decide(96*M+1,1),2);
        Thread.currentThread().interrupt();reason(decide(M,1),3);
        check(Thread.currentThread().isInterrupted(),"diagnostic admission does not clear caller cancellation");Thread.interrupted();
        capture();for(int family:new int[]{8,16,32})GpuSnapshotBudget1981.offerStrip1981(family);
        capture();GpuSnapshotBudget1981.Decision1985 stripTurn=decide(M,16);reason(stripTurn,7);
        check(stripTurn.preferredFamily==8,"named strip families retain their independent rotation");
        GpuSnapshotBudget1981.Copy strip=GpuSnapshotBudget1981.tryCopy(32*M,0);
        check(strip!=null&&strip.begin(),"the generic strip still uses the original 32 MiB allowance");strip.close();
        reason(decide(1,0),6);
        capture();GpuSnapshotBudget1981.Decision1985 stale=decide(M,0);reason(stale,0);
        long epoch=stale.copyEpoch;capture();GpuSnapshotBudget1981.Decision1985 fresh=decide(M,0);reason(fresh,0);
        check(stale.copyEpoch==epoch&&fresh.copyEpoch!=epoch,"capture changes leave earlier decision epochs immutable");
        check(!stale.copy.begin()&&fresh.copy.begin(),"the obsolete tentative token cannot start while the new one can");
        stale.copy.close();reason(decide(M,0),8);fresh.copy.close();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+
            ",\"snapshot_exact_refusal_diagnostics1985\":true,\"snapshot_decision_immutability1985\":true}");
    }
}
