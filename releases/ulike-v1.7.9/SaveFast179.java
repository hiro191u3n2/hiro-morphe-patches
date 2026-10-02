package com.hiro.ulike;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import androidx.heifwriter.HeifWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/** Direct pending-MediaStore HEIF writer: same image pipeline, fewer file passes. */
public final class SaveFast179 {
    private static final long STOP_TIMEOUT_MS=45000L;
    private static final ExecutorService PREP=Executors.newSingleThreadExecutor(new ThreadFactory(){
        public Thread newThread(Runnable r){Thread t=new Thread(r,"ULike-HeifPrep179");t.setDaemon(true);return t;}
    });
    private SaveFast179(){}
    private static final class Pending {
        final ContentResolver resolver;final Uri uri;final ParcelFileDescriptor pfd;final HeifWriter writer;final int width,height;final String name;
        Pending(ContentResolver r,Uri u,ParcelFileDescriptor f,HeifWriter w,int x,int y,String n){resolver=r;uri=u;pfd=f;writer=w;width=x;height=y;name=n;}
    }
    public static String saveAndPublish(Bitmap input,int rotation){
        if(Build.VERSION.SDK_INT<29) return SaveQuality2.saveAndPublishLegacy179(input,rotation);
        Context app=SaveQuality2.app179();
        if(app==null||input==null||input.isRecycled()){SaveQuality2.failure179();return null;}
        if(Looper.myLooper()==Looper.getMainLooper()){SaveQuality2.status179("HEIF保存はワーカースレッドで実行します");SaveQuality2.failure179();return null;}
        boolean fixed=SaveQuality2.fixed179();
        int[] size=SaveQuality2.output179(input.getWidth(),input.getHeight(),rotation,fixed);
        if(size==null||size.length<2||size[0]<=0||size[1]<=0){SaveQuality2.status179("HEIF保存サイズを決定できません");SaveQuality2.failure179();return null;}
        final int width=size[0],height=size[1];
        final String name="ULike_"+System.currentTimeMillis()+"_"+UUID.randomUUID().toString().substring(0,8)+".heic";
        Future<Pending> future=PREP.submit(new Callable<Pending>(){public Pending call() throws Exception{return prepare(app,name,width,height);}});
        Bitmap rendered=null;Pending pending=null;
        try{
            Bitmap normalized=SaveQuality2.normalize179(input,rotation,fixed);
            rendered=PhotoDetail.applyDetail(normalized,input);
            if(rendered==null||rendered.isRecycled()||rendered.getWidth()!=width||rendered.getHeight()!=height) throw new IOException("size normalization failed");
            pending=future.get();
            pending.writer.start();
            pending.writer.addBitmap(rendered);
            pending.writer.stop(STOP_TIMEOUT_MS);
            pending.writer.close();
            if(!verifyOnce(pending)) throw new IOException("HEIF verification failed");
            ContentValues ready=new ContentValues();ready.put("is_pending",0);
            if(pending.resolver.update(pending.uri,ready,null,null)!=1) throw new IOException("MediaStore publish failed");
            String path=SaveIo168.checkedPath179(pending.resolver,pending.uri);
            SaveQuality2.status179("HEIFをDCIM/Cameraへ保存しました "+width+"×"+height);
            try{
                Intent intent=new Intent("com.lemon.faceu.action.scan_file");intent.setPackage(app.getPackageName());intent.putExtra("com.lemon.faceu.action.scan_file_key",path);app.sendBroadcast(intent);
            }catch(RuntimeException ignored){}
            return path;
        }catch(InterruptedException e){Thread.currentThread().interrupt();SaveQuality2.status179("HEIF保存を中断しました");cleanup(future,pending);SaveQuality2.failure179();return null;
        }catch(ExecutionException e){SaveQuality2.status179("HEIF準備に失敗しました: "+simple(e.getCause()));cleanup(future,pending);SaveQuality2.failure179();return null;
        }catch(Throwable e){SaveQuality2.status179("HEIF保存に失敗しました: "+simple(e));cleanup(future,pending);SaveQuality2.failure179();return null;
        }finally{
            if(rendered!=null&&rendered!=input&&!rendered.isRecycled())rendered.recycle();
            closePfd(pending);
        }
    }
    private static Pending prepare(Context app,String name,int width,int height)throws Exception{
        ContentResolver resolver=app.getContentResolver();
        ContentValues v=new ContentValues();
        v.put("_display_name",name);v.put("mime_type","image/heic");v.put("relative_path","DCIM/Camera");v.put("is_pending",1);v.put("width",width);v.put("height",height);v.put("orientation",0);v.put("datetaken",System.currentTimeMillis());
        Uri collection=Uri.parse("content://media/external_primary/images/media");
        Uri uri=resolver.insert(collection,v);if(uri==null)throw new IOException("MediaStore insert failed");
        ParcelFileDescriptor pfd=null;HeifWriter writer=null;
        try{
            pfd=resolver.openFileDescriptor(uri,"rw");if(pfd==null)throw new IOException("MediaStore fd failed");
            writer=new HeifWriter.Builder(pfd.getFileDescriptor(),width,height,2).setQuality(100).setMaxImages(1).setGridEnabled(true).setRotation(0).build();
            return new Pending(resolver,uri,pfd,writer,width,height,name);
        }catch(Throwable e){
            try{if(writer!=null)writer.close();}catch(Throwable ignored){}try{if(pfd!=null)pfd.close();}catch(Throwable ignored){}try{resolver.delete(uri,null,null);}catch(Throwable ignored){}throw e;
        }
    }
    private static boolean verifyOnce(Pending p){
        ParcelFileDescriptor check=null;
        try{
            check=p.resolver.openFileDescriptor(p.uri,"r");if(check==null||check.getStatSize()==0)return false;
            BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFileDescriptor(check.getFileDescriptor(),null,o);
            if(o.outWidth!=p.width||o.outHeight!=p.height)return false;
            String mime=o.outMimeType;return mime==null||mime.toLowerCase(Locale.ROOT).contains("hei");
        }catch(Throwable e){return false;}finally{try{if(check!=null)check.close();}catch(Throwable ignored){}}
    }
    private static void cleanup(Future<Pending> future,Pending known){
        Pending p=known;
        if(p==null&&future!=null){
            future.cancel(true);
            if(future.isDone()&&!future.isCancelled())try{p=future.get();}catch(Throwable ignored){}
        }
        if(p!=null){try{p.writer.close();}catch(Throwable ignored){}try{p.resolver.delete(p.uri,null,null);}catch(Throwable ignored){}closePfd(p);}
    }
    private static void closePfd(Pending p){if(p!=null)try{p.pfd.close();}catch(Throwable ignored){}}
    private static String simple(Throwable e){if(e==null)return "unknown";String n=e.getClass().getSimpleName();return n==null||n.length()==0?"error":n;}
}
