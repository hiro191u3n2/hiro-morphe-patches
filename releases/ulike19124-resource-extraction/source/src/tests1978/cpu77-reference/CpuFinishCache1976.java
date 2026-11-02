package com.hiro.ulike;

import java.util.Arrays;

/** Optional CPU neighbourhood reuse. A detached copy of one complete native
 * band sequence must match twice and beat the original sequence by five percent.
 * No Plan, callback, worker scratch, Bitmap or mutable mask survives the call. */
final class CpuFinishCache1976 {
    private CpuFinishCache1976() {}
    private static final Object SNAPSHOTS=new Object();
    static String key(int width,int rows,int first,int last,boolean variable,boolean moire,boolean sharp,
            int gain,int floor,int limit,boolean texture,boolean halo) {
        return "cpu-finish1976-exact2-time5-v1:"+width+":"+rows+":"+first+":"+last+":"+
            variable+":"+moire+":"+sharp+":"+gain+":"+floor+":"+limit+":"+texture+":"+halo;
    }
    static boolean enabled(String key) {
        try {
            GpuQualification1961.Record proof=GpuQualification1961.restore(key);
            return proof!=null&&proof.variant==1&&proof.cpuNanos>0&&proof.gpuNanos>0&&
                proof.gpuNanos<=proof.cpuNanos-proof.cpuNanos/20;
        } catch(RuntimeException unavailable){return false;}catch(LinkageError unavailable){return false;}
    }
    static Snapshot capture(String key,int[] source,int width,int rows,int first,int last,
            boolean variable,boolean moire,boolean sharp,int gain,int floor,int limit,boolean texture,boolean halo) {
        if(GpuQualification1961.background()||Thread.currentThread().isInterrupted()||last<=first)return null;
        try {synchronized(SNAPSHOTS) {
            long count=(long)width*rows,core=(long)width*(last-first);
            if(width<1||rows<1||first<0||last>rows||last-first>256||count>Integer.MAX_VALUE||
                    count<1||core>Integer.MAX_VALUE/4||source==null||source.length<count)return null;
            int bands=0;for(int at=first;at<last;){int end=Math.min(last,(at+16)&~15);if(end<=at)end=Math.min(last,at+16);bands++;at=end;}
            long retained=4L*count+(sharp?(variable?16L*core:16L*bands):0)+bands*128L+4096;
            // Three private outputs plus possible JNI source/target, policy and cache copies.
            long peak=20L*count+4L*1024*1024+256L*width+4096;
            if(retained>96L*1024*1024||!GpuQualification1961.canQueue(key,retained)||!GpuNoise1960.workspaceFits(retained+peak))return null;
            return new Snapshot(key,Arrays.copyOf(source,(int)count),width,rows,first,last,variable,moire,sharp,
                gain,floor,limit,texture,halo,bands,retained,peak);
        }}catch(RuntimeException unavailable){return null;}catch(LinkageError unavailable){return null;}catch(OutOfMemoryError unavailable){return null;}
    }
    static final class Snapshot implements GpuQualification1961.Probe {
        final String key;final int width,rows,first,last,gain,floor,limit;
        final boolean variable,moire,sharp,texture,halo;final long retained,peak;
        int[] source,starts,ends;int[][] policies;int recorded;
        Snapshot(String key,int[] source,int width,int rows,int first,int last,boolean variable,boolean moire,
                boolean sharp,int gain,int floor,int limit,boolean texture,boolean halo,int bands,long retained,long peak) {
            this.key=key;this.source=source;this.width=width;this.rows=rows;this.first=first;this.last=last;
            this.variable=variable;this.moire=moire;this.sharp=sharp;this.gain=gain;this.floor=floor;this.limit=limit;
            this.texture=texture;this.halo=halo;this.retained=retained;this.peak=peak;
            starts=new int[bands];ends=new int[bands];policies=new int[bands][];
        }
        void record(int start,int end,int[] policy) {
            if(source==null)return;
            try {
                if(recorded>=starts.length||start!=(recorded==0?first:ends[recorded-1])||end<=start||end>last)throw new IllegalStateException("CPU finish proof band");
                int count=sharp?(variable?Math.multiplyExact(Math.multiplyExact(width,end-start),4):4):0;
                if(count>0&&(policy==null||policy.length<count))throw new IllegalStateException("CPU finish proof policy");
                starts[recorded]=start;ends[recorded]=end;policies[recorded]=count==0?null:Arrays.copyOf(policy,count);recorded++;
            }catch(RuntimeException unavailable){close();}catch(OutOfMemoryError unavailable){close();}
        }
        void queue() {
            if(source==null||recorded!=starts.length||ends[recorded-1]!=last){close();return;}
            try {GpuQualification1961.schedule(key,retained,this);}
            catch(RuntimeException unavailable){close();}catch(LinkageError unavailable){close();}catch(OutOfMemoryError unavailable){close();}
        }
        public void run(GpuQualification1961.Cancellation cancellation) {
            if(source==null||cancellation.cancelled()||!GpuNoise1960.workspaceFits(peak))return;
            int[][] output={new int[source.length],new int[source.length]};int[] firstReference=null;
            long fastestOld=Long.MAX_VALUE,slowestNew=0;
            for(int trial=-1;trial<2;trial++) {
                long[] elapsed=new long[2];
                for(int turn=0;turn<2;turn++) {
                    if(cancellation.cancelled())return;int choice=(trial&1)==0?turn:1-turn;
                    Arrays.fill(output[choice],0);
                    long start=System.nanoTime();boolean okay=NativeMoire1951.proof1976(this,output[choice],choice==1);
                    elapsed[choice]=Math.max(1L,System.nanoTime()-start);
                    if(!okay||cancellation.cancelled())return;
                }
                if(!Arrays.equals(output[0],output[1])||trial==1&&!Arrays.equals(firstReference,output[0])){GpuQualification1961.rejectExact(key);return;}
                if(trial==0)firstReference=output[0].clone();
                if(trial>=0){fastestOld=Math.min(fastestOld,elapsed[0]);slowestNew=Math.max(slowestNew,elapsed[1]);}
            }
            if(cancellation.cancelled())return;
            if(slowestNew<=fastestOld-fastestOld/20)GpuQualification1961.qualified(key,fastestOld,slowestNew,1);
            else GpuQualification1961.rejectSpeed(key);
        }
        public void close(){source=null;starts=null;ends=null;policies=null;}
    }
}
