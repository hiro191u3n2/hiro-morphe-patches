package com.hiro.ulike;

import android.graphics.Bitmap;

/** The published Chroma186 neighbourhood sums and two colour/tone decisions,
 * evaluated in compute shaders. No live-photo CPU oracle or pixel arithmetic. */
final class GpuRequiredChroma1965 {
    private GpuRequiredChroma1965() { }
    static final int SHADER=46;
    private static void unchanged(Bitmap input,long generation) {
        if(input.isRecycled()||input.getGenerationId()!=generation)
            throw GpuRequiredFailure1965.forbidden("chroma-source-mutated");
    }
    static Bitmap apply(final Bitmap input,final int radius) {
        if(input==null||input.isRecycled()||input.getConfig()!=Bitmap.Config.ARGB_8888||radius<1||radius>128)
            throw new GpuRequiredFailure1965("chroma","GPU colour correction input unsupported");
        final int width=input.getWidth(),height=input.getHeight();
        final long sourceGeneration=input.getGenerationId();
        return GpuRequired1965.run("chroma",sourceGeneration,new GpuRequired1965.Work<Bitmap>() {
            public Bitmap run() {
                unchanged(input,sourceGeneration);
                if(!GpuNoise1960.supports(SHADER))return null;
                Bitmap output=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
                output.setDensity(input.getDensity());
                output.setHasAlpha(input.hasAlpha());
                output.setPremultiplied(input.isPremultiplied());
                boolean completed=false;
                try {
                    for(int first=0;first<height;first+=32) {
                        if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("GPU colour correction cancelled");
                        int core=Math.min(32,height-first),top=Math.max(0,first-radius),bottom=Math.min(height,first+core+radius);
                        int rows=bottom-top,begin=first-top,count=Math.multiplyExact(width,core);
                        long extra=8L*width*rows+48L*count+65536L;
                        if(!GpuNoise1960.workspaceFits(extra))return null;
                        int[] source=new int[Math.multiplyExact(width,rows)];
                        unchanged(input,sourceGeneration);input.getPixels(source,0,width,0,top,width,rows);
                        unchanged(input,sourceGeneration);
                        int[] candidate=new int[count];GpuNoise1960.Session session=GpuNoise1960.open();
                        if(session==null)return null;
                        try {
                            int[] params=new int[32];params[1]=width;params[2]=rows;params[3]=begin;params[4]=begin+core;params[5]=radius;
                            GpuNoise1960.Batch batch=new GpuNoise1960.Batch().upload(0,source).allocate(1,36L*count).allocate(2,4L*count)
                                .dispatch(SHADER,new int[]{0,1,2},params,new float[32],count);
                            params=params.clone();params[0]=1;batch.dispatch(SHADER,new int[]{0,1,2},params,new float[32],count);
                            if(!session.executeInto(batch,2,count,candidate,0))return null;
                        } finally {session.close();}
                        output.setPixels(candidate,0,width,0,first,width,core);
                    }
                    unchanged(input,sourceGeneration);completed=true;return output;
                } finally {if(!completed&&!output.isRecycled())output.recycle();}
            }
        });
    }
}
