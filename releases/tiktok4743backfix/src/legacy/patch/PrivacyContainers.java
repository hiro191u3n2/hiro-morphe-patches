/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
package app.hiro.tiktok4753.patches;

import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.value.ImmutableStringEncodedValue;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction21c;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction31c;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.CRC32;

/** Some libdex_df_*.so entries are ZIP/DEX containers, not native ELF files.
 * DEX is rebuilt with a sorted string table, signature and checksum; ZIP32 local
 * and central records retain names, order, flags, extras, comments, attributes,
 * STORED compression and original 4-byte payload alignment. No raw ZIP edits.
 */
public final class PrivacyContainers {
    private PrivacyContainers() {}
    public static final class Result {
        public final byte[] bytes;
        public final String format;
        public final int hosts, dexConstants, dexFields, dexEntries;
        Result(byte[] bytes, String format, int hosts, int constants, int fields, int dexEntries) {
            this.bytes=bytes;this.format=format;this.hosts=hosts;this.dexConstants=constants;this.dexFields=fields;this.dexEntries=dexEntries;
        }
        public int changes() { return hosts+dexConstants+dexFields; }
    }
    private static void require(boolean value,String message) { if(!value) throw new IllegalStateException("TikTok privacy container: "+message); }
    private static boolean starts(byte[] bytes, int... prefix) {
        if(bytes.length<prefix.length)return false;for(int i=0;i<prefix.length;i++)if((bytes[i]&255)!=prefix[i])return false;return true;
    }
    public static Result rewrite(byte[] original) throws IOException {
        if(starts(original,0x7f,0x45,0x4c,0x46)) {
            byte[] bytes=original.clone();int count=LegacyPrivacy.rewriteNativeBytes(bytes);return new Result(bytes,"ELF",count,0,0,0);
        }
        if(starts(original,0x50,0x4b,3,4)) return zip(original);
        if(starts(original,0x64,0x65,0x78,0x0a))return dex(original);
        byte[] probe=original.clone();require(LegacyPrivacy.rewriteNativeBytes(probe)==0,"tracking endpoints in an unsupported binary format");
        return new Result(original,"OTHER_UNCHANGED",0,0,0,0);
    }
    private static boolean stringInstruction(Instruction instruction) {
        return (instruction.getOpcode()==Opcode.CONST_STRING||instruction.getOpcode()==Opcode.CONST_STRING_JUMBO)&&instruction instanceof ReferenceInstruction&&
            ((ReferenceInstruction)instruction).getReference() instanceof StringReference;
    }
    private static String value(Instruction instruction) { return ((StringReference)((ReferenceInstruction)instruction).getReference()).getString(); }
    private static int changedConstants(Method method) {
        if(method.getImplementation()==null)return 0;int count=0;
        for(Instruction insn:method.getImplementation().getInstructions())if(stringInstruction(insn)&&!value(insn).equals(LegacyPrivacy.rewriteTrackingHosts(value(insn))))count++;
        return count;
    }
    private static Result dex(byte[] original) throws IOException {
        DexBackedDexFile input=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),new ByteArrayInputStream(original));
        List<ClassDef> classes=new ArrayList<ClassDef>();int constants=0,fields=0;
        for(ClassDef clazz:input.getClasses()) {
            List<Field> newFields=new ArrayList<Field>();List<Method> newMethods=new ArrayList<Method>();boolean changed=false;
            for(Field field:clazz.getFields()){
                Field replacement=field;
                if((field.getAccessFlags()&8)!=0&&field.getInitialValue() instanceof StringEncodedValue){
                    String value=((StringEncodedValue)field.getInitialValue()).getValue(),rewritten=LegacyPrivacy.rewriteTrackingHosts(value);
                    if(!value.equals(rewritten)){
                        replacement=new ImmutableField(field.getDefiningClass(),field.getName(),field.getType(),field.getAccessFlags(),
                            new ImmutableStringEncodedValue(rewritten),field.getAnnotations(),field.getHiddenApiRestrictions());fields++;changed=true;
                    }
                }
                newFields.add(replacement);
            }
            for(Method method:clazz.getMethods()){
                if(changedConstants(method)==0){newMethods.add(method);continue;}
                List<Instruction> instructions=new ArrayList<Instruction>();
                for(Instruction insn:method.getImplementation().getInstructions()){
                    Instruction replacement=insn;
                    if(stringInstruction(insn)){
                        String rewritten=LegacyPrivacy.rewriteTrackingHosts(value(insn));
                        if(!rewritten.equals(value(insn))){
                            int reg=((OneRegisterInstruction)insn).getRegisterA();ImmutableStringReference ref=new ImmutableStringReference(rewritten);
                            replacement=insn.getOpcode()==Opcode.CONST_STRING_JUMBO?new ImmutableInstruction31c(Opcode.CONST_STRING_JUMBO,reg,ref):new ImmutableInstruction21c(Opcode.CONST_STRING,reg,ref);
                            constants++;changed=true;
                        }
                    }
                    instructions.add(replacement);
                }
                // No mutable instruction builder: original obfuscator debug records may
                // address interior code units. Same-width replacements preserve them.
                MethodImplementation implementation=method.getImplementation();
                newMethods.add(new ImmutableMethod(method.getDefiningClass(),method.getName(),method.getParameters(),method.getReturnType(),method.getAccessFlags(),
                    method.getAnnotations(),method.getHiddenApiRestrictions(),new ImmutableMethodImplementation(implementation.getRegisterCount(),instructions,
                        implementation.getTryBlocks(),implementation.getDebugItems())));
            }
            classes.add(changed?new ImmutableClassDef(clazz.getType(),clazz.getAccessFlags(),clazz.getSuperclass(),clazz.getInterfaces(),clazz.getSourceFile(),
                clazz.getAnnotations(),newFields,newMethods):clazz);
        }
        if(constants+fields==0)return new Result(original,"DEX",0,0,0,1);
        MemoryDataStore store=new MemoryDataStore(original.length);
        try {
            DexPool.writeTo(store,new ImmutableDexFile(input.getOpcodes(),classes));
            byte[] output=store.getData();require((output.length&3)==0,"rebuilt DEX length is not aligned");
            return new Result(output,"DEX",0,constants,fields,1);
        } finally { store.close(); }
    }
    private static int u16(byte[] bytes,int at){require(at>=0&&at+2<=bytes.length,"truncated ZIP16");return (bytes[at]&255)|((bytes[at+1]&255)<<8);}
    private static int u32(byte[] bytes,int at){require(at>=0&&at+4<=bytes.length,"truncated ZIP32");return u16(bytes,at)|(u16(bytes,at+2)<<16);}
    private static void put32(byte[] bytes,int at,int value){for(int i=0;i<4;i++)bytes[at+i]=(byte)(value>>>(i*8));}
    private static final class Entry {
        String name;int central,centralSize,local,data,compressed,uncompressed,crc,method,flags,newLocal;byte[] replacement;
        int replacementCrc(){CRC32 c=new CRC32();c.update(replacement);return (int)c.getValue();}
    }
    private static Result zip(byte[] original) throws IOException {
        int end=-1;
        for(int at=original.length-22;at>=Math.max(0,original.length-65557);at--)
            if(u32(original,at)==0x06054b50&&at+22+u16(original,at+20)==original.length){end=at;break;}
        require(end>=0,"ZIP end record missing");
        require(u16(original,end+4)==0&&u16(original,end+6)==0,"multi-disk ZIP not supported");
        int count=u16(original,end+10),centralSize=u32(original,end+12),central=u32(original,end+16);
        require(count==u16(original,end+8)&&count<65535&&central>=0&&centralSize>=0&&central+centralSize==end,"invalid ZIP32 central directory");
        List<Entry> entries=new ArrayList<Entry>();int cursor=central,constants=0,fields=0,dexCount=0;
        Set<String> names=new HashSet<String>();
        for(int i=0;i<count;i++) {
            require(u32(original,cursor)==0x02014b50,"invalid ZIP central header");Entry e=new Entry();e.central=cursor;
            e.flags=u16(original,cursor+8);e.method=u16(original,cursor+10);e.crc=u32(original,cursor+16);
            e.compressed=u32(original,cursor+20);e.uncompressed=u32(original,cursor+24);e.local=u32(original,cursor+42);
            int nameLength=u16(original,cursor+28),extraLength=u16(original,cursor+30),commentLength=u16(original,cursor+32);
            e.centralSize=46+nameLength+extraLength+commentLength;
            require(cursor+e.centralSize<=end&&e.local>=0&&e.compressed>=0&&e.uncompressed>=0,"invalid ZIP entry bounds");
            e.name=new String(original,cursor+46,nameLength,StandardCharsets.UTF_8);require(names.add(e.name),"duplicate ZIP name "+e.name);
            require(u32(original,e.local)==0x04034b50,"invalid ZIP local header");
            e.data=e.local+30+u16(original,e.local+26)+u16(original,e.local+28);
            require(e.data>=e.local&&e.data+e.compressed<=central,"invalid ZIP payload bounds");
            if(e.name.matches("classes(?:[0-9]+)?\\.dex")) {
                require(e.method==0&&e.compressed==e.uncompressed&&(e.flags&9)==0,"embedded DEX must be unencrypted STORED without data descriptor");
                require((e.data&3)==0,"original embedded DEX is not four-byte aligned");
                require(u16(original,e.local+6)==e.flags&&u16(original,e.local+8)==e.method&&u32(original,e.local+14)==e.crc&&
                    u32(original,e.local+18)==e.compressed&&u32(original,e.local+22)==e.uncompressed,"ZIP local/central metadata mismatch");
                byte[] input=Arrays.copyOfRange(original,e.data,e.data+e.compressed);CRC32 crc=new CRC32();crc.update(input);
                require((int)crc.getValue()==e.crc,"original embedded DEX ZIP CRC mismatch");
                Result result=dex(input);dexCount++;constants+=result.dexConstants;fields+=result.dexFields;
                if(result.changes()>0)e.replacement=result.bytes;
            }
            entries.add(e);cursor+=e.centralSize;
        }
        if(constants+fields==0)return new Result(original,"ZIP_DEX",0,0,0,dexCount);
        List<Entry> ordered=new ArrayList<Entry>(entries);Collections.sort(ordered,new Comparator<Entry>(){public int compare(Entry a,Entry b){return Integer.compare(a.local,b.local);}});
        ByteArrayOutputStream out=new ByteArrayOutputStream(original.length);cursor=0;
        for(Entry e:ordered) {
            require(e.local>=cursor,"overlapping ZIP local records");out.write(original,cursor,e.local-cursor);e.newLocal=out.size();
            byte[] header=Arrays.copyOfRange(original,e.local,e.data);
            if(e.replacement!=null){put32(header,14,e.replacementCrc());put32(header,18,e.replacement.length);put32(header,22,e.replacement.length);}
            out.write(header);
            if(e.name.matches("classes(?:[0-9]+)?\\.dex"))require((out.size()&3)==0,"rebuilt embedded DEX alignment changed");
            if(e.replacement!=null)out.write(e.replacement);else out.write(original,e.data,e.compressed);
            cursor=e.data+e.compressed;
        }
        out.write(original,cursor,central-cursor);int newCentral=out.size();cursor=central;
        for(Entry e:entries) {
            byte[] record=Arrays.copyOfRange(original,e.central,e.central+e.centralSize);put32(record,42,e.newLocal);
            if(e.replacement!=null){put32(record,16,e.replacementCrc());put32(record,20,e.replacement.length);put32(record,24,e.replacement.length);}
            out.write(record);cursor=e.central+e.centralSize;
        }
        out.write(original,cursor,end-cursor);int newCentralSize=out.size()-newCentral;
        byte[] eocd=Arrays.copyOfRange(original,end,original.length);put32(eocd,12,newCentralSize);put32(eocd,16,newCentral);out.write(eocd);
        return new Result(out.toByteArray(),"ZIP_DEX",0,constants,fields,dexCount);
    }
}
