package com.hiro.ulike;
/** Calls the original row-profile fixture against actual115 with only the
 * controlled optional shape-profile overload and its preflight race added. */
public final class ResidentShapeProfile19115Test {
    static int assertions,cases;static boolean faulty;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void run(int initial,int after,boolean execute){
        GpuStrongDefault1990.shapeProfile=initial;GpuStrongDefault1990.nextShapeProfile=after;GpuStrongDefault1990.shapeCalls=0;
        ResidentFinishProfile19112Test.run(64,64,64,execute);
        check(GpuQualification1961.restoredKey.contains(":strong="+initial+";"),"whole proof binds the source-shape profile, not legacy fallback99");
        check(GpuStrongDefault1990.shapeCalls==(faulty?2:3),"source geometry rechecked after preflight unless reviewed guard was removed");cases++;
    }
    public static void main(String[] args){
        faulty=args.length==1&&"faulty".equals(args[0]);
        if(faulty)run(4,7,true);
        else {run(4,4,true);run(7,7,true);run(4,7,false);run(7,4,false);}
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+(assertions+ResidentFinishProfile19112Test.assertions)+",\"cases\":"+cases+",\"faulty_guard_removed\":"+faulty+"}");
    }
}
