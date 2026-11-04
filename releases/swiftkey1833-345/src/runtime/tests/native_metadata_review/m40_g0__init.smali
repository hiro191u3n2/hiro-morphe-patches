.method public constructor <init>(Lm40/b;Loz/u6;)V
    .locals 2

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lm40/g0;->a:Lm40/b;

    new-instance v0, Lah0/x0;

    invoke-interface {p1}, Lm40/b;->sourceMetadata()Ln40/a;

    move-result-object p1

    const/16 v1, 0xd

    invoke-direct {v0, v1, p1, p2}, Lah0/x0;-><init>(ILjava/lang/Object;Ljava/lang/Object;)V

    iput-object v0, p0, Lm40/g0;->b:Lah0/x0;

    return-void
.end method
