.method public final n(Lfc0/m1;Lfc0/v1;)Lfc0/n2;
    .locals 11

    iget-object v0, p0, Lfc0/s1;->t:Lfb0/y1;

    if-eqz v0, :cond_5

    new-instance v4, Lfc0/g2;

    iget-object v1, p0, Lfc0/s1;->d:Ljava/lang/Object;

    invoke-direct {v4, v1}, Lfc0/g2;-><init>(Lwb0/t0;)V

    iget-object v0, v0, Lfb0/y1;->c:Lfb0/z1;

    new-instance v7, Lxc0/e;

    iget-object v1, p0, Lfc0/s1;->t:Lfb0/y1;

    iget-object v8, p0, Lfc0/s1;->a:Lcom/touchtype/KeyboardService;

    invoke-direct {v7, v8, v1}, Lxc0/e;-><init>(Lcom/touchtype/KeyboardService;Lfb0/y1;)V

    iget-object v1, p2, Lfc0/v1;->g:Ljava/util/ArrayList;

    invoke-interface {v1}, Ljava/lang/Iterable;->iterator()Ljava/util/Iterator;

    move-result-object v1

    const/4 v9, 0x0

    move v6, v9

    move v10, v6

    :cond_0
    :goto_0
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_2

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/String;

    const-string v3, "useSpacebarSymbol=true"

    invoke-virtual {v3, v2}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result v3

    const/4 v5, 0x1

    if-eqz v3, :cond_1

    move v10, v5

    goto :goto_0

    :cond_1
    const-string v3, "spaceShouldResetCyclesWithoutSpacing=true"

    invoke-virtual {v3, v2}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_0

    move v6, v5

    goto :goto_0

    :cond_2
    new-instance v2, Lhc0/b;

    invoke-direct {v2}, Lhc0/b;-><init>()V

    iget-object v1, p0, Lfc0/s1;->k:Lfc0/k1;

    iget-object v3, v1, Lfc0/k1;->h:Lfz/h;

    invoke-virtual {v3}, Lfz/h;->b()Z

    move-result v3

    move v5, v3

    sget-object v3, Lfc0/l1;->a:Lf1/m2;

    if-eqz v5, :cond_3

    move-object v5, p1

    invoke-virtual/range {v1 .. v6}, Lfc0/k1;->e(Lhc0/b;Lcm0/d;Lfc0/g2;Lfc0/m1;Z)V

    goto :goto_1

    :cond_3
    move-object v5, v7

    move v7, v6

    move-object v6, v5

    move-object v5, p1

    invoke-virtual/range {v1 .. v7}, Lfc0/k1;->i(Lhc0/b;Lcm0/d;Lfc0/g2;Lfc0/m1;Lxc0/h;Z)V

    :goto_1
    invoke-virtual {v2, v4}, Lhc0/b;->e(Lfc0/d2;)Lhc0/a;

    move-result-object v6

    sget-object p1, Ldd0/r;->l:Ldd0/r;

    iget-object p2, p2, Lfc0/v1;->f:Ldd0/r;

    if-nez p2, :cond_4

    goto :goto_2

    :cond_4
    move-object p1, p2

    :goto_2
    invoke-virtual {p0, v0, v10}, Lfc0/s1;->m(Lfb0/z1;Z)Lmc0/h;

    move-result-object p2

    new-instance v7, Lgc0/l;

    invoke-virtual {v8}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object v0

    const-string v1, "getResources(...)"

    invoke-static {v0, v1}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    const v1, 0x7f140a31

    new-array v2, v9, [I

    invoke-direct {v7, v0, v1, v2}, Lgc0/l;-><init>(Landroid/content/res/Resources;I[I)V

    move-object v1, p0

    move-object v3, v4

    move-object v2, v5

    move-object v4, p1

    move-object v5, p2

    invoke-virtual/range {v1 .. v7}, Lfc0/s1;->B(Lfc0/m1;Lfc0/g2;Ldd0/r;Lmc0/h;Lhc0/a;Lgc0/c;)Lfc0/n2;

    move-result-object p1

    return-object p1

    :cond_5
    move-object v1, p0

    move-object v5, p1

    sget-object p1, Lfc0/v1;->w:Ljava/lang/Object;

    invoke-static {}, Lfc0/l1;->c()Lfc0/v1;

    move-result-object p1

    invoke-virtual {p0, v5, p1}, Lfc0/s1;->C(Lfc0/m1;Lfc0/v1;)Lfc0/n2;

    move-result-object p1

    return-object p1
.end method
