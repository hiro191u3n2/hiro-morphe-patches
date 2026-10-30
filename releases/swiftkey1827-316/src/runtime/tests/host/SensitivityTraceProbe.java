import hiro.swiftkey.FlickSymbols;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/** Identical event sequences against pinned old and current production classes. */
public final class SensitivityTraceProbe extends FlickSymbolsHostTest {
    public static void main(String[] args) throws Exception {
        boolean explicit=args.length>0&&args[0].equals("standard");int index=0;
        for(int scale:new int[]{1,2})for(int slop:new int[]{0,24})
        for(float angle:new float[]{0,-45,45,-65,65,80,180})
        for(float length:new float[]{3,6,12,18,19.2f,21,30,45})for(int sampling=0;sampling<3;sampling++) {
            scenario();View v=new View(true,false);v.width*=scale;v.height*=scale;ViewConfiguration.scaledTouchSlop=slop*scale;
            if(explicit)v.prefs.intValues.put("hiro_up_flick_sensitivity",2);
            long d=down(v,'a');NativeKey key=v.key('a');float dx=horizontalAt(angle,length*scale),up=upwardAt(angle,length*scale);
            boolean moved=false,released;
            if(sampling==0)released=up(v,'a',d,dx,up,80);
            else if(sampling==1){moved=moveAt(v,'a',d,dx,up,20);released=up(v,'a',d,0,2,80);}
            else {MotionEvent e=event(1,key.x(v),key.y(v)-2,d,80);e.history=new float[][]{{key.x(v)+dx,key.y(v)-up}};released=dispatch(v,e);}
            System.out.println((index++)+"|"+moved+"|"+released+"|"+v.insertions+"|"+v.cancelCalls+"|"+v.tapUps+"|"+v.events+"|"+FlickSymbols.pending(v));
        }
    }
}

