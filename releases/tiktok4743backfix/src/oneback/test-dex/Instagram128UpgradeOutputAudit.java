import java.util.*;
import app.hiro.oneback.patches.Instagram452Upgrade;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Independent emitted-APK audit for runtime-only127 ->128. No native method,
 * hook or app-class metadata delta is authorized. Uses the existing semantic
 * comparator's documented DEX encoding/debug normalization only. */
public final class Instagram128UpgradeOutputAudit {
    static void check(boolean ok,String why){RealUpgradeAudit.check(ok,why);}
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("PUBLISHED127_APK UPGRADED_APK FINAL128_MPP");
        Map<String,ClassDef> before=RealUpgradeAudit.read(args[0]),after=RealUpgradeAudit.read(args[1]),runtime=RealUpgradeAudit.runtime(args[2]);
        Instagram452Upgrade.verify(before);Instagram452Upgrade.verify(after);
        int classes=0,methods=0,rtClasses=0;
        for(ClassDef old:before.values()) {
            if(old.getType().startsWith(RealUpgradeAudit.PREFIX))continue;
            ClassDef fresh=after.get(old.getType());check(fresh!=null,"application class retained "+old.getType());
            RealUpgradeAudit.classMeta(old,fresh);
            Map<String,Method> om=RealUpgradeAudit.methods(old),nm=RealUpgradeAudit.methods(fresh);
            check(om.keySet().equals(nm.keySet()),"exact application method inventory "+old.getType());
            for(String id:om.keySet()){RealUpgradeAudit.compare(id,om.get(id),nm.get(id));methods++;}
            if(++classes%10000==0)System.out.println("Compared application classes="+classes+", methods="+methods);
        }
        for(ClassDef exact:runtime.values()) {
            ClassDef fresh=after.get(exact.getType());check(fresh!=null,"runtime class retained "+exact.getType());
            RealUpgradeAudit.classMeta(exact,fresh);
            Map<String,Method> em=RealUpgradeAudit.methods(exact),nm=RealUpgradeAudit.methods(fresh);
            check(em.keySet().equals(nm.keySet()),"exact emitted runtime method inventory "+exact.getType());
            for(String id:em.keySet())RealUpgradeAudit.compare(id,em.get(id),nm.get(id));rtClasses++;
        }
        for(ClassDef c:after.values()) {
            check(before.containsKey(c.getType())||runtime.containsKey(c.getType()),"no unexpected class "+c.getType());
            if(c.getType().startsWith(RealUpgradeAudit.PREFIX))check(runtime.containsKey(c.getType()),"no stale runtime "+c.getType());
            for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())
                if(i instanceof ReferenceInstruction){Reference r=((ReferenceInstruction)i).getReference();
                    String owner=r instanceof MethodReference?((MethodReference)r).getDefiningClass():r instanceof FieldReference?((FieldReference)r).getDefiningClass():"";
                    if(owner.startsWith(RealUpgradeAudit.PREFIX))check(RealUpgradeAudit.resolves(runtime,r),"runtime reference resolves "+r);
                }
        }
        System.out.println("PASS Instagram128UpgradeOutputAudit: applicationClasses="+classes+", unchangedApplicationMethods="+methods+
            ", exactRuntimeClasses="+rtClasses+", nativeBoundariesPreserved=7, assertions="+RealUpgradeAudit.assertions+
            ", constStringWidthChanges="+RealUpgradeAudit.jumboDeltas+", gotoWidthChanges="+RealUpgradeAudit.gotoDeltas+
            ", nopPaddingCountChanges="+RealUpgradeAudit.nopDeltas+", redundantExceptionHandlerChanges="+RealUpgradeAudit.redundantHandlerDeltas);
        System.out.println("All non-runtime code, native gates and metadata retained. Runtime matches finalMPP. Encoding widths, padding, unselectable duplicate exception handlers and debug positions use the existing documented normalization. Resources verified separately; no physical Android execution.");
    }
}
