.method public final e(Lhc0/b;Lcm0/d;Lfc0/g2;Lfc0/m1;Z)V
    .locals 12

    iget-object v0, p0, Lfc0/k1;->a:Lcom/touchtype/KeyboardService;

    const v1, 0x7f1406a2

    invoke-virtual {v0, v1}, Landroid/content/Context;->getString(I)Ljava/lang/String;

    move-result-object v0

    const-string v1, "getString(...)"

    invoke-static {v0, v1}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    new-instance v1, Lic0/g;

    const/16 v2, 0x20

    move-object v3, p3

    invoke-direct {v1, p3, v2}, Lic0/g;-><init>(Lfc0/d2;I)V

    const/4 v2, 0x1

    new-array v3, v2, [Lic0/a;

    const/4 v4, 0x0

    aput-object v1, v3, v4

    invoke-virtual {p1, p2, v3}, Lhc0/b;->l(Lcm0/d;[Lic0/a;)V

    iget-object v1, p0, Lfc0/k1;->h:Lfz/h;

    invoke-virtual {v1}, Lfz/h;->b()Z

    move-result v1

    const/4 v3, 0x2

    const-string v5, " "

    iget-object v6, p0, Lfc0/k1;->d:Ljava/lang/Object;

    iget-object v7, p0, Lfc0/k1;->l:Lbp/f;

    const/4 v8, 0x3

    sget-object v9, Loz/n4;->b:Loz/n4;

    if-eqz v1, :cond_1

    new-instance v1, Lfc0/y;

    invoke-direct {v1}, Ljava/lang/Object;-><init>()V

    sget-object v10, Lv20/c;->h:Lv20/c;

    new-instance v11, Lfc0/n;

    invoke-direct {v11, v10, p0, v9}, Lfc0/n;-><init>(Lv20/c;Lfc0/k1;Loz/n4;)V

    new-array v9, v3, [Lic0/a;

    aput-object v1, v9, v4

    aput-object v11, v9, v2

    invoke-virtual {p1, p2, v9}, Lhc0/b;->f(Lcm0/d;[Lic0/a;)V

    new-instance v1, Lfc0/o0;

    invoke-direct {v1, v8, v4, p0}, Lfc0/o0;-><init>(IILjava/lang/Object;)V

    new-instance v8, Lfb0/k2;

    invoke-direct {v8, v2, p0, v0}, Lfb0/k2;-><init>(ILjava/lang/Object;Ljava/lang/Object;)V

    new-array v0, v3, [Lic0/a;

    aput-object v1, v0, v4

    aput-object v8, v0, v2

    invoke-virtual {p1, p2, v0}, Lhc0/b;->g(Lcm0/d;[Lic0/a;)V

    if-eqz p5, :cond_0

    new-instance v0, Lic0/t;

    invoke-direct {v0, v7, v6}, Lic0/t;-><init>(Lbp/f;Lwb0/t0;)V

    new-array v1, v2, [Lic0/a;

    aput-object v0, v1, v4

    invoke-virtual {p1, p2, v1}, Lhc0/b;->f(Lcm0/d;[Lic0/a;)V

    return-void

    :cond_0
    new-instance v0, Lfc0/q0;

    invoke-direct {v0, p0, v4}, Lfc0/q0;-><init>(Lfc0/k1;I)V

    new-instance v1, Lfc0/d1;

    invoke-direct {v1, p0, v5, v5, v4}, Lfc0/d1;-><init>(Lfc0/k1;Ljava/lang/String;Ljava/lang/String;Z)V

    new-array v3, v3, [Lic0/a;

    aput-object v0, v3, v4

    aput-object v1, v3, v2

    invoke-virtual {p1, p2, v3}, Lhc0/b;->f(Lcm0/d;[Lic0/a;)V

    return-void

    :cond_1
    new-instance v1, Lfc0/y;

    invoke-direct {v1}, Ljava/lang/Object;-><init>()V

    sget-object v10, Lv20/c;->h:Lv20/c;

    new-instance v11, Lfc0/n;

    invoke-direct {v11, v10, p0, v9}, Lfc0/n;-><init>(Lv20/c;Lfc0/k1;Loz/n4;)V

    new-array v9, v3, [Lic0/a;

    aput-object v1, v9, v4

    aput-object v11, v9, v2

    invoke-virtual {p1, p2, v9}, Lhc0/b;->i(Lcm0/d;[Lic0/a;)V

    new-instance v1, Lfc0/n;

    sget-object v9, Loz/n4;->e:Loz/n4;

    invoke-direct {v1, v10, p0, v9}, Lfc0/n;-><init>(Lv20/c;Lfc0/k1;Loz/n4;)V

    new-array v9, v2, [Lic0/a;

    aput-object v1, v9, v4

    invoke-virtual {p1, p2, v9}, Lhc0/b;->y(Lcm0/d;[Lic0/a;)V

    iget v1, p0, Lfc0/k1;->w:I

    new-instance v9, Lfc0/o0;

    invoke-direct {v9, v8, v4, p0}, Lfc0/o0;-><init>(IILjava/lang/Object;)V

    new-array v8, v2, [Lic0/a;

    aput-object v9, v8, v4

    invoke-virtual {p1, v1, p2, v8}, Lhc0/b;->p(ILcm0/d;[Lic0/a;)V

    iget v1, p0, Lfc0/k1;->w:I

    new-instance v8, Lfb0/k2;

    invoke-direct {v8, v2, p0, v0}, Lfb0/k2;-><init>(ILjava/lang/Object;Ljava/lang/Object;)V

    new-array v0, v2, [Lic0/a;

    aput-object v8, v0, v4

    invoke-virtual {p1, v1, p2, v0}, Lhc0/b;->q(ILcm0/d;[Lic0/a;)V

    if-eqz p5, :cond_2

    new-instance v0, Lic0/t;

    invoke-direct {v0, v7, v6}, Lic0/t;-><init>(Lbp/f;Lwb0/t0;)V

    new-array v1, v2, [Lic0/a;

    aput-object v0, v1, v4

    invoke-virtual {p1, p2, v1}, Lhc0/b;->i(Lcm0/d;[Lic0/a;)V

    new-array v1, v2, [Lic0/a;

    aput-object v0, v1, v4

    invoke-virtual {p1, p2, v1}, Lhc0/b;->y(Lcm0/d;[Lic0/a;)V

    :goto_0
    move-object/from16 v0, p4

    goto :goto_1

    :cond_2
    new-instance v0, Lfc0/q0;

    invoke-direct {v0, p0, v4}, Lfc0/q0;-><init>(Lfc0/k1;I)V

    new-instance v1, Lfc0/d1;

    invoke-direct {v1, p0, v5, v5, v4}, Lfc0/d1;-><init>(Lfc0/k1;Ljava/lang/String;Ljava/lang/String;Z)V

    new-array v5, v3, [Lic0/a;

    aput-object v0, v5, v4

    aput-object v1, v5, v2

    invoke-virtual {p1, p2, v5}, Lhc0/b;->i(Lcm0/d;[Lic0/a;)V

    new-array v3, v3, [Lic0/a;

    aput-object v0, v3, v4

    aput-object v1, v3, v2

    invoke-virtual {p1, p2, v3}, Lhc0/b;->y(Lcm0/d;[Lic0/a;)V

    goto :goto_0

    :goto_1
    invoke-virtual {p0, p1, p2, v0, v2}, Lfc0/k1;->f(Lhc0/b;Lcm0/d;Lfc0/m1;Z)V

    return-void
.end method
