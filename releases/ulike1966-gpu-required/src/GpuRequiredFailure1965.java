package com.hiro.ulike;

/** A required noise/correction stage did not finish. The original photograph
 * remains owned by the capture; saving an unprocessed substitute is forbidden. */
public final class GpuRequiredFailure1965 extends RuntimeException {
    public final String stage;
    public final boolean fallbackForbidden;
    public GpuRequiredFailure1965(String stage,String message){this(stage,message,null,false);}
    public GpuRequiredFailure1965(String stage,String message,Throwable cause){this(stage,message,cause,false);}
    private GpuRequiredFailure1965(String stage,String message,Throwable cause,boolean forbidden){
        super("GPU required ["+stage+"]: "+message,cause);this.stage=stage;fallbackForbidden=forbidden;
    }
    public static GpuRequiredFailure1965 forbidden(String stage){
        return new GpuRequiredFailure1965(stage,"CPU or unprocessed source fallback is forbidden",null,true);
    }
}
