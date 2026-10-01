package com.hiro.ulike.hdr.input.android;

import android.graphics.Rect;
import android.hardware.DataSpace;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import java.nio.ByteBuffer;
import com.hiro.ulike.hdr.input.*;

/** Tests adapter decisions/ownership using host doubles, not camera/HAL or SDK implementations. */
public final class AndroidP010FrameReaderTest {
    private static int checks;
    private static final CaptureRequest REQUEST = new CaptureRequest();
    private static final Object OWNER=new Object(), SESSION=new Object(), READER=new Object(), CALLBACK=new Object();
    private static final CaptureMatch.Context CONTEXT=context(null);
    private static final P010FrameReader.Request SPEC=new P010FrameReader.Request(2,2,HdrFrame.Encoding.BT2020_NCL_HLG_FULL,12);
    private interface Checked { void run() throws Exception; }
    public static void main(String[] args) throws Exception {
        FakeImage borrowed=new FakeImage();
        HdrFrame frame=copy(borrowed,false,CONTEXT,total(),CONTEXT);
        check(borrowed.closes==0 && frame.samples(HdrFrame.Component.Y).get(0)==513,"borrowed Image stays open; ten bits survive");
        FakeImage owned=new FakeImage(); copy(owned,true,CONTEXT,total(),CONTEXT);
        check(owned.closes==1,"owned success closes once");
        FakeImage bad=new FakeImage(); bad.dataSpace=0;
        expect(InputRejected.Reason.COLOR,()->copy(bad,true,CONTEXT,total(),CONTEXT));
        check(bad.closes==1,"owned rejection closes once");
        FakeImage borrowedBad=new FakeImage(); borrowedBad.dataSpace=0;
        expect(InputRejected.Reason.COLOR,()->copy(borrowedBad,false,CONTEXT,total(),CONTEXT));
        check(borrowedBad.closes==0,"borrowed rejection does not close");
        FakeImage badClose=new FakeImage(); badClose.dataSpace=0; badClose.throwOnClose=true;
        try { copy(badClose,true,CONTEXT,total(),CONTEXT); throw new AssertionError("accepted bad dataspace"); }
        catch(InputRejected e) { check(e.reason==InputRejected.Reason.COLOR && e.getSuppressed().length==1,"close failure does not mask rejection"); }
        check(badClose.closes==1,"throwing close attempted once");
        FakeImage consumed=new FakeImage(); consumed.planes[0].getBuffer().position(2);
        expect(InputRejected.Reason.BUFFER_BOUNDS,()->copy(consumed,true,CONTEXT,total(),CONTEXT));
        check(consumed.closes==1,"consumed cursor rejected and closed");
        CaptureMatch.Context physical=context("5");
        expect(InputRejected.Reason.RESULT,()->copy(new FakeImage(),true,physical,total(),physical));
        TotalCaptureResult result=total(); CaptureResult p=new CaptureResult();
        p.putForTest(CaptureResult.SENSOR_TIMESTAMP,1234L); result.physical.put("5",p);
        result.putForTest(CaptureResult.SENSOR_TIMESTAMP,9999L);
        HdrFrame physicalFrame=copy(new FakeImage(),true,physical,result,physical);
        check(physicalFrame.timestampNs==1234 && "5".equals(physicalFrame.physicalId),"physical timestamp used, not logical timestamp");
        TotalCaptureResult unrelated=new TotalCaptureResult(new CaptureRequest());
        unrelated.putForTest(CaptureResult.SENSOR_TIMESTAMP,1234L);
        expect(InputRejected.Reason.RESULT,()->copy(new FakeImage(),true,CONTEXT,unrelated,CONTEXT));
        check(AndroidP010FrameReader.encoding(DataSpace.STANDARD_BT2020|DataSpace.TRANSFER_HLG|DataSpace.RANGE_LIMITED)
                ==HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED,"limited range decoded explicitly");
        expect(InputRejected.Reason.COLOR,()->AndroidP010FrameReader.encoding(DataSpace.STANDARD_BT2020|DataSpace.TRANSFER_HLG));
        expect(InputRejected.Reason.COLOR,()->AndroidP010FrameReader.encoding((7<<16)|DataSpace.TRANSFER_HLG|DataSpace.RANGE_FULL));
        expect(InputRejected.Reason.COLOR,()->AndroidP010FrameReader.encoding(DataSpace.STANDARD_BT2020|(6<<22)|DataSpace.RANGE_FULL));
        expect(InputRejected.Reason.COLOR,()->AndroidP010FrameReader.encoding(DataSpace.STANDARD_BT2020|DataSpace.TRANSFER_HLG|DataSpace.RANGE_FULL|1));
        System.out.println("AndroidP010FrameReaderTest PASS ("+checks+" assertions; host doubles only)");
    }
    private static HdrFrame copy(FakeImage image,boolean close,CaptureMatch.Context c,TotalCaptureResult r,CaptureMatch.Context submitted) throws InputRejected {
        CaptureMatch.Source source=new CaptureMatch.Source(c.owner,c.session,c.reader,c.cameraId,c.physicalId,
                CaptureMatch.TimestampConvention.SENSOR_START_OF_EXPOSURE);
        return close ? AndroidP010FrameReader.copyAndClose(image,SPEC,c,source,r,submitted,()->c)
                : AndroidP010FrameReader.copyBorrowed(image,SPEC,c,source,r,submitted,()->c);
    }
    private static CaptureMatch.Context context(String physical) {
        return new CaptureMatch.Context(OWNER,SESSION,READER,REQUEST,CALLBACK,1,"0",physical);
    }
    private static TotalCaptureResult total() {
        TotalCaptureResult r=new TotalCaptureResult(REQUEST); r.putForTest(CaptureResult.SENSOR_TIMESTAMP,1234L); return r;
    }
    private static void check(boolean ok,String detail) { if(!ok) throw new AssertionError(detail); checks++; }
    private static void expect(InputRejected.Reason reason,Checked action) throws Exception {
        try { action.run(); throw new AssertionError("accepted "+reason); }
        catch(InputRejected e) { check(e.reason==reason,"expected "+reason+", got "+e.reason); }
    }
    private static final class FakeImage extends Image {
        int closes, dataSpace=DataSpace.STANDARD_BT2020|DataSpace.TRANSFER_HLG|DataSpace.RANGE_FULL;
        boolean throwOnClose;
        final Plane[] planes={plane(8,4,2),plane(2,4,4),plane(2,4,4)};
        FakeImage() { ByteBuffer y=planes[0].getBuffer(); int word=513<<6; y.put(0,(byte)word); y.put(1,(byte)(word>>>8)); }
        public int getFormat(){return 54;} public int getWidth(){return 2;} public int getHeight(){return 2;}
        public int getDataSpace(){return dataSpace;} public long getTimestamp(){return 1234;}
        public Rect getCropRect(){return new Rect(0,0,2,2);} public Plane[] getPlanes(){return planes;}
        public void close(){closes++; if(throwOnClose) throw new IllegalStateException("close failed");}
    }
    private static Image.Plane plane(int bytes,final int row,final int pixel) {
        final ByteBuffer data=ByteBuffer.allocate(bytes);
        return new Image.Plane(){ public ByteBuffer getBuffer(){return data;}
            public int getRowStride(){return row;} public int getPixelStride(){return pixel;} };
    }
}
