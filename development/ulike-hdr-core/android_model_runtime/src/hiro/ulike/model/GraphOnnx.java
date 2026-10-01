package hiro.ulike.model;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static hiro.ulike.model.PinnedModel.require;

/** Minimal ONNX protobuf encoder for the audited, SHA-pinned operator subset. */
final class GraphOnnx {
    private GraphOnnx() {}
    static final class Proto {
        private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        private void raw(long value) { while((value & ~127L)!=0){bytes.write((int)(value&127)|128);value>>>=7;}bytes.write((int)value); }
        Proto integer(int field,long value){require(field>0 && value>=0,"protobuf integer");raw((long)field<<3);raw(value);return this;}
        Proto data(int field,byte[] value){raw(((long)field<<3)|2);raw(value.length);bytes.write(value,0,value.length);return this;}
        Proto text(int field,String value){return data(field,value.getBytes(StandardCharsets.UTF_8));}
        Proto message(int field,Proto value){return data(field,value.get());}
        byte[] get(){return bytes.toByteArray();}
    }
    static int integer(String value){require(value.matches("0|[1-9][0-9]{0,9}"),"integer syntax");long n=Long.parseLong(value);require(n<=Integer.MAX_VALUE,"integer bound");return (int)n;}
    static String name(String value){require(value.matches("[A-Za-z0-9_.]{1,256}"),"graph name");return value;}
    static Proto attrInt(String name,long value){return new Proto().text(1,name).integer(20,2).integer(3,value);}
    static Proto attrInts(String name,int... values){Proto p=new Proto().text(1,name).integer(20,7);for(int v:values)p.integer(8,v);return p;}
    static Proto attrString(String name,String value){return new Proto().text(1,name).integer(20,3).text(4,value);}
    static Proto node(String op,String label,String[] inputs,String output,Proto... attrs){
        Proto p=new Proto();for(String input:inputs)p.text(1,input);p.text(2,output).text(3,label).text(4,op);
        for(Proto attr:attrs)p.message(5,attr);return p;
    }
    static Proto tensor(String name,int datatype,int[] dimensions,byte[] raw){
        Proto p=new Proto();for(int d:dimensions)p.integer(1,d);return p.integer(2,datatype).text(8,name).data(9,raw);
    }
    static Proto value(String name,int[] dimensions){
        Proto shape=new Proto();for(int d:dimensions)shape.message(1,new Proto().integer(1,d));
        Proto type=new Proto().message(1,new Proto().integer(1,1).message(2,shape));
        return new Proto().text(1,name).message(2,type);
    }
    private static final class Edge {
        final String[] inputs; final String output;
        Edge(String[] inputs,String output){this.inputs=inputs;this.output=output;}
    }
    static PinnedModel.CompiledModel compile(PinnedModel.Style style,PinnedModel.Decoded decoded){
        String text=decoded.graph;require(text.length()>0 && text.length()<=1000000 && text.indexOf('\0')<0 && text.indexOf('\\')<0,"graph text");
        List<String[]> rows=new ArrayList<>();for(String row:text.split("\\r?\\n"))if(!row.trim().isEmpty())rows.add(row.trim().split("\\s+"));
        require(rows.size()>=3 && rows.size()<=1026,"graph row bound");String[] header=rows.get(0),data=rows.get(1);
        int expected=style==PinnedModel.Style.NATURAL_BLUSH?97:96;
        require(header.length==3 && header[0].equals("1") && integer(header[1])==expected && rows.size()==expected+2,"graph header/count");
        require(header[2].equals(style==PinnedModel.Style.NATURAL_BLUSH?"1636678705":"1614766077"),"graph marker");
        require(Arrays.equals(data,new String[]{"DataV2","data","1","256","256","3","4","0","0"}),"graph input");
        byte[] weights=decoded.weights;require(weights.length==(style==PinnedModel.Style.NATURAL_BLUSH?239900:250004)*4,"weight count");
        ByteBuffer floats=ByteBuffer.wrap(weights).order(ByteOrder.LITTLE_ENDIAN);
        for(int i=0;i<weights.length;i+=4)require(Float.isFinite(floats.getFloat(i)),"nonfinite weight");
        Map<String,int[]> shapes=new HashMap<>();shapes.put("data",new int[]{1,3,256,256});Set<String> names=new HashSet<>();
        List<Edge> edges=new ArrayList<>();Proto graph=new Proto().text(2,"pinned-native-tensor-replay");int cursor=0;long aggregate=3*256*256;
        String last=null;
        for(int index=0;index<expected;index++){
            String[] row=rows.get(index+2);require(row.length>=2,"truncated operator");String kind=row[0],label=name(row[1]),prefix="@replay/"+index;
            require(names.add(label),"duplicate operator name");String[] inputs;String output;int[] params=null;
            switch(kind){
                case "Convolution": case "DepthwiseSeparableConvolution":
                    require(row.length==19,"convolution fields");params=new int[15];for(int j=0;j<15;j++)params[j]=integer(row[j+2]);
                    for(int j=0;j<5;j++)require(params[j]>0 && params[j]<=4096,"kernel/stride bound");
                    require(params[5]<=4096 && params[6]<=4096 && params[7]==1 && params[8]<=1,"padding/bias/activation");
                    require(Arrays.equals(Arrays.copyOfRange(params,9,15),new int[]{4,0,4,0,4,0}),"convolution datatypes");
                    inputs=new String[]{name(row[17])};output=name(row[18]);break;
                case "Concat":
                    require(row.length==8 && row[2].equals("2") && row[6].equals("4") && row[7].equals("0"),"concat fields");
                    inputs=new String[]{name(row[3]),name(row[4])};output=name(row[5]);break;
                case "Eltwise":
                    require(row.length==8 && row[5].equals("4") && row[6].equals("0") && (row[7].equals("0")||row[7].equals("1")),"elementwise fields");
                    inputs=new String[]{name(row[2]),name(row[3])};output=name(row[4]);params=new int[]{integer(row[7])};break;
                case "UpSampling":
                    require(row.length==5 && row[4].equals("BILINEAR"),"upsampling fields");inputs=new String[]{name(row[2])};output=name(row[3]);break;
                case "Tanh":
                    require(row.length==6 && row[4].equals("4") && row[5].equals("0"),"tanh fields");inputs=new String[]{name(row[2])};output=name(row[3]);break;
                default: throw new IllegalArgumentException("unsupported graph operator");
            }
            require(!shapes.containsKey(output),"duplicate tensor output");for(String input:inputs)require(shapes.containsKey(input),"nontopological graph");
            int[] input=shapes.get(inputs[0]),shape=input.clone();
            switch(kind){
                case "Convolution": case "DepthwiseSeparableConvolution": {
                    int o=params[0],kh=params[1],kw=params[2],sh=params[3],sw=params[4],ph=params[5],pw=params[6],channels=input[1];
                    boolean dw=kind.equals("DepthwiseSeparableConvolution");require(kh==kw && sh==sw && ph==pw && (!dw||o==channels),"unaudited convolution geometry");
                    long count=(long)kh*kw*channels*(dw?1:o);require(count<=Integer.MAX_VALUE && (count+o)*4<=weights.length-cursor,"weight stream bound");
                    byte[] reordered=new byte[(int)count*4];int target=0;
                    for(int oc=0;oc<o;oc++)for(int ic=0;ic<(dw?1:channels);ic++)for(int y=0;y<kh;y++)for(int x=0;x<kw;x++){
                        int rawIndex=dw?(y*kw+x)*channels+oc:((oc*kh+y)*kw+x)*channels+ic;
                        System.arraycopy(weights,cursor+rawIndex*4,reordered,target,4);target+=4;
                    }
                    String wname=prefix+"/weight",bname=prefix+"/bias";
                    graph.message(5,tensor(wname,1,new int[]{o,dw?1:channels,kh,kw},reordered));
                    cursor+=reordered.length;graph.message(5,tensor(bname,1,new int[]{o},PinnedModel.slice(weights,cursor,o*4)));cursor+=o*4;
                    shape=new int[]{input[0],o,(input[2]+2*ph-kh)/sh+1,(input[3]+2*pw-kw)/sw+1};
                    require(input[2]+2*ph>=kh && input[3]+2*pw>=kw,"convolution output geometry");
                    String dest=params[8]==1?prefix+"/pre_relu":output;
                    graph.message(1,node("Conv",prefix+"/conv",new String[]{inputs[0],wname,bname},dest,
                        attrInts("kernel_shape",kh,kw),attrInts("strides",sh,sw),attrInts("pads",ph,pw,ph,pw),attrInts("dilations",1,1),attrInt("group",dw?o:1),attrString("auto_pad","NOTSET")));
                    if(params[8]==1)graph.message(1,node("Relu",prefix+"/relu",new String[]{dest},output));break;
                }
                case "Concat": {
                    int[] other=shapes.get(inputs[1]);require(input[0]==other[0] && input[2]==other[2] && input[3]==other[3],"concat shape");shape[1]+=other[1];
                    graph.message(1,node("Concat",prefix,inputs,output,attrInt("axis",1)));break;
                }
                case "Eltwise": {
                    require(Arrays.equals(input,shapes.get(inputs[1])),"elementwise broadcast forbidden");String dest=params[0]==1?prefix+"/pre_relu":output;
                    graph.message(1,node("Add",prefix+"/add",inputs,dest));if(params[0]==1)graph.message(1,node("Relu",prefix+"/relu",new String[]{dest},output));break;
                }
                case "UpSampling": {
                    shape[2]*=2;shape[3]*=2;String sizes=prefix+".sizes";ByteBuffer raw=ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);for(int d:shape)raw.putLong(d);
                    graph.message(5,tensor(sizes,7,new int[]{4},raw.array()));
                    graph.message(1,node("Resize",prefix,new String[]{inputs[0],"","",sizes},output,attrString("mode","linear"),attrString("coordinate_transformation_mode","half_pixel"),attrInt("exclude_outside",0),attrInt("antialias",0)));break;
                }
                case "Tanh": graph.message(1,node("Tanh",prefix,inputs,output));break;
                default:throw new AssertionError();
            }
            long size=1;for(int d:shape){require(d>0 && d<=4096,"intermediate shape bound");size*=d;}require(size<=8000000,"intermediate tensor bound");aggregate+=size;require(aggregate<=64000000,"aggregate tensor bound");
            shapes.put(output,shape);edges.add(new Edge(inputs,output));last=output;
        }
        require(cursor==weights.length && Arrays.equals(shapes.get(last),new int[]{1,4,256,256}),"final model contract");
        Set<String> needed=new HashSet<>();needed.add(last);for(int i=edges.size()-1;i>=0;i--){Edge e=edges.get(i);require(needed.remove(e.output),"disconnected operator");needed.addAll(Arrays.asList(e.inputs));}
        require(needed.size()==1 && needed.contains("data"),"unresolved input");
        graph.message(11,value("data",shapes.get("data"))).message(12,value(last,shapes.get(last)));
        byte[] onnx=new Proto().integer(1,10).text(2,"local-pinned-android-model-replay").message(7,graph).message(8,new Proto().text(1,"").integer(2,18)).get();
        return new PinnedModel.CompiledModel(style,"data",last,expected,decoded,onnx);
    }
}
