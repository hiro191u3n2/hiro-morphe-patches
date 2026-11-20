package app.hiro.newxui.patches;

import java.io.File;
import java.util.*;
import app.morphe.patcher.patch.*;

/** Real Piko dependency graphs with controlled invalid metadata cases. */
public final class UiSelectionAudit {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void rejected(Runnable check,String reason){
        try{check.run();throw new AssertionError("Accepted invalid selection: "+reason);}
        catch(IllegalStateException expected){require(expected.getMessage().contains(reason),"Unexpected rejection: "+expected.getMessage());}
    }
    static void visit(Patch patch,Set<Patch> seen){if(seen.add(patch))for(Patch dependency:patch.getDependencies())visit(dependency,seen);}
    static void withBundle(Collection<Patch> patches,PatchBundleInfo bundle,Runnable check){
        Map<Patch,PatchBundleInfo> originals=new IdentityHashMap<Patch,PatchBundleInfo>();
        try{for(Patch patch:patches){originals.put(patch,patch.getBundle$morphe_patcher());patch.setBundle$morphe_patcher(bundle);}check.run();}
        finally{for(Map.Entry<Patch,PatchBundleInfo> entry:originals.entrySet())entry.getKey().setBundle$morphe_patcher(entry.getValue());}
    }
    public static void main(String[] args)throws Exception{
        for(String path:args){
            PatchLoader.Jar loader=PatchKt.loadPatchesFromJar(Collections.singleton(new File(path)));
            List<Patch> selected=new ArrayList<Patch>();
            for(Patch patch:loader)if(patch.getName()!=null&&patch.getName().startsWith("NewX:")&&patch.getDefault())selected.add(patch);
            require(!selected.isEmpty(),"Missing real default NewX patches: "+path);
            UiOptionsPatch.validateSelection(selected);
            Set<Patch> graph=Collections.newSetFromMap(new IdentityHashMap<Patch,Boolean>());
            for(Patch patch:selected)visit(patch,graph);
            Patch settings=null;
            for(Patch patch:graph)if("SettingsPatch.newXSettingsPatch".equals(patch.getDeclaredAs$morphe_patcher()))settings=patch;
            require(settings!=null,"Missing real settings dependency");
            PatchBundleInfo original=settings.getBundle$morphe_patcher();
            for (String version : Arrays.asList("3.12.0-dev.99", "3.13.0", "4.0.0-beta.1")) {
                withBundle(graph, original.copy(original.getFile(), "Piko", version, original.getPatcherVersion(),
                    original.getRetryingFiltersCanChangeMatches()), () -> UiOptionsPatch.validateSelection(selected));
            }
            withBundle(graph,original.copy(original.getFile(),"Other source",original.getVersion(),original.getPatcherVersion(),original.getRetryingFiltersCanChangeMatches()),
                ()->rejected(()->UiOptionsPatch.validateSelection(selected),"NewX設定の提供元"));
            List<Patch> singleton=Collections.singletonList(settings);
            withBundle(singleton,null,()->rejected(()->UiOptionsPatch.validateSelection(selected),"NewX設定の提供元"));
            withBundle(singleton,original.copy(new File(original.getFile().getParentFile(),"different-piko-source.mpp"),original.getName(),original.getVersion(),original.getPatcherVersion(),original.getRetryingFiltersCanChangeMatches()),
                ()->rejected(()->UiOptionsPatch.validateSelection(selected),"複数のPikoソース"));
            UiOptionsPatch.validateSelection(selected);
        }
        rejected(()->UiOptionsPatch.validateSelection(Collections.emptyList()),"NewX項目と一緒に選択");
        rejected(()->UiOptionsPatch.validateSelection(Collections.singletonList(UiOptionsPatch.PATCH)),"NewX項目と一緒に選択");
        System.out.println("PASS UI selection: "+args.length+" official Piko graphs accepted; future version labels accepted; wrong-name/missing-metadata/mixed-source/no-Piko/UI-only rejected");
    }
}
