package com.hiro.ulike.hdr.photo;

import android.content.Context;
import android.net.Uri;
import com.hiro.ulike.hdr.gainmap.GainmapMath;
import com.hiro.ulike.hdr.gainmap.GainmapSave;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Concrete synchronous worker API for the app capture coordinator. */
public final class AndroidPhotoTransaction implements AutoCloseable {
    public final PhotoTransaction staging;
    private AndroidPhotoTransaction(PhotoTransaction staging){this.staging=staging;}
    private static Path root(Context context)throws IOException{
        Path root=context.getNoBackupFilesDir().toPath().resolve("ulike-photo-transactions-v1");Files.createDirectories(root);return root;
    }
    public static int recover(Context context)throws IOException{return PhotoTransaction.recover(root(context),new MediaStorePhotos(context));}
    public static AndroidPhotoTransaction begin(Context context,PhotoIdentity identity,long diskBudget)throws IOException{
        return new AndroidPhotoTransaction(PhotoTransaction.begin(root(context),identity,new MediaStorePhotos(context),diskBudget));
    }
    public Uri savePair(PairedStore.Pair pair,GainmapSave.QualityLimits limits,GainmapSave.Codec codec)throws IOException{
        return Uri.parse(staging.savePair(pair,limits,codec));
    }
    public Uri savePair(GainmapMath.Image processedSdr,GainmapMath.Image processedHdr,double headroom,
            GainmapSave.QualityLimits limits,GainmapSave.Codec codec)throws IOException{
        return Uri.parse(staging.savePair(processedSdr,processedHdr,headroom,limits,codec));
    }
    @Override public void close()throws IOException{staging.close();}
}
