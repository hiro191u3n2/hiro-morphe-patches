import java.io.*;
/** Independent Java evaluation of the frozen CPU expression; no shader algorithm port. */
public strictfp final class RegionalOracle19107 {
    static float value(int bits){return Float.intBitsToFloat(bits);}
    public static void main(String[] args)throws Exception {
        long cases=0;
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])));
            DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[1])))){
            for(;;){int ai;try{ai=in.readInt();}catch(EOFException end){break;}
                int bi=in.readInt(),ci=in.readInt(),di=in.readInt(),px=in.readInt(),py=in.readInt();
                float a=value(ai),b=value(bi),c=value(ci),d=value(di),fx=px/128f,fy=py/128f;
                float result=(a*(1-fx)+b*fx)*(1-fy)+(c*(1-fx)+d*fx)*fy;
                out.writeInt(Float.floatToIntBits(result));cases++;
            }
        }
        System.out.println("RESULT {\"cases\":"+cases+",\"strictfp\":true,\"canonical_nan\":true}");
    }
}
