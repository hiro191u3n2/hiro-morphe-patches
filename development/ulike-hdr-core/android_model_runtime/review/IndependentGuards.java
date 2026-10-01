package hiro.ulike.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/** Independent host guards. Fixture graph/weights remain private, never bundled. */
public final class IndependentGuards {
    interface Throwing { void call() throws Exception; }
    static int checks;
    static void check(boolean condition,String name) {
        if(!condition)throw new AssertionError(name); checks++;
    }
    static void reject(Throwing action,String name) throws Exception {
        boolean rejected=false;
        try { action.call(); } catch(IllegalArgumentException expected) { rejected=true; }
        check(rejected,name);
    }
    static ByteArrayInputStream stream(byte[] b) {return new ByteArrayInputStream(b);}
    static byte[] altered(byte[] b) {byte[] copy=b.clone();copy[copy.length/2]^=1;return copy;}
    static final class Tracked extends ByteArrayInputStream {
        boolean closed;
        Tracked(byte[] b){super(b);}
        @Override public void close(){closed=true;}
    }
    static PinnedModel.CompiledModel compile(PinnedModel.Style style,byte[] model,byte[] lib,byte[] effect) throws Exception {
        return PinnedModel.compile(style,stream(model),stream(lib),effect==null?null:stream(effect));
    }
    static void badGraph(PinnedModel.Style style,String graph,byte[] weights,String name) throws Exception {
        reject(()->GraphOnnx.compile(style,new PinnedModel.Decoded(graph,weights)),name);
    }
    static String replaceToken(String graph,int rowIndex,int tokenIndex,String value) {
        String[] rows=graph.split("\\n");String[] tokens=rows[rowIndex].trim().split("\\s+");
        tokens[tokenIndex]=value;rows[rowIndex]=String.join(" ",tokens);return String.join("\n",rows)+"\n";
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("baoman goodlike bytenn effect private-fixtures-dir");
        byte[] baoman=Files.readAllBytes(Paths.get(args[0])),goodlike=Files.readAllBytes(Paths.get(args[1]));
        byte[] bytenn=Files.readAllBytes(Paths.get(args[2])),effect=Files.readAllBytes(Paths.get(args[3]));
        Path dir=Paths.get(args[4]);
        reject(()->PinnedModel.compile(null,stream(baoman),stream(bytenn),null),"null style");
        reject(()->PinnedModel.compile(PinnedModel.Style.NATURAL_BLUSH,null,stream(bytenn),null),"null model");
        reject(()->PinnedModel.compile(PinnedModel.Style.NATURAL_BLUSH,stream(baoman),null,null),"null library");
        reject(()->compile(PinnedModel.Style.PURITY2,goodlike,bytenn,null),"Purity effect required");
        reject(()->compile(PinnedModel.Style.NATURAL_BLUSH,goodlike,bytenn,null),"wrong model Natural");
        reject(()->compile(PinnedModel.Style.PURITY2,baoman,bytenn,effect),"wrong model Purity");
        reject(()->compile(PinnedModel.Style.NATURAL_BLUSH,altered(baoman),bytenn,null),"model mutation");
        reject(()->compile(PinnedModel.Style.NATURAL_BLUSH,baoman,Arrays.copyOf(bytenn,bytenn.length-1),null),"truncated loader");
        reject(()->compile(PinnedModel.Style.PURITY2,goodlike,bytenn,altered(effect)),"effect mutation");
        reject(()->PinnedModel.readPinned(stream(new byte[0]),PinnedModel.sha(new byte[0])),"empty stream");
        byte[] shortBytes={0,1,2,3,4,5};
        InputStream zeroReads=new InputStream(){int i;@Override public int read(){return i<shortBytes.length?shortBytes[i++]:-1;}
            @Override public int read(byte[] b,int off,int len){return i<shortBytes.length?0:-1;}};
        check(Arrays.equals(PinnedModel.readPinned(zeroReads,PinnedModel.sha(shortBytes)),shortBytes),"zero-read progress");
        IOException sentinel=new IOException("test sentinel");
        InputStream broken=new InputStream(){@Override public int read()throws IOException{throw sentinel;}};
        try {PinnedModel.readPinned(broken,"");throw new AssertionError("read failure swallowed");}
        catch(IOException got){check(got==sentinel,"read failure preserved");}
        InputStream enormous=new InputStream(){long remaining=128L*1024*1024+1;
            @Override public int read(){if(remaining==0)return -1;remaining--;return 0;}
            @Override public int read(byte[] b,int off,int len){if(remaining==0)return -1;int n=(int)Math.min(remaining,len);Arrays.fill(b,off,off+n,(byte)0);remaining-=n;return n;}};
        reject(()->PinnedModel.readPinned(enormous,PinnedModel.sha(shortBytes)),"128 MiB stream cap");
        for(PinnedModel.Style style:PinnedModel.Style.values()) {
            String prefix=style==PinnedModel.Style.NATURAL_BLUSH?"baoman":"purity";
            byte[] raw=style==PinnedModel.Style.NATURAL_BLUSH?baoman:goodlike;
            Tracked a=new Tracked(raw),b=new Tracked(bytenn),c=new Tracked(effect);
            PinnedModel.CompiledModel model=PinnedModel.compile(style,a,b,c);
            check(!a.closed && !b.closed && !c.closed,"caller owns streams");
            byte[] onnx=model.copyOnnx(),copy=model.copyOnnx();copy[0]^=1;
            check(Arrays.equals(onnx,model.copyOnnx()),"defensive ONNX copy");
            long[] shape=model.inputShape();shape[0]=999;
            check(Arrays.equals(model.inputShape(),new long[]{1,3,256,256}),"defensive input shape");
            Files.write(dir.resolve(prefix+".java.onnx"),onnx);
            String graph=new String(Files.readAllBytes(dir.resolve(prefix+".graph")),"US-ASCII");
            byte[] weights=Files.readAllBytes(dir.resolve(prefix+".weights"));
            badGraph(style,graph,Arrays.copyOf(weights,weights.length-4),"truncated weights");
            badGraph(style,graph,Arrays.copyOf(weights,weights.length+4),"extra weights");
            byte[] nonfinite=weights.clone();nonfinite[0]=0;nonfinite[1]=0;nonfinite[2]=(byte)0x80;nonfinite[3]=0x7f;
            badGraph(style,graph,nonfinite,"infinite weight");
            badGraph(style,graph+"\0",weights,"NUL graph");
            badGraph(style,graph+"\\",weights,"backslash graph");
            badGraph(style,replaceToken(graph,0,1,"0"),weights,"operator count");
            badGraph(style,replaceToken(graph,0,2,"0"),weights,"marker mismatch");
            badGraph(style,replaceToken(graph,1,3,"512"),weights,"input shape");
            badGraph(style,replaceToken(graph,2,0,"UnknownOp"),weights,"operator subset");
            badGraph(style,replaceToken(graph,2,1,"@replay/collision"),weights,"reserved name");
            badGraph(style,replaceToken(graph,2,2,"9999999999"),weights,"integer overflow");
            badGraph(style,replaceToken(graph,2,2,"-1"),weights,"negative integer");
            badGraph(style,replaceToken(graph,2,2,"01"),weights,"noncanonical integer");
            badGraph(style,replaceToken(graph,2,3,"0"),weights,"zero kernel");
            badGraph(style,replaceToken(graph,2,9,"0"),weights,"unsupported bias");
            badGraph(style,replaceToken(graph,2,10,"2"),weights,"unsupported activation");
            badGraph(style,replaceToken(graph,2,11,"2"),weights,"dtype mismatch");
            badGraph(style,replaceToken(graph,2,17,"missing_tensor"),weights,"nontopological input");
            badGraph(style,replaceToken(graph,2,18,"data"),weights,"duplicate output");
            String[] rows=graph.split("\\n"),first=rows[2].split("\\s+");
            badGraph(style,replaceToken(graph,3,1,first[1]),weights,"duplicate operator label");
            String third=rows[3].split("\\s+")[0];
            if(third.equals("Convolution") || third.equals("DepthwiseSeparableConvolution")) {
                String firstOutput=first[18];
                badGraph(style,replaceToken(graph,3,18,firstOutput),weights,"duplicate intermediate output");
            }
        }
        System.out.println("PASS independent_guard_checks="+checks);
    }
}
