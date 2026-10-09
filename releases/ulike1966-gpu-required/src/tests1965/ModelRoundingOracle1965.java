package com.hiro.ulike;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Random;
/** Test-only Java operation used by frozen SingleNoise1955.probe. */
public final class ModelRoundingOracle1965 {
    public static void main(String[] args)throws Exception {
        ArrayList<Double> values=new ArrayList<Double>();
        values.add(0.0);values.add(-0.0);values.add(Double.MIN_VALUE);values.add(Double.MIN_NORMAL);
        for(int base=0;base<=8191;base++) {
            double half=base+.5,lower=Math.nextDown(half),upper=Math.nextUp(half);
            values.add(Math.nextDown(lower));values.add(lower);values.add(half);
            values.add(upper);values.add(Math.nextUp(upper));
        }
        Random random=new Random(1965);
        for(int i=0;i<2048;i++)values.add(random.nextDouble()*8192);
        int naiveMismatches=0;
        try(DataOutputStream out=new DataOutputStream(new FileOutputStream(args[0]))) {
            out.writeInt(196548);out.writeInt(values.size());
            for(double value:values) {
                long bits=Double.doubleToRawLongBits(value);
                int expected=(int)Math.round(value);
                out.writeInt((int)bits);out.writeInt((int)(bits>>>32));out.writeInt(expected);
                if((int)Math.floor(value+.5)!=expected)naiveMismatches++;
            }
        }
        if(naiveMismatches==0)throw new AssertionError("No binary64 half-boundary regression fixture");
        System.out.println("{\"status\":\"passed\",\"records\":"+values.size()+",\"naiveMismatches\":"+naiveMismatches+"}");
    }
}
