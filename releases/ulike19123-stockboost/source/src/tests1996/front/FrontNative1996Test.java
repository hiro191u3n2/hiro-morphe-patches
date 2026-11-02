package com.hiro.ulike;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;
public final class FrontNative1996Test {
 static int assertions;static final int W=32,H=32;
 static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
 static void state(int s,int reason)throws Exception{Field f=SingleNoise1955.class.getDeclaredField("nativeState");f.setAccessible(true);f.setInt(null,s);f=SingleNoise1955.class.getDeclaredField("nativeFailure1996");f.setAccessible(true);f.setInt(null,reason);}
 public static void main(String[] args)throws Exception{
  final int[] input=new int[W*H];Random random=new Random(196);
  for(int i=0;i<input.length;i++){int v=110+random.nextInt(37);input[i]=0xff000000|v<<16|v<<8|v;}
  SingleNoise1955.Model model=SingleNoise1955.probe(new SingleNoise1955.Patches(){public void read(int[] p,int x,int y,int w,int h){for(int r=0;r<h;r++)System.arraycopy(input,(y+r)*W+x,p,r*w,w);}},W,H);
  check(model.meanSigma()>=.3f,"case must enter native eligible route");CpuSingle1978.current=model;
  GpuPolicy1960.Protection protection=new GpuPolicy1960.Protection();int[] expected=new int[input.length];
  SingleNoise1955.processJavaRange(input,expected,W,H,0,H,0,H,0,3,false,model,protection);
  if(args.length>0){
   state(0,0);SingleNoise1955.Workspace ws=new SingleNoise1955.Workspace();ws.begin1996();int[] out=new int[input.length];
   SingleNoise1955.processRange(input,out,W,H,0,H,0,H,0,3,false,model,protection,ws);
   int category=args[0].equals("abi")?6:4;check(ws.frontRoutes1996[category]==1,"actual loading failure category");
   check(ws.frontRoutes1996[8]==1&&ws.frontWork1996[8]==0,"unavailable native does not invent JNI attempt");
   check(Arrays.equals(out,expected),"real load rejection preserves Java output");ws.close();
  }else{
   for(int mode=0;mode<3;mode++){
    state(1,0);CpuSingle1978.mode=mode;SingleNoise1955.Workspace ws=new SingleNoise1955.Workspace();ws.begin1996();
    int[] out=new int[input.length];SingleNoise1955.processRange(input,out,W,H,0,H,0,H,0,3,false,model,protection,ws);
    check(Arrays.equals(out,expected),"poisoned candidate cannot leak after false/symbol failure mode"+mode);
    check(ws.frontRoutes1996[mode]==1,"exact native result category mode"+mode);
    check(ws.frontWork1996[4]==1&&ws.frontWork1996[8]==1,"policy and native API attempts observed");
    check(ws.frontRoutes1996[3]==(mode==0?0:1)&&ws.frontRoutes1996[8]==(mode==0?0:1),"Java start and successful finish separated");
    check(ws.selectedBackend1982==0,"native and Java remain CPU output adoption");
    ws.begin1996();check(Arrays.equals(ws.frontWork1996,new long[24])&&Arrays.equals(ws.frontRoutes1996,new long[10]),"new strip clears scalar counters");ws.close();
   }
   state(-1,5);SingleNoise1955.Workspace ws=new SingleNoise1955.Workspace();ws.begin1996();int[] out=new int[input.length];
   SingleNoise1955.processRange(input,out,W,H,0,H,0,H,0,3,false,model,protection,ws);
   check(ws.frontRoutes1996[9]==1&&ws.frontWork1996[8]==0,"later strips distinguish cached symbol failure without retry");ws.close();
   state(1,0);CpuSingle1978.mode=0;ws=new SingleNoise1955.Workspace();SingleNoise1955.processRange(input,out,W,H,0,H,0,H,0,3,false,model,protection,ws);
   check(Arrays.equals(ws.frontWork1996,new long[24])&&Arrays.equals(ws.frontRoutes1996,new long[10]),"background/plain calls do not record foreground work");ws.close();
   ws=new SingleNoise1955.Workspace();ws.begin1996();SingleNoise1955.processRange(input,out,W,H,0,H,0,H,0,0,false,model,protection,ws);
   check(ws.frontRoutes1996[7]==1&&ws.frontRoutes1996[8]==1&&Arrays.equals(out,input),"clean bypass retains exact source and cause");ws.close();
  }
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+"}");
 }
}
