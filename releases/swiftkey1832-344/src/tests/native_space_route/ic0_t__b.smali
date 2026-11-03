.method public final b(Llh0/a;)V
    .locals 10

    const-string v0, "bc"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    iget-object v0, p0, Lic0/t;->a:Lbp/f;

    iget-object v0, v0, Lbp/f;->a:Ljava/lang/Object;

    check-cast v0, Lp10/a;

    invoke-virtual {v0}, Lp10/a;->iterator()Ljava/util/Iterator;

    move-result-object v0

    const/4 v1, 0x0

    move v2, v1

    :goto_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v3

    if-eqz v3, :cond_1

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ls40/d;

    invoke-virtual {v3}, Ls40/d;->b()V

    iget v4, v3, Ls40/d;->d:I

    if-eqz v4, :cond_0

    const/4 v4, 0x1

    goto :goto_1

    :cond_0
    move v4, v1

    :goto_1
    iput v1, v3, Ls40/d;->d:I

    or-int/2addr v2, v4

    goto :goto_0

    :cond_1
    if-nez v2, :cond_2

    const/4 v9, 0x0

    const/4 v7, 0x0

    iget-object v3, p0, Lic0/t;->b:Ljava/lang/Object;

    const-string v5, " "

    const-string v6, " "

    const/4 v8, 0x0

    move-object v4, p1

    invoke-interface/range {v3 .. v9}, Lwb0/t0;->P(Llh0/a;Ljava/lang/String;Ljava/lang/String;Lec0/m;ZLjava/lang/Integer;)V

    :cond_2
    return-void
.end method
