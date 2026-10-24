package com.hiro.ulike;

import java.util.Arrays;

/** Restore existing exact CPU neighbourhood reuse without creating speculative
 * photo copies. The retained owned-input verifier still requires two matching
 * complete native band sequences and a five-percent complete-call advantage. */
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
        // CPU finishing-cache trials repeatedly cost a foreground source and
        // policy copy, including when the after-save queue later cancels them.
        // Keep the original kernel or a pre-existing exact/speed certificate.
        return null;
    }
    static final class Snapshot implements GpuQualification1961.Probe {
        final String key;final int width,rows,first,last,gain,floor,limit;
        final boolean variable,moire,sharp,texture,halo;final long retained,peak;
        int[] source,starts,ends;int[][] policies;int recorded;
        GpuQualification1961.Reservation1984 reservation1993;
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
            try {
                GpuQualification1961.Reservation1984 reservation=reservation1993;
                if(reservation==null){close();return;}
                try{reservation.commit(this);}finally{reservation1993=null;reservation.close();}
            }
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
            else GpuQualification1961.rejectMeasuredSpeed1992(key,"CPU仕上げキャッシュ",fastestOld,slowestNew);
        }
        public void close(){source=null;starts=null;ends=null;policies=null;
            GpuQualification1961.Reservation1984 reservation=reservation1993;reservation1993=null;
            if(reservation!=null)reservation.close();}
    }
}
