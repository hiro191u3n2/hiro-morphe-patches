package com.hiro.ulike.hdr.photo;

import com.hiro.ulike.hdr.beauty.HdrBeautyProcessor;
import com.hiro.ulike.hdr.gainmap.GainmapMath;
import com.hiro.ulike.hdr.gainmap.GainmapSave;
import com.hiro.ulike.hdr.gainmap.PhotoTestCodecFactory;
import com.hiro.ulike.hdr.input.HdrBeautyIntegration;
import com.hiro.ulike.hdr.input.SmoothPhotoFixture;
import hiro.ulike.model.PinnedModel;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Actual pinned model + synthetic P010/frame-resolved style fixture to final photo bytes.
 * No real portrait, phone Camera2 or unimplemented native style binding is claimed. */
public final class ActualHdrPhotoTest {
    private static void raw(Path path,GainmapMath.Image image)throws Exception{
        double[] row=new double[image.frame.width*3];ByteBuffer bytes=ByteBuffer.allocate(row.length*8).order(ByteOrder.LITTLE_ENDIAN);
        try(OutputStream out=Files.newOutputStream(path)){for(int y=0;y<image.frame.height;y++){image.read(y,row);bytes.clear();for(double v:row)bytes.putDouble(v);out.write(bytes.array());}}
    }
    public static void main(String[] args)throws Exception{
        PinnedModel.Style style=PinnedModel.Style.valueOf(args[0]);Path work=Paths.get(args[5]);Files.createDirectories(work);
        boolean sharp=args.length>6&&args[6].equals("sharp");
        PhotoGeometry geometry=new PhotoGeometry(512,384,90,true,1,2,382,508);
        HdrBeautyIntegration.Fixture fixture=HdrBeautyIntegration.prepareFromFrame(style,new File(args[1]),new File(args[2]),new File(args[3]),new File(args[4]),
                sharp?HdrBeautyIntegration.fixture(512,384):SmoothPhotoFixture.create(512,384),geometry.id());
        HdrBeautyProcessor.Snapshot snapshot=fixture.snapshot;
        PhotoIdentity identity=new PhotoIdentity(snapshot.frameIdentity,geometry.width,geometry.height,snapshot.captureId,snapshot.geometryId,
                "hdr-layerwise-source-over-and-style-v1",snapshot.settingsSnapshotBytes(),1790815000000L);
        if(!identity.settingsSha256.equals(snapshot.settingsSha256))throw new AssertionError("canonical settings identity changed");
        Path root=work.resolve("private"),gallery=work.resolve("gallery"),codec=work.resolve("codec");Files.createDirectories(root);Files.createDirectories(codec);
        TransactionTest.Store store=new TransactionTest.Store(gallery,"");String uri=null,rejection=null;long workspace;HdrBeautyProcessor.Result appearance;
        try(PhotoTransaction tx=PhotoTransaction.begin(root,identity,store,256L*1024*1024)){
            GeometryPairWriter transformed=tx.createTransformedPair(geometry,snapshot.policy.headroom(),4*1024*1024);
            appearance=HdrBeautyProcessor.render(snapshot,HdrBeautyIntegration.bindings(true),transformed.asHdrSink(),HdrBeautyProcessor.Budget.standard());
            PairedStore.Pair pair=transformed.pair();workspace=transformed.tileArrayWorkspaceBound;
            raw(work.resolve("expected.sdr"),pair.sdr());raw(work.resolve("expected.hdr"),pair.hdr());
            try {
                uri=tx.savePair(pair,new GainmapSave.QualityLimits(.2,.5,.05),PhotoTestCodecFactory.open(codec,style.name()));
                if(sharp)throw new AssertionError("sharp fixture unexpectedly passed quality gate; review expected rejection");
                if(store.inspect(uri,tx.plan).pending)throw new AssertionError("callback before publication");
            } catch(IOException failure) {
                if(!sharp||!failure.getMessage().startsWith("actual decoded SDR/HDR reconstruction exceeds explicit quality limits:"))throw failure;
                rejection=failure.getMessage();
            }
        }
        if(store.pending()!=0 || store.photos()!=(sharp?0:1))throw new AssertionError("partial or duplicate photo");
        try(java.util.stream.Stream<Path> files=Files.list(root)){if(files.anyMatch(p->p.getFileName().toString().startsWith("tx-")))throw new AssertionError("private transaction not cleaned");}
        System.out.println("{\"style\":\""+style+"\",\"status\":\"PASS\",\"actual_pinned_model\":true,\"fixture\":\""+(sharp?"sharp_rejection":"smooth_success")+"\",\"quality_rejection\":"+(rejection==null?"null":"\""+rejection+"\"")+",\"published_photo_count\":"+store.photos()+",\"source_raster\":[512,384],\"output_raster\":[382,508],\"rotation_clockwise\":90,\"mirror\":true,\"crop\":[1,2,382,508],\"tile_workspace_bytes\":"+workspace+",\"post_style_changed_pixels\":"+appearance.changedStagePixels+",\"full_native_style_bindings\":false,\"android_device_execution\":false}");
    }
}
