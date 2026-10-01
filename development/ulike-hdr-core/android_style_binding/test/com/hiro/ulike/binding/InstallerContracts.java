package com.hiro.ulike.binding;
import java.nio.file.*;import java.io.*;
public class InstallerContracts {
 static int checks;static void check(boolean b){checks++;if(!b)throw new AssertionError();}interface Work{void run()throws Exception;}static void fails(Work w)throws Exception{try{w.run();}catch(Exception expected){checks++;return;}throw new AssertionError("installer accepted invalid request");}
 public static void main(String[] args)throws Exception{
  File parent=new File(args[2]);check(parent.isDirectory());String[] ids={ShotStyleSettings.NATURAL,ShotStyleSettings.PURITY};String[] names={"natural","purity"};
  for(int i=0;i<2;i++){final File archive=new File(args[i]);final String style=ids[i],name=names[i];StyleObserverInstaller.Installation result=StyleObserverInstaller.install(archive,style,parent,name,314);check(result.nonce==314&&result.styleId.equals(style)&&result.copiedFiles==(i==0?45:143));check(result.expectedFeatures.equals(ObservedStyleFrame.expectedFeatures(style)));check(!result.actualDeviceDeliveryVerified&&!result.completeStyleBinding);Path sentinel=result.directory.toPath().resolve("sentinel.txt");Files.writeString(sentinel,"do not alter");fails(()->StyleObserverInstaller.install(archive,style,parent,name,315));check(Files.readString(sentinel).equals("do not alter"));Files.delete(sentinel);fails(()->StyleObserverInstaller.install(archive,style,parent,"badnonce",0));fails(()->StyleObserverInstaller.install(archive,style,parent,"../escape",314));fails(()->StyleObserverInstaller.install(archive,"unknown",parent,"unknown",314));}
  byte[] modified=Files.readAllBytes(Path.of(args[0]));modified[37]^=1;Path bad=parent.toPath().resolve("bad.zip");Files.write(bad,modified);fails(()->StyleObserverInstaller.install(bad.toFile(),ids[0],parent,"badarchive",314));check(!Files.exists(parent.toPath().resolve("badarchive")));Files.delete(bad);
  System.out.println("Android installer contracts PASS "+checks);
 }
}
