package com.hiro.ulike;

/** Bounded observations of the private resident speed trials. No image, model,
 * session, result, certificate, or admission decision is retained here. */
public final class ResidentTiming19111 {
    private ResidentTiming19111() {}
    private static final int PHASES=14, LENGTH=29;
    private static final long LIMIT=86400000000000L;
    private static final ThreadLocal<Sample> CURRENT=new ThreadLocal<Sample>();
    private static final class Sample {
        final long[] values=new long[LENGTH];
        long started,last;int active=-1;boolean running,stopped,valid=true;
        Sample(){values[0]=1;for(int i=21;i<LENGTH;i++)values[i]=-1;}
        void interval(long now,boolean complete){
            long delta=now-last;
            if(delta<0||delta>LIMIT){valid=false;return;}
            if(active>=0){
                if(values[7+active]>LIMIT-delta){valid=false;return;}
                values[7+active]+=delta;
                long bit=1L<<active;
                if(complete){values[5]|=bit;values[6]&=~bit;}
                else {values[6]|=bit;values[5]&=~bit;}
            }
            last=now;
        }
        void stop(long now,int status){
            if(!running||stopped)return;
            interval(now,status==1);stopped=true;
            long wall=now-started,subtotal=0;
            if(wall<0||wall>LIMIT){valid=false;return;}
            for(int i=0;i<PHASES;i++){
                if(values[7+i]<0||subtotal>LIMIT-values[7+i]){valid=false;return;}
                subtotal+=values[7+i];
            }
            if(subtotal>wall){valid=false;return;}
            // Cleanup can run after a failed producer. Without intercepting
            // that producer's exception, no visited phase is certified complete.
            if(status!=1){values[6]|=values[5];values[5]=0;}
            values[1]=status;values[2]=wall;values[3]=subtotal;values[4]=wall-subtotal;
        }
    }
    /** Reflection entry point keeps diagnostic-only dependencies out of old
     * standalone host fixtures. Every failure is confined to this observer. */
    public static void dispatch(String operation,long[] args){
        try {
            if("begin".equals(operation)){
                CURRENT.remove();
                GpuQualification1961.Cancellation owner=GpuResident1976.cancellation();
                if(owner==null||owner.cancelled()||!GpuQualification1961.background()||GpuResident1976.cpuOracle())return;
                if(args==null||args.length<2||args[0]<0||args[0]>1||args[1]<0||args[1]>1)return;
                CURRENT.set(new Sample());return;
            }
            Sample value=CURRENT.get();if(value==null)return;
            if("publish".equals(operation)){
                CURRENT.remove();
                GpuQualification1961.Cancellation owner=GpuResident1976.cancellation();
                boolean cancelled=Thread.currentThread().isInterrupted()||owner!=null&&owner.cancelled();
                if(!value.stopped)value.stop(System.nanoTime(),cancelled?3:2);
                else if(cancelled){value.values[1]=3;value.values[6]|=value.values[5];value.values[5]=0;}
                if(!value.running||!value.stopped||!value.valid)return;
                Class<?> sink=Class.forName("com.hiro.ulike.GpuQualification1961");
                sink.getMethod("residentTiming19111",long[].class).invoke(null,(Object)value.values.clone());
                return;
            }
            if(args==null||args.length==0||value.stopped)return;
            if("start".equals(operation)){
                if(value.running){value.valid=false;return;}
                value.started=args[0];value.last=args[0];value.running=true;return;
            }
            if(!value.running)return;
            if("phase".equals(operation)){
                int phase=(int)args[0];
                if(args[0]!=phase||phase<0||phase>=PHASES){value.valid=false;return;}
                if(phase!=value.active){value.interval(System.nanoTime(),true);value.active=phase;}
            }else if("stop".equals(operation)){
                if(args.length<2||args[1]<1||args[1]>3){value.valid=false;return;}
                value.stop(args[0],(int)args[1]);
            }else if("strongPlan".equals(operation)){
                if(args.length<3||args[0]<1||args[0]>64||args[1]<1||args[1]>65536||args[2]<0)return;
                value.values[21]=args[0];value.values[22]=args[1];value.values[23]=args[2];
            }else if("strongCounts".equals(operation)){
                if(args.length<2||args[0]<0||args[1]<0||args[0]>1000000||args[1]>1000000)return;
                value.values[24]=args[0];value.values[25]=args[1];
            }else if("finishRows".equals(operation)){
                if(args[0]<1||args[0]>65536)return;
                value.values[26]=args[0];
                if(value.values[27]<0)value.values[27]=0;
                if(value.values[28]<0)value.values[28]=0;
            }else if("geometryBand".equals(operation)){
                if(value.values[27]>=0&&value.values[27]<1000000)value.values[27]++;
            }else if("finishBand".equals(operation)){
                if(value.values[28]>=0&&value.values[28]<1000000)value.values[28]++;
            }
        } catch(Throwable optional) {
            try{CURRENT.remove();}catch(Throwable ignored){}
        }
    }
}
