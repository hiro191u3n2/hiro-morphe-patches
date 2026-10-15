package com.hiro.ulike;

/** Real retry predicate under pressure: observing it must not retire inputs. */
public final class PermissionSnapshot1996Test {
    static int checks;
    static void check(boolean value,String why) {
        checks++;if(!value)throw new AssertionError(why);
    }
    public static void main(String[] args)throws Exception {
        QueueModel1994Test.reset();
        long mib=1024L*1024;
        QueueModel1994Test.Probe retained=QueueModel1994Test.queue(
            "cpu-colour1976-exact2-time5-v1:permission-pressure",60*mib);
        String request="strong-resident1996:3:permission-pressure";
        GpuQualification1961.QueueDecision1983 first=
            GpuQualification1961.permissionSnapshot1996(request,48*mib);
        check(first.accepted&&first.reason==GpuQualification1961.QueueDecision1983.READY,
            "retry permission does not pretend to reserve memory");
        check(first.queuedJobs==1&&first.runningJobs==0&&first.retainedBytes==60*mib,
            "snapshot reports the actual existing queue");
        check(first.requestedBytes==48*mib,"requested size remains separate from retained size");
        for(int i=0;i<20;i++) {
            GpuQualification1961.QueueDecision1983 next=
                GpuQualification1961.permissionSnapshot1996(request,48*mib);
            check(next.retainedBytes==60*mib&&next.queuedJobs==1,"observation leaves queued ownership unchanged");
        }
        check(retained.closes.get()==0&&retained.runs.get()==0,"observation neither retires nor runs proof");
        check(GpuQualification1961.retainedBytes()==60*mib,"no hidden reservation or budget reclamation");
        GpuQualification1961.rejectSpeed(request);
        GpuQualification1961.QueueDecision1983 speed=
            GpuQualification1961.permissionSnapshot1996(request,48*mib);
        check(!speed.accepted&&speed.reason==GpuQualification1961.QueueDecision1983.RETRY_COOLDOWN,
            "speed cooldown has an actual reason rather than unspecified skipped");
        check(speed.retryRemainingNanos>0&&speed.retries==0,
            "initial speed failure has a cooldown without inventing a retry");
        for(int i=0;i<10;i++) {
            GpuQualification1961.QueueDecision1983 next=
                GpuQualification1961.permissionSnapshot1996(request,48*mib);
            check(next.retries==speed.retries&&next.reason==speed.reason,"observation spends no retry");
        }
        GpuQualification1961.rejectExact(request);
        GpuQualification1961.QueueDecision1983 exact=
            GpuQualification1961.permissionSnapshot1996(request,48*mib);
        check(!exact.accepted&&exact.reason==GpuQualification1961.QueueDecision1983.EXACT_REJECTED,
            "exact rejection remains authoritative");
        check(speed.reason==GpuQualification1961.QueueDecision1983.RETRY_COOLDOWN&&first.accepted,
            "earlier scalar snapshots do not change with current history");
        check(retained.closes.get()==0&&GpuQualification1961.retainedBytes()==60*mib,
            "rejected observations still preserve unrelated queued owner");
        check(GpuQualification1961.permissionSnapshot1996(null,1).reason==
            GpuQualification1961.QueueDecision1983.INVALID_KEY,"invalid key is identified");
        QueueModel1994Test.reset();
        check(retained.closes.get()==1,"ordinary capture cancellation still owns cleanup exactly once");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+
            ",\"permission_observation_no_queue_mutation\":true,\"physical_android_tested\":false}");
    }
}
