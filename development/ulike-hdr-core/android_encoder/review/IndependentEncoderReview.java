import com.hiro.ulike.hdr.encoder.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
public class IndependentEncoderReview {
 static int checks;
 static void check(boolean ok) { checks++; if(!ok) throw new AssertionError("check="+checks); }
 static int code(ByteBuffer b,int p) { return ((b.get(p)&255)|((b.get(p+1)&255)<<8))>>>6; }
 public static void main(String[] args) throws Exception {
  for(int[] dim:new int[][]{{2,2},{4,4},{34,6},{64,32}}) {
   int w=dim[0],h=dim[1]; short[] yy=new short[w*h],cb=new short[w*h/4],cr=cb.clone();
   for(int i=0;i<yy.length;i++) yy[i]=(short)((i*43+1023)%1024);
   for(int i=0;i<cb.length;i++){cb[i]=(short)((i*13+55)%1024);cr[i]=(short)((i*61+875)%1024);}
   P010Frame f=new P010Frame(w,h,P010Frame.Encoding.BT2020_NCL_HLG_FULL,"capture","geometry","process",ShortBuffer.wrap(yy),ShortBuffer.wrap(cb),ShortBuffer.wrap(cr));
   int ys=w*2+12,us=w*2+20;
   ByteBuffer y=ByteBuffer.allocate(7+ys*h),u=ByteBuffer.allocate(11+us*h/2);Arrays.fill(y.array(),(byte)0x55);Arrays.fill(u.array(),(byte)0x66);
   y.position(3);u.position(5);ByteBuffer v=u.duplicate();v.position(7);
   f.copyTo(new P010Frame.Plane(y,ys,2),new P010Frame.Plane(u,us,4),new P010Frame.Plane(v,us,4));
   for(int r=0;r<h;r++) for(int c=0;c<w;c++) check(code(y,3+r*ys+2*c)==yy[r*w+c]);
   for(int r=0;r<h/2;r++) for(int c=0;c<w/2;c++) {check(code(u,5+r*us+4*c)==cb[r*w/2+c]);check(code(u,7+r*us+4*c)==cr[r*w/2+c]);}
   check(y.position()==3&&u.position()==5&&v.position()==7);check(y.get(2)==0x55&&u.get(4)==0x66);
   byte[] yBefore=y.array().clone(),uBefore=u.array().clone();
   boolean rejected=false;
   try {f.copyTo(new P010Frame.Plane(y,ys,2),new P010Frame.Plane(u,w*2-2,4),new P010Frame.Plane(v,w*2-2,4));}catch(IllegalArgumentException e){rejected=true;}
   check(rejected&&Arrays.equals(yBefore,y.array())&&Arrays.equals(uBefore,u.array()));
   rejected=false;
   try {f.copyTo(new P010Frame.Plane(y,ys,2),new P010Frame.Plane(u,us,4),new P010Frame.Plane(v,us+2,4));}catch(IllegalArgumentException e){rejected=true;}
   check(rejected&&Arrays.equals(yBefore,y.array())&&Arrays.equals(uBefore,u.array()));
  }
  byte[] hevc=Files.readAllBytes(Path.of(args[0]));HevcProof.Sps proof=HevcProof.inspect(hevc,66,34,false);
  check(proof.width==66&&proof.height==34&&proof.profile==2&&proof.lumaBits==10&&proof.chromaBits==10&&proof.primaries==9&&proof.transfer==18&&proof.matrix==9);
  System.out.println("{\"independent_checks\":"+checks+",\"status\":\"PASS\"}");
 }
}
