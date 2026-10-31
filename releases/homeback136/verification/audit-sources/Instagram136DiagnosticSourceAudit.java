import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;

/** Independent diagnostic-only source inverse. No production helper is called. */
public final class Instagram136DiagnosticSourceAudit {
 static final String TRACE="InstagramBackTrace.";
 static final Set<String> CALLS=new HashSet<>(Arrays.asList("registration","begin","clear","entry","guards","origin","finish","nativeResult","dispatcher","layers","scene","safe","rejected"));
 static final Set<String> IDENTITIES=new HashSet<>(Arrays.asList("observeReject","observeAllow","observeParent","observeContainer","observeMismatch"));
 static final Set<String> FILES=new HashSet<>(Arrays.asList("InstagramExternalProfile.java","InstagramDispatcherBack.java","OneBackExit.java"));
 static int removed,unwrapped,compared;
 static final class Source extends SimpleJavaFileObject {
  final String text;Source(String text){super(URI.create("string:///Audit.java"),Kind.SOURCE);this.text=text;}
  public CharSequence getCharContent(boolean ignore){return text;}
 }
 static final class Parsed {
  final CompilationUnitTree unit;final SourcePositions positions;
  Parsed(String text)throws Exception{
   DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
   JavacTask task=(JavacTask)ToolProvider.getSystemJavaCompiler().getTask(null,null,diagnostics,Arrays.asList("-proc:none"),null,Arrays.asList(new Source(text)));
   unit=task.parse().iterator().next();positions=Trees.instance(task).getSourcePositions();
   for(Diagnostic<?> d:diagnostics.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR)throw new AssertionError("Invalid Java input: "+d);
  }
 }
 static String strip(String text)throws Exception{
  for(int pass=0;pass<8;pass++){
   Parsed p=new Parsed(text);final String source=text;List<Object[]> edits=new ArrayList<>();
   new TreeScanner<Void,Void>() {
    void edit(Tree tree,String value){edits.add(new Object[]{(int)p.positions.getStartPosition(p.unit,tree),(int)p.positions.getEndPosition(p.unit,tree),value});}
    public Void visitExpressionStatement(ExpressionStatementTree tree,Void unused){
     ExpressionTree expression=tree.getExpression();
     if(expression instanceof MethodInvocationTree&&((MethodInvocationTree)expression).getMethodSelect().toString().startsWith(TRACE)){
      MethodInvocationTree call=(MethodInvocationTree)expression;if(!CALLS.contains(call.getMethodSelect().toString().substring(TRACE.length())))throw new AssertionError("Unreviewed trace call");
      for(ExpressionTree argument:call.getArguments())passive(argument);edit(tree,"");removed++;return null;
     }
     return super.visitExpressionStatement(tree,unused);
    }
    public Void visitMethodInvocation(MethodInvocationTree tree,Void unused){
     String name=tree.getMethodSelect().toString();
     if(name.startsWith(TRACE+"observe")){
      if(!IDENTITIES.contains(name.substring(TRACE.length())))throw new AssertionError("Unreviewed identity observer");
      if(tree.getArguments().isEmpty())throw new AssertionError("Identity wrapper without original value");
      for(int i=0;i<tree.getArguments().size()-1;i++)passive(tree.getArguments().get(i));
      ExpressionTree original=tree.getArguments().get(tree.getArguments().size()-1);
      edit(tree,source.substring((int)p.positions.getStartPosition(p.unit,original),(int)p.positions.getEndPosition(p.unit,original)));unwrapped++;return null;
     }
     return super.visitMethodInvocation(tree,unused);
    }
   }.scan(p.unit,null);
   if(edits.isEmpty())return text;
   edits.sort((a,b)->Integer.compare((int)b[0],(int)a[0]));StringBuilder changed=new StringBuilder(text);int previous=text.length();
   for(Object[] edit:edits){int start=(int)edit[0],end=(int)edit[1];if(end>previous)throw new AssertionError("Overlapping diagnostic inverses");changed.replace(start,end,(String)edit[2]);previous=start;}text=changed.toString();
  }
  throw new AssertionError("Unexpected deeply nested instrumentation");
 }
 static void passive(Tree argument){new TreeScanner<Void,Void>(){
  public Void visitMethodInvocation(MethodInvocationTree call,Void unused){String name=call.getMethodSelect().toString();if(!name.equals("RETURNS.containsKey")&&!name.equals("InstagramBack.forwarding"))throw new AssertionError("Additional getter/callback in trace arguments: "+name);return super.visitMethodInvocation(call,unused);}
  public Void visitNewClass(NewClassTree tree,Void unused){throw new AssertionError("Allocation in trace arguments");}
  public Void visitNewArray(NewArrayTree tree,Void unused){throw new AssertionError("Allocation in trace arguments");}
  public Void visitAssignment(AssignmentTree tree,Void unused){throw new AssertionError("Assignment in trace arguments");}
  public Void visitCompoundAssignment(CompoundAssignmentTree tree,Void unused){throw new AssertionError("Assignment in trace arguments");}
  public Void visitUnary(UnaryTree tree,Void unused){if(Arrays.asList(Tree.Kind.PREFIX_INCREMENT,Tree.Kind.POSTFIX_INCREMENT,Tree.Kind.PREFIX_DECREMENT,Tree.Kind.POSTFIX_DECREMENT).contains(tree.getKind()))throw new AssertionError("Mutation in trace arguments");return super.visitUnary(tree,unused);}
 }.scan(argument,null);}
 static String canonical(String text)throws Exception{
  // Normalize only AST-confirmed single return/throw blocks used by an if.
  // Do not normalize literal contents, method/catch bodies or lexical scopes.
  for(int pass=0;pass<8;pass++){
   Parsed p=new Parsed(text);final String source=text;List<Object[]> edits=new ArrayList<>();
   new TreePathScanner<Void,Void>(){
    public Void visitBlock(BlockTree block,Void unused){
     Tree parent=getCurrentPath().getParentPath()==null?null:getCurrentPath().getParentPath().getLeaf();
     if(parent instanceof IfTree&&block.getStatements().size()==1){StatementTree statement=block.getStatements().get(0);if(statement.getKind()==Tree.Kind.RETURN||statement.getKind()==Tree.Kind.THROW||statement.getKind()==Tree.Kind.EXPRESSION_STATEMENT){
      edits.add(new Object[]{(int)p.positions.getStartPosition(p.unit,block),(int)p.positions.getEndPosition(p.unit,block),source.substring((int)p.positions.getStartPosition(p.unit,statement),(int)p.positions.getEndPosition(p.unit,statement))});return null;
     }}return super.visitBlock(block,unused);
    }
   }.scan(p.unit,null);
   if(edits.isEmpty())return p.unit.toString();
   edits.sort((a,b)->Integer.compare((int)b[0],(int)a[0]));StringBuilder changed=new StringBuilder(text);
   for(Object[] edit:edits)changed.replace((int)edit[0],(int)edit[1],(String)edit[2]);text=changed.toString();
  }throw new AssertionError("Unexpected normalization depth");
 }
 public static void main(String[] args)throws Exception{
  if(args.length!=2)throw new IllegalArgumentException("BASELINE_RUNTIME CANDIDATE_RUNTIME");
  Path old=Paths.get(args[0]),now=Paths.get(args[1]);int beforeRemoved=removed,beforeUnwrapped=unwrapped;
  try(java.util.stream.Stream<Path> files=Files.list(old)){
   for(Path file:(Iterable<Path>)files.filter(p->p.toString().endsWith(".java"))::iterator){
    Path candidate=now.resolve(file.getFileName());if(!Files.isRegularFile(candidate))throw new AssertionError("Removed baseline file "+file);
    String candidateText=Files.readString(candidate);if(candidateText.contains("InstagramBackTrace.")&&!FILES.contains(file.getFileName().toString()))throw new AssertionError("Unexpected diagnostic insertion file "+file);
    String a=canonical(Files.readString(file)),b=canonical(strip(candidateText));
    if(!a.equals(b)){int pos=0;while(pos<Math.min(a.length(),b.length())&&a.charAt(pos)==b.charAt(pos))pos++;throw new AssertionError("Non-diagnostic AST delta in "+file.getFileName()+" at "+pos+"\nBEFORE: "+a.substring(Math.max(0,pos-100),Math.min(a.length(),pos+350))+"\nAFTER: "+b.substring(Math.max(0,pos-100),Math.min(b.length(),pos+350)));}compared++;
   }
  }
  try(java.util.stream.Stream<Path> files=Files.list(now)){for(Path p:(Iterable<Path>)files.filter(p->p.toString().endsWith(".java"))::iterator)if(!Files.exists(old.resolve(p.getFileName()))&&!p.getFileName().toString().equals("InstagramBackTrace.java"))throw new AssertionError("Unexpected new runtime file "+p);}
  if(compared!=28||removed-beforeRemoved!=33||unwrapped-beforeUnwrapped!=15)throw new AssertionError("Unreviewed diagnostic source inventory/counts");
  System.out.println("PASS independent diagnostic-only source AST inverse: baselineFiles="+compared+", standaloneTraceCalls="+(removed-beforeRemoved)+", identityWrappers="+(unwrapped-beforeUnwrapped)+"; all original executable source decisions retained; new trace helper reviewed separately");
 }
}
