package com.hiro.ulike;

/** Whole-call time and Strong plan feed the active resident speed guard.
 * No image, model, session, result, certificate, or decision is retained here. */
public final class ResidentTiming19111 {
    private ResidentTiming19111() {}
    private static final int PHASES=14, LENGTH=29;
    private static final long LIMIT=86400000000000L;
    private static final ThreadLocal<Sample> CURRENT=new ThreadLocal<Sample>();
    private static final ThreadLocal<CpuSample19115> CPU19115=new ThreadLocal<CpuSample19115>();
    /** Existing CPU oracle calls only. These boundaries exclude the model and
     * post-Strong image signatures; no pixels or replay work are retained. */
    private static final class CpuSample19115 {
        final long[] values=new long[17];
        long last;int active=-1,highest=1;boolean stopped,valid=true;
        CpuSample19115(){values[0]=1;values[2]=values[3]=-1;for(int i=6;i<9;i++)values[i]=-1;for(int i=10;i<17;i++)values[i]=-1;}
        void interval(long now,boolean complete){
            if(active<2||active>3)return;
            long delta=now-last;if(delta<0||delta>LIMIT){valid=false;return;}
            int bit=1<<(active-2);values[active]=delta;
            if(complete)values[4]|=bit;else values[5]|=bit;
            active=-1;
        }
        void phase(long phase,long now){
            if(stopped||phase<2||phase>4)return;
            if(phase<highest){valid=false;return;}
            if(phase==highest)return;
            interval(now,phase==highest+1);highest=(int)phase;
            if(phase<4){active=(int)phase;last=now;}
        }
        void stop(long now,int status){
            if(stopped)return;interval(now,false);stopped=true;
            if(status==1&&values[5]!=0)status=4;
            values[1]=status;
            if(status!=1){values[5]|=values[4];values[4]=0;}
        }
    }
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
            if("beginCpu19115".equals(operation)){
                CPU19115.remove();GpuQualification1961.Cancellation owner=GpuResident1976.cancellation();
                if(owner==null||owner.cancelled()||!GpuQualification1961.background()||!GpuResident1976.cpuOracle()||
                        args==null||args.length<1||args[0]<0||args[0]>1)return;
                CPU19115.set(new CpuSample19115());return;
            }
            CpuSample19115 cpu=CPU19115.get();
            if(cpu!=null){
                if("publishCpu19115".equals(operation)){
                    CPU19115.remove();GpuQualification1961.Cancellation owner=GpuResident1976.cancellation();
                    boolean cancelled=Thread.currentThread().isInterrupted()||owner!=null&&owner.cancelled();
                    if(!cpu.stopped)cpu.stop(System.nanoTime(),cancelled?3:2);
                    else if(cancelled){cpu.values[1]=3;cpu.values[5]|=cpu.values[4];cpu.values[4]=0;}
                    if(cpu.valid)Class.forName("com.hiro.ulike.GpuQualification1961")
                        .getMethod("residentCpuTiming19115",long[].class).invoke(null,(Object)cpu.values.clone());
                    return;
                }
                if(!cpu.stopped&&args!=null&&args.length>0){
                    if("phase".equals(operation))cpu.phase(args[0],System.nanoTime());
                    else if("stopCpu19115".equals(operation)&&args[0]>=1&&args[0]<=3)cpu.stop(System.nanoTime(),(int)args[0]);
                    else if("strongPlan".equals(operation)&&args.length>=3&&args[0]>=1&&args[0]<=64&&args[1]>=1&&args[1]<=65536&&args[2]>=0&&args[2]<=Long.MAX_VALUE/4){
                        cpu.values[6]=args[0];cpu.values[7]=args[1];cpu.values[8]=args[2];
                    }else if("cpuStrongCounts19115".equals(operation)&&args.length==7){
                        boolean valid=true;for(long count:args)if(count<0||count>1000000)valid=false;
                        if(valid&&cpu.values[9]==0){cpu.values[9]=1;System.arraycopy(args,0,cpu.values,10,7);}
                    }
                }
            }
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
            try{CURRENT.remove();CPU19115.remove();}catch(Throwable ignored){}
        }
    }
}
