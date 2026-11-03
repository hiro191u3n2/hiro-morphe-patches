.method public final varargs i(Lcm0/d;[Lic0/a;)V
    .locals 5

    const-string v0, "actors"

    invoke-static {p2, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    iget-object v0, p0, Lhc0/b;->d:Ljava/util/List;

    if-nez v0, :cond_0

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    :cond_0
    iput-object v0, p0, Lhc0/b;->d:Ljava/util/List;

    array-length v0, p2

    const/4 v1, 0x0

    :goto_0
    if-ge v1, v0, :cond_2

    aget-object v2, p2, v1

    iget-object v3, p0, Lhc0/b;->d:Ljava/util/List;

    if-eqz v3, :cond_1

    new-instance v4, Lhc0/o;

    invoke-static {v2}, Lic0/d;->a(Lic0/a;)Lic0/e;

    move-result-object v2

    invoke-direct {v4, p1, v2}, Lhc0/o;-><init>(Lcm0/d;Ljava/lang/Object;)V

    invoke-interface {v3, v4}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    :cond_1
    add-int/lit8 v1, v1, 0x1

    goto :goto_0

    :cond_2
    return-void
.end method
