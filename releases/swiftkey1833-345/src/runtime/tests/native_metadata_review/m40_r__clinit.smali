.method static constructor <clinit>()V
    .locals 2

    const-string v0, "\\d"

    invoke-static {v0}, Ljava/util/regex/Pattern;->compile(Ljava/lang/String;)Ljava/util/regex/Pattern;

    move-result-object v0

    const-string v1, "compile(...)"

    invoke-static {v0, v1}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    sput-object v0, Lm40/r;->a:Ljava/util/regex/Pattern;

    new-instance v0, Lm40/o;

    const/4 v1, 0x0

    invoke-direct {v0, v1}, Lm40/o;-><init>(I)V

    sput-object v0, Lm40/r;->b:Lm40/o;

    new-instance v0, Lm40/n;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    sput-object v0, Lm40/r;->c:Lm40/n;

    new-instance v0, Lm40/l;

    invoke-direct {v0, v1}, Lm40/l;-><init>(I)V

    sput-object v0, Lm40/r;->d:Lm40/l;

    new-instance v0, Lm40/m;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    sput-object v0, Lm40/r;->e:Lm40/m;

    new-instance v0, Lm40/p;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    sput-object v0, Lm40/r;->f:Lm40/p;

    new-instance v0, Lm40/f;

    const/4 v1, 0x1

    invoke-direct {v0, v1}, Lm40/f;-><init>(I)V

    sput-object v0, Lm40/r;->g:Lm40/f;

    new-instance v0, Lm40/g;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    sput-object v0, Lm40/r;->h:Lm40/g;

    new-instance v0, Lm40/f;

    const/4 v1, 0x0

    invoke-direct {v0, v1}, Lm40/f;-><init>(I)V

    sput-object v0, Lm40/r;->i:Lm40/f;

    new-instance v0, Lm40/e;

    invoke-direct {v0, v1}, Lm40/e;-><init>(I)V

    sput-object v0, Lm40/r;->j:Lm40/e;

    new-instance v0, Lm40/e;

    const/4 v1, 0x1

    invoke-direct {v0, v1}, Lm40/e;-><init>(I)V

    sput-object v0, Lm40/r;->k:Lm40/e;

    new-instance v0, Ll40/a;

    invoke-direct {v0, v1}, Ll40/a;-><init>(I)V

    sput-object v0, Lm40/r;->l:Ll40/a;

    new-instance v0, Lm40/i;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    sput-object v0, Lm40/r;->m:Lm40/i;

    new-instance v0, Lm40/d;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    sput-object v0, Lm40/r;->n:Lm40/d;

    new-instance v0, Lm40/j;

    const/4 v1, 0x0

    invoke-direct {v0, v1}, Lm40/j;-><init>(I)V

    sput-object v0, Lm40/r;->o:Lm40/j;

    new-instance v0, Lm40/q;

    invoke-direct {v0, v1}, Lm40/q;-><init>(I)V

    sput-object v0, Lm40/r;->p:Lm40/q;

    new-instance v0, Lm40/h;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    sput-object v0, Lm40/r;->q:Lm40/h;

    return-void
.end method
