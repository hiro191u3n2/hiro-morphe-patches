package com.hiro.ulike.hdr.photo;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Real API33+ DCIM/Camera MediaStore publisher. Operates only on this app's exact UUID item. */
public final class MediaStorePhotos implements PendingPhotoStore {
    private static final String[] COLUMNS={MediaStore.Images.Media._ID,MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,MediaStore.MediaColumns.OWNER_PACKAGE_NAME,
            MediaStore.MediaColumns.IS_PENDING,MediaStore.MediaColumns.SIZE};
    private static final String MATCH=MediaStore.MediaColumns.DISPLAY_NAME+"=? AND "+MediaStore.MediaColumns.RELATIVE_PATH+"=? AND "+MediaStore.MediaColumns.OWNER_PACKAGE_NAME+"=?";
    private final ContentResolver resolver;private final String owner;private final Uri collection;
    public MediaStorePhotos(Context context){
        if(Build.VERSION.SDK_INT<33)throw new UnsupportedOperationException("HDR transaction requires API33+");
        Context app=Objects.requireNonNull(context).getApplicationContext();if(app==null)app=context;
        resolver=app.getContentResolver();owner=app.getPackageName();collection=MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
    }
    private String[] args(Plan p){return new String[]{p.displayName,p.relativePath,owner};}
    private Uri uri(String text)throws IOException{
        Uri value=Uri.parse(text);long id;
        try{id=ContentUris.parseId(value);}catch(RuntimeException e){throw new IOException("invalid media item URI",e);}
        if(id<1 || !ContentUris.withAppendedId(collection,id).equals(value))throw new IOException("unexpected MediaStore volume/item URI");return value;
    }
    private Bundle query(Plan plan){Bundle b=new Bundle();b.putString(ContentResolver.QUERY_ARG_SQL_SELECTION,MATCH);b.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,args(plan));b.putInt(MediaStore.QUERY_ARG_MATCH_PENDING,MediaStore.MATCH_INCLUDE);return b;}
    @Override public String insertPending(Plan plan)throws IOException{
        ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,plan.displayName);values.put(MediaStore.MediaColumns.MIME_TYPE,"image/heic");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,plan.relativePath);values.put(MediaStore.MediaColumns.IS_PENDING,1);values.put(MediaStore.Images.ImageColumns.DATE_TAKEN,plan.dateTakenMs);
        Uri created;
        try{created=resolver.insert(collection,values);}catch(RuntimeException e){throw new IOException("MediaStore pending insert failed",e);}
        if(created==null)throw new IOException("MediaStore insert returned no URI");uri(created.toString());return created.toString();
    }
    private Entry row(Cursor cursor,Plan plan,boolean inspectBytes)throws IOException{
        long id=cursor.getLong(0);String display=cursor.getString(1),relative=cursor.getString(2),packageName=cursor.getString(3);
        boolean owned=owner.equals(packageName),matching=plan.displayName.equals(display)&&plan.relativePath.equals(relative),pending=cursor.getInt(4)!=0;
        Uri image=ContentUris.withAppendedId(collection,id);long bytes=cursor.isNull(5)?-1:cursor.getLong(5);
        if(inspectBytes && owned && matching)try(ParcelFileDescriptor descriptor=resolver.openFileDescriptor(image,"r")){
            if(descriptor==null)throw new IOException("MediaStore read descriptor unavailable");bytes=descriptor.getStatSize();
        }
        return new Entry(image.toString(),owned,matching,pending,bytes);
    }
    @Override public Entry inspect(String text,Plan plan)throws IOException{
        try(Cursor cursor=resolver.query(uri(text),COLUMNS,query(plan),null)){
            if(cursor==null)throw new IOException("MediaStore ownership query returned null");if(!cursor.moveToFirst())return null;
            Entry entry=row(cursor,plan,true);if(cursor.moveToNext())throw new IOException("ambiguous media URI");return entry;
        }catch(RuntimeException e){throw new IOException("MediaStore inspect failed",e);}
    }
    @Override public List<Entry> find(Plan plan)throws IOException{
        List<Entry> entries=new ArrayList<>();
        try(Cursor cursor=resolver.query(collection,COLUMNS,query(plan),null)){
            if(cursor==null)throw new IOException("MediaStore recovery query returned null");
            while(cursor.moveToNext()){if(entries.size()>=8)throw new IOException("unexpected duplicate UUID photo names");entries.add(row(cursor,plan,false));}
        }catch(RuntimeException e){throw new IOException("MediaStore recovery query failed",e);}return entries;
    }
    @Override public void writeAndSync(String text,Plan plan,long expectedBytes,Write write)throws IOException{
        Entry before=inspect(text,plan);if(before==null || !before.owned || !before.matching || !before.pending || expectedBytes<1)throw new IOException("not an owned pending photo");
        ParcelFileDescriptor descriptor=resolver.openFileDescriptor(uri(text),"w");if(descriptor==null)throw new IOException("MediaStore output descriptor unavailable");
        try(ParcelFileDescriptor.AutoCloseOutputStream output=new ParcelFileDescriptor.AutoCloseOutputStream(descriptor)){
            class Counting extends OutputStream{
                long count;
                @Override public void write(int value)throws IOException{byte[] b={(byte)value};write(b,0,1);}
                @Override public void write(byte[] bytes,int offset,int length)throws IOException{
                    if(offset<0 || length<0 || (long)offset+length>bytes.length)throw new IndexOutOfBoundsException("output range");
                    if(length>expectedBytes-count)throw new IOException("photo output exceeds verified file bytes");
                    while(length>0){if(Thread.currentThread().isInterrupted())throw new IOException("photo output cancelled");int n=Math.min(length,65536);output.write(bytes,offset,n);count+=n;offset+=n;length-=n;}
                }
            }
            Counting counted=new Counting();write.to(counted);if(counted.count!=expectedBytes)throw new IOException("incomplete photo output");
            output.flush();output.getFD().sync();if(descriptor.getStatSize()!=expectedBytes)throw new IOException("MediaStore stored byte count mismatch");
        }
    }
    @Override public void publish(String text,Plan plan)throws IOException{
        ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.IS_PENDING,0);
        try{if(resolver.update(uri(text),values,MATCH+" AND "+MediaStore.MediaColumns.IS_PENDING+"=1",args(plan))!=1)throw new IOException("MediaStore publish did not update one pending image");}
        catch(RuntimeException e){throw new IOException("MediaStore publish failed",e);}
    }
    @Override public void deletePending(String text,Plan plan)throws IOException{
        try{resolver.delete(uri(text),MATCH+" AND "+MediaStore.MediaColumns.IS_PENDING+"=1",args(plan));}
        catch(RuntimeException e){throw new IOException("pending image cleanup failed",e);}
    }
}
