import hiro.swiftkey.FlickSymbols;
/** Identical explicit native-field-shaped fixtures against old/new production Java. */
public final class NativePopupDigitRowProbe {
  public static void main(String[] args)throws Exception {
    String row="qwertyuiop",digits="1234567890";
    for(int col=0;col<10;col++) {
      FlickSymbolsHostTest.scenario();
      FlickSymbolsHostTest.View v=new FlickSymbolsHostTest.View(true,false);
      for(int i=2;i<=8;i++)new FlickSymbolsHostTest.PopupKey(v,row.charAt(i),digits.substring(i,i+1),i%3);
      char key=row.charAt(col);long d=FlickSymbolsHostTest.down(v,key);
      boolean started=FlickSymbols.pending(v);
      boolean move=FlickSymbolsHostTest.moveAt(v,key,d,0,30,30);
      boolean release=FlickSymbolsHostTest.up(v,key,d,0,45,90);
      String stage="";for(String line:FlickSymbolsHostTest.copyDiagnostic(v.service).split("\n"))if(line.startsWith("最後の段階:"))stage=line.substring(7).trim();
      System.out.println(key+"="+digits.charAt(col)+" start="+started+" move="+move+" up="+release+" count="+v.insertions.size()+" stage="+stage);
    }
  }
}
