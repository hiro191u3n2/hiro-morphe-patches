package com.hiro.ulike;

/** Mandatory GPU shot-policy calculation. Host operations copy immutable image
 * statistics, options and captured metadata into descriptors, then hydrate the
 * complete GPU Plan. No noise sigma, sharpening gain/floor, or remaining budget
 * is calculated on the host, including the local-noise budget override. */
public final class GpuRequiredPlan1965 {
    private GpuRequiredPlan1965(){}
    public static QualityPixels1932.Plan create(final QualityPixels1932.NoiseStats stats,
            final int iso,final long exposureNanos,final int lensKind,final float beautyStrength,
            final int requestedNoise,final int requestedSharp,final boolean texturePriority,
            final boolean shadowPriority,final float outputScale,final boolean haloSuppression,
            final SpatialNoise1934 localNoise,final QualityPixels1932.RegionMask faceRegions) {
        return GpuRequired1965.run("shot-policy",0,new GpuRequired1965.Work<QualityPixels1932.Plan>() {
            public QualityPixels1932.Plan run() {
                if(!GpuNoise1960.soft64Verified1965()||!GpuNoise1960.supports(GpuNoise1960.PLAN_SOFT1965))
                    throw new GpuRequiredFailure1965("shot-policy","GPU binary64 policy unavailable: "+GpuNoise1960.failure1965());
                int[] u=new int[32];float[] f=new float[32];
                u[0]=stats==null?0:1;u[1]=iso;u[2]=(int)exposureNanos;u[3]=(int)(exposureNanos>>>32);
                u[4]=lensKind;u[5]=requestedNoise;u[6]=requestedSharp;u[7]=stats==null?0:stats.samples;
                f[0]=stats==null?0:stats.lumaSigma;f[1]=stats==null?0:stats.chromaSigma;
                f[2]=beautyStrength;f[3]=outputScale;
                GpuNoise1960.Session session=GpuNoise1960.open();
                if(session==null)throw new GpuRequiredFailure1965("shot-policy","GPU session unavailable");
                try {
                    if(!session.allocate(0,36)||!session.dispatch(GpuNoise1960.PLAN_SOFT1965,new int[]{0},u,f,1))
                        throw new GpuRequiredFailure1965("shot-policy","GPU policy incomplete: "+GpuNoise1960.failure1965());
                    int[] words=session.readInts(0,9);
                    if(words==null)throw new GpuRequiredFailure1965("shot-policy","GPU policy readback incomplete");
                    GpuRequired1965.note("shot-policy","precision=portable-binary64-on-GPU shader="+GpuNoise1960.PLAN_SOFT1965);
                    return QualityPixels1932.fromGpu1965(words,texturePriority,shadowPriority,haloSuppression,localNoise,faceRegions);
                } finally {session.close();}
            }
        });
    }
}
