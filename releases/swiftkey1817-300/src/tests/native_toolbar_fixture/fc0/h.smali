.class public final synthetic Lfc0/h;
.super Ljava/lang/Object;

.method public final invoke(Ljava/lang/Object;)Ljava/lang/Object;
    .locals 13

    iget v0, p0, Lfc0/h;->a:I

    sget-object v1, Lnl0/f0;->a:Lnl0/f0;

    sget-object v2, Lfc0/l1;->a:Lf1/m2;

    const-string v3, "$this$buildAction"

    sget-object v4, Loz/i6;->b:Loz/i6;

    iget-object v5, p0, Lfc0/h;->d:Lfc0/m1;

    iget-object v6, p0, Lfc0/h;->c:Lfc0/g2;

    iget-object v7, p0, Lfc0/h;->b:Lfc0/k1;

    const/4 v8, 0x2

    const/4 v9, 0x1

    const/4 v10, 0x0

    check-cast p1, Lhc0/b;

    packed-switch v0, :pswitch_data_0

    invoke-static {p1, v3}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    new-instance v0, Lic0/g;

    invoke-direct {v0, v6, v10}, Lic0/g;-><init>(Lfc0/d2;I)V

    iget-object v3, v7, Lfc0/k1;->p:Lwa0/u;

    new-array v11, v9, [Lic0/a;

    aput-object v0, v11, v10

    invoke-virtual {p1, v2, v11}, Lhc0/b;->l(Lcm0/d;[Lic0/a;)V

    iget-object v0, v7, Lfc0/k1;->a:Lcom/touchtype/KeyboardService;

    const v11, 0x7f140b8f

    invoke-virtual {v0, v11}, Landroid/content/Context;->getString(I)Ljava/lang/String;

    move-result-object v0

    const-string v11, "getString(...)"

    invoke-static {v0, v11}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    new-instance v11, Lic0/g;

    invoke-direct {v11, v6, v10}, Lic0/g;-><init>(Lfc0/d2;I)V

    new-instance v6, Lfc0/v0;

    invoke-direct {v6, v7, v10}, Lfc0/v0;-><init>(Ljava/lang/Object;I)V

    new-array v12, v8, [Lic0/a;

    aput-object v11, v12, v10

    aput-object v6, v12, v9

    invoke-virtual {v7, p1, v2, v0, v12}, Lfc0/k1;->a(Lhc0/b;Lcm0/d;Ljava/lang/String;[Lic0/a;)V

    invoke-virtual {v7, p1}, Lfc0/k1;->j(Lhc0/b;)V

    iget-object v0, v7, Lfc0/k1;->h:Lfz/h;

    invoke-virtual {v0}, Lfz/h;->b()Z

    move-result v6

    if-eqz v6, :cond_0

    new-instance v0, Lfc0/y;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    new-instance v5, Lic0/a0;

    invoke-direct {v5, v4, v3}, Lic0/a0;-><init>(Loz/i6;Lwa0/u;)V

    new-array v3, v8, [Lic0/a;

    aput-object v0, v3, v10

    aput-object v5, v3, v9

    invoke-virtual {p1, v2, v3}, Lhc0/b;->f(Lcm0/d;[Lic0/a;)V

    goto :goto_0

    :cond_0
    new-instance v6, Lfc0/y;

    invoke-direct {v6}, Ljava/lang/Object;-><init>()V

    new-instance v11, Lic0/a0;

    invoke-direct {v11, v4, v3}, Lic0/a0;-><init>(Loz/i6;Lwa0/u;)V

    new-array v8, v8, [Lic0/a;

    aput-object v6, v8, v10

    aput-object v11, v8, v9

    invoke-virtual {p1, v2, v8}, Lhc0/b;->i(Lcm0/d;[Lic0/a;)V

    invoke-virtual {v7, p1, v2, v5, v10}, Lfc0/k1;->f(Lhc0/b;Lcm0/d;Lfc0/m1;Z)V

    invoke-virtual {v0}, Lfz/h;->b()Z

    move-result v0

    if-eqz v0, :cond_1

    new-instance v0, Lic0/a0;

    invoke-direct {v0, v4, v3}, Lic0/a0;-><init>(Loz/i6;Lwa0/u;)V

    new-array v3, v9, [Lic0/a;

    aput-object v0, v3, v10

    invoke-virtual {p1, v2, v3}, Lhc0/b;->y(Lcm0/d;[Lic0/a;)V

    :cond_1
    :goto_0
    return-object v1

    :pswitch_0
    invoke-static {p1, v3}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-virtual {v7}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    new-instance v0, Lic0/g;

    invoke-direct {v0, v6, v10}, Lic0/g;-><init>(Lfc0/d2;I)V

    iget-object v3, v7, Lfc0/k1;->p:Lwa0/u;

    new-array v6, v9, [Lic0/a;

    aput-object v0, v6, v10

    invoke-virtual {p1, v2, v6}, Lhc0/b;->l(Lcm0/d;[Lic0/a;)V

    invoke-virtual {v7, p1}, Lfc0/k1;->j(Lhc0/b;)V

    iget-object v0, v7, Lfc0/k1;->h:Lfz/h;

    invoke-virtual {v0}, Lfz/h;->b()Z

    move-result v6

    if-eqz v6, :cond_2

    new-instance v0, Lfc0/y;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    new-instance v5, Lic0/a0;

    invoke-direct {v5, v4, v3}, Lic0/a0;-><init>(Loz/i6;Lwa0/u;)V

    new-array v3, v8, [Lic0/a;

    aput-object v0, v3, v10

    aput-object v5, v3, v9

    invoke-virtual {p1, v2, v3}, Lhc0/b;->f(Lcm0/d;[Lic0/a;)V

    goto :goto_1

    :cond_2
    new-instance v6, Lfc0/y;

    invoke-direct {v6}, Ljava/lang/Object;-><init>()V

    new-instance v11, Lic0/a0;

    invoke-direct {v11, v4, v3}, Lic0/a0;-><init>(Loz/i6;Lwa0/u;)V

    new-array v8, v8, [Lic0/a;

    aput-object v6, v8, v10

    aput-object v11, v8, v9

    invoke-virtual {p1, v2, v8}, Lhc0/b;->i(Lcm0/d;[Lic0/a;)V

    invoke-virtual {v7, p1, v2, v5, v10}, Lfc0/k1;->f(Lhc0/b;Lcm0/d;Lfc0/m1;Z)V

    invoke-virtual {v0}, Lfz/h;->b()Z

    move-result v0

    if-eqz v0, :cond_3

    new-instance v0, Lic0/a0;

    invoke-direct {v0, v4, v3}, Lic0/a0;-><init>(Loz/i6;Lwa0/u;)V

    new-array v3, v9, [Lic0/a;

    aput-object v0, v3, v10

    invoke-virtual {p1, v2, v3}, Lhc0/b;->y(Lcm0/d;[Lic0/a;)V

    :cond_3
    :goto_1
    return-object v1

    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_0
    .end packed-switch
.end method
