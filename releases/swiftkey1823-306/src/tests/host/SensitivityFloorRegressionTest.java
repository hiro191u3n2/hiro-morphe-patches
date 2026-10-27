import android.view.MotionEvent;
import android.view.ViewConfiguration;
import java.util.Arrays;

/** Regression for platform touch-slop floors erasing sensitivity differences. */
public final class SensitivityFloorRegressionTest extends SensitivityHostTestSupport {
    static int firstCapture(int level,int slop,float degrees,boolean historical) throws Exception {
        View v=configured(level,false);ViewConfiguration.scaledTouchSlop=slop;
        long d=down(v,'a');NativeKey key=v.key('a');
        for(int length=1;length<=160;length++) {
            MotionEvent move=event(2,key.x(v)+horizontalAt(degrees,length),key.y(v)-upwardAt(degrees,length),d,20+length);
            if(historical)move.history=new float[][]{
                {key.x(v)+horizontalAt(degrees,length-.75f),key.y(v)-upwardAt(degrees,length-.75f)},
                {key.x(v)+horizontalAt(degrees,length-.5f),key.y(v)-upwardAt(degrees,length-.5f)},
                {key.x(v)+horizontalAt(degrees,length-.25f),key.y(v)-upwardAt(degrees,length-.25f)}};
            if(dispatch(v,move)) {
                check(v.cancelCalls==1&&v.insertions.isEmpty(),"one native cancel at first qualifying sampled movement");
                check(up(v,'a',d,0,1,250),"owned gesture remains captured when UP returns near its start");
                oneSymbol(v,"sampled floor-regression flick");
                return length;
            }
        }
        throw new AssertionError("WIDE_DIAGONAL_MUST_REMAIN_REACHABLE at level="+level+" slop="+slop+" angle="+degrees);
    }

    static int[] thresholds(int slop,float angle,boolean historical) throws Exception {
        int[] values=new int[5];
        for(int level=0;level<5;level++)values[level]=firstCapture(level,slop,angle,historical);
        return values;
    }

    static void separation() throws Exception {
        for(float angle:new float[]{65,-65})for(boolean historical:new boolean[]{false,true})for(int level:new int[]{0,4}) {
            View v=configured(level,false);ViewConfiguration.scaledTouchSlop=40;
            long d=down(v,'a');NativeKey key=v.key('a');
            for(int length:new int[]{8,16,24,32,40,48}) {
                MotionEvent move=event(2,key.x(v)+horizontalAt(angle,length),key.y(v)-upwardAt(angle,length),d,20+length);
                if(historical)move.history=new float[][]{
                    {key.x(v)+horizontalAt(angle,length-6),key.y(v)-upwardAt(angle,length-6)},
                    {key.x(v)+horizontalAt(angle,length-4),key.y(v)-upwardAt(angle,length-4)},
                    {key.x(v)+horizontalAt(angle,length-2),key.y(v)-upwardAt(angle,length-2)}};
                boolean owned=dispatch(v,move);
                if(level==0)check(!owned,"LOW_LEVEL_MUST_REJECT_CAPPED_SLOP_DIAGONAL: 48px path at65degrees must stay below very-low sensitivity");
            }
            if(level==0) {
                check(!up(v,'a',d,horizontalAt(angle,48),upwardAt(angle,48),100),"low-sensitivity UP cannot introduce a delayed capture");
                check(v.insertions.isEmpty()&&v.cancelCalls==0&&v.tapUps==1,"very-low setting leaves the same short diagonal as one native tap");
            } else {
                check(up(v,'a',d,0,1,100),"very-high setting already owns the identical short diagonal");
                oneSymbol(v,"low/high sensitivity physical separation");
            }
        }
    }

    static void grid() throws Exception {
        final int[] slops={0,12,24,40,80};
        final float[] angles={0,45,65,67};
        int[][][] moveResults=new int[slops.length][angles.length][];
        for(int s=0;s<slops.length;s++)for(int a=0;a<angles.length;a++)for(boolean historical:new boolean[]{false,true}) {
            int[] values=thresholds(slops[s],angles[a],historical);
            for(int level=1;level<5;level++)check(values[level-1]>values[level],"ALL_FIVE_LEVELS_MUST_REMAIN_DISTINCT: slop="+slops[s]+" angle="+angles[a]+" history="+historical+" thresholds="+Arrays.toString(values));
            if(!historical)moveResults[s][a]=values;
            else check(Arrays.equals(values,moveResults[s][a]),"same first-capture event with intermediate historical samples");
            if(slops[s]==80)check(Arrays.equals(values,moveResults[s-1][a]),"platform movement tolerance remains capped at half the80px key");
            if(slops[s]>=40&&angles[a]>=65)check(values[0]>80&&values[0]<160,"lowest sensitivity wide diagonal reaches its later threshold before direction handoff");
        }
    }

    public static void main(String[] args) throws Exception {
        if(args.length>0&&args[0].equals("probe")) {
            System.out.println("PROBE "+Arrays.toString(thresholds(40,65,true)));return;
        }
        if(args.length>0&&args[0].equals("separation")) {separation();return;}
        separation();grid();
        System.out.println("PASS Sensitivity floor regression: "+scenarios+" scenarios, "+assertions+" assertions");
        System.out.println("REPORT {\"status\":\"PASSED\",\"device_tested\":false,\"android_instrumentation_tested\":false,\"scenarios\":"+scenarios+",\"assertions\":"+assertions+"}");
    }
}

/** Reuse explicit host setup without inheriting a test main or production code. */
class SensitivityHostTestSupport extends FlickSymbolsHostTest {
    static View configured(int level,boolean glide) {
        scenario();android.app.AlertDialog.reset();View v=new View(true,glide);
        v.prefs.intValues.put("hiro_up_flick_sensitivity",level);return v;
    }
}
