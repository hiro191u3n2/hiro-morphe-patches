.method public constructor <init>(Lcom/microsoft/fluency/Prediction;Lm40/f0;Ll40/c;)V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const/4 v0, 0x0

    iput-boolean v0, p0, Lm40/c0;->i:Z

    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    iput-object p1, p0, Lm40/c0;->a:Lcom/microsoft/fluency/Prediction;

    iput-object p2, p0, Lm40/c0;->b:Lm40/f0;

    iput-object p3, p0, Lm40/c0;->c:Ll40/c;

    new-instance p2, Lcb0/p;

    invoke-direct {p2, p1}, Lcb0/p;-><init>(Ljava/lang/Object;)V

    iput-object p2, p0, Lm40/c0;->d:Lcb0/p;

    return-void
.end method
