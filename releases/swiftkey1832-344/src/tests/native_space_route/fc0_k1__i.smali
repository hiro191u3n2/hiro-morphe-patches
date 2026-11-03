.method public final i(Lhc0/b;Lcm0/d;Lfc0/g2;Lfc0/m1;Lxc0/h;Z)V
    .locals 17

    move-object/from16 v0, p0

    move-object/from16 v1, p1

    move-object/from16 v2, p2

    move-object/from16 v3, p3

    move-object/from16 v4, p4

    new-instance v5, Lfc0/y;

    invoke-direct {v5}, Ljava/lang/Object;-><init>()V

    const/4 v6, 0x1

    new-array v7, v6, [Lic0/a;

    const/4 v8, 0x0

    aput-object v5, v7, v8

    invoke-virtual {v1, v2, v7}, Lhc0/b;->i(Lcm0/d;[Lic0/a;)V

    new-instance v5, Lic0/g;

    const/16 v7, 0x20

    invoke-direct {v5, v3, v7}, Lic0/g;-><init>(Lfc0/d2;I)V

    new-array v7, v6, [Lic0/a;

    aput-object v5, v7, v8

    invoke-virtual {v1, v2, v7}, Lhc0/b;->l(Lcm0/d;[Lic0/a;)V

    sget-object v5, Lv20/c;->h:Lv20/c;

    new-instance v7, Lfc0/n;

    sget-object v9, Loz/n4;->b:Loz/n4;

    invoke-direct {v7, v5, v0, v9}, Lfc0/n;-><init>(Lv20/c;Lfc0/k1;Loz/n4;)V

    new-array v9, v6, [Lic0/a;

    aput-object v7, v9, v8

    invoke-virtual {v1, v2, v9}, Lhc0/b;->i(Lcm0/d;[Lic0/a;)V

    iget-object v7, v0, Lfc0/k1;->b:Lfb0/r1;

    invoke-interface {v7}, Lfb0/r1;->d()Z

    move-result v9

    const/16 v10, 0x96

    if-nez v9, :cond_0

    new-instance v9, Lfc0/n;

    sget-object v11, Loz/n4;->c:Loz/n4;

    invoke-direct {v9, v5, v0, v11}, Lfc0/n;-><init>(Lv20/c;Lfc0/k1;Loz/n4;)V

    new-array v5, v6, [Lic0/a;

    aput-object v9, v5, v8

    invoke-virtual {v1, v10, v2, v5}, Lhc0/b;->p(ILcm0/d;[Lic0/a;)V

    :cond_0
    if-eqz p6, :cond_1

    new-instance v5, Lic0/t;

    iget-object v9, v0, Lfc0/k1;->l:Lbp/f;

    iget-object v11, v0, Lfc0/k1;->d:Ljava/lang/Object;

    invoke-direct {v5, v9, v11}, Lic0/t;-><init>(Lbp/f;Lwb0/t0;)V

    new-array v9, v6, [Lic0/a;

    aput-object v5, v9, v8

    invoke-virtual {v1, v2, v9}, Lhc0/b;->i(Lcm0/d;[Lic0/a;)V

    invoke-interface {v7}, Lfb0/r1;->d()Z

    move-result v9

    if-nez v9, :cond_2

    new-array v9, v6, [Lic0/a;

    aput-object v5, v9, v8

    invoke-virtual {v1, v10, v2, v9}, Lhc0/b;->p(ILcm0/d;[Lic0/a;)V

    goto :goto_0

    :cond_1
    new-instance v5, Lfc0/q0;

    invoke-direct {v5, v0, v8}, Lfc0/q0;-><init>(Lfc0/k1;I)V

    new-array v9, v6, [Lic0/a;

    aput-object v5, v9, v8

    new-instance v5, Laa0/f;

    const/16 v11, 0x10

    invoke-direct {v5, v11}, Laa0/f;-><init>(I)V

    invoke-virtual {v1, v5, v9}, Lhc0/b;->x(Lcm0/d;[Lic0/a;)V

    new-instance v5, Lfc0/d1;

    const-string v9, " "

    invoke-direct {v5, v0, v9, v9, v8}, Lfc0/d1;-><init>(Lfc0/k1;Ljava/lang/String;Ljava/lang/String;Z)V

    new-array v9, v6, [Lic0/a;

    aput-object v5, v9, v8

    invoke-virtual {v1, v2, v9}, Lhc0/b;->i(Lcm0/d;[Lic0/a;)V

    invoke-interface {v7}, Lfb0/r1;->d()Z

    move-result v9

    if-nez v9, :cond_2

    new-array v9, v6, [Lic0/a;

    aput-object v5, v9, v8

    invoke-virtual {v1, v10, v2, v9}, Lhc0/b;->p(ILcm0/d;[Lic0/a;)V

    :cond_2
    :goto_0
    iget v5, v0, Lfc0/k1;->w:I

    invoke-virtual {v0, v1, v2, v3, v5}, Lfc0/k1;->d(Lhc0/b;Lcm0/d;Lfc0/g2;I)V

    new-instance v5, Le60/b;

    new-instance v9, Landroidx/lifecycle/h;

    const/4 v11, 0x7

    move-object/from16 v12, p5

    invoke-direct {v9, v12, v11}, Landroidx/lifecycle/h;-><init>(Ljava/lang/Object;I)V

    invoke-direct {v5, v3, v9}, Le60/b;-><init>(Lfc0/d2;Lcm0/a;)V

    iget-object v3, v5, Le60/b;->d:Ljava/lang/Object;

    check-cast v3, Lic0/w;

    new-instance v9, Lfc0/o2;

    iget-object v11, v4, Lfc0/m1;->a:Landroid/graphics/RectF;

    invoke-virtual {v11}, Landroid/graphics/RectF;->width()F

    move-result v11

    const v12, 0x3dcccccd    # 0.1f

    mul-float/2addr v11, v12

    invoke-direct {v9, v11}, Lfc0/o2;-><init>(F)V

    new-instance v11, Lcom/touchtype/common/languagepacks/f0;

    const/4 v12, 0x5

    const-string v13, "direction"

    invoke-static {v12, v13}, Lk1/z;->v(ILjava/lang/String;)V

    new-instance v14, Llc0/c;

    invoke-static {v12}, Lk1/z;->d(I)I

    move-result v15

    invoke-direct {v14, v15}, Llc0/c;-><init>(I)V

    invoke-direct {v11, v14, v9}, Lcom/touchtype/common/languagepacks/f0;-><init>(Llc0/b;Lcm0/a;)V

    new-instance v14, Lcom/touchtype/common/languagepacks/f0;

    invoke-static {v6, v13}, Lk1/z;->v(ILjava/lang/String;)V

    new-instance v13, Llc0/c;

    invoke-static {v6}, Lk1/z;->d(I)I

    move-result v15

    invoke-direct {v13, v15}, Llc0/c;-><init>(I)V

    invoke-direct {v14, v13, v9}, Lcom/touchtype/common/languagepacks/f0;-><init>(Llc0/b;Lcm0/a;)V

    new-instance v13, Lcom/touchtype/common/languagepacks/f0;

    new-instance v15, Li2/n;

    const/16 v10, 0x8

    invoke-direct {v15, v10}, Li2/n;-><init>(I)V

    invoke-direct {v13, v15, v9}, Lcom/touchtype/common/languagepacks/f0;-><init>(Llc0/b;Lcm0/a;)V

    new-instance v9, Lfc0/o0;

    const/4 v10, 0x3

    invoke-direct {v9, v10, v8, v0}, Lfc0/o0;-><init>(IILjava/lang/Object;)V

    new-array v10, v6, [Lic0/a;

    aput-object v9, v10, v8

    invoke-virtual {v1, v14, v2, v10}, Lhc0/b;->n(Lcom/touchtype/common/languagepacks/f0;Lcm0/d;[Lic0/a;)V

    new-instance v9, Lfc0/o0;

    const/4 v10, 0x4

    invoke-direct {v9, v10, v8, v0}, Lfc0/o0;-><init>(IILjava/lang/Object;)V

    new-array v10, v6, [Lic0/a;

    aput-object v9, v10, v8

    invoke-virtual {v1, v11, v2, v10}, Lhc0/b;->n(Lcom/touchtype/common/languagepacks/f0;Lcm0/d;[Lic0/a;)V

    iget-object v9, v5, Le60/b;->c:Ljava/lang/Object;

    check-cast v9, Lfc0/b1;

    new-instance v10, Landroidx/credentials/playservices/a;

    invoke-direct {v10, v9, v12}, Landroidx/credentials/playservices/a;-><init>(Ljava/lang/Object;I)V

    iget-object v5, v5, Le60/b;->e:Ljava/lang/Object;

    check-cast v5, Ldt/n;

    const/4 v11, 0x2

    new-array v12, v11, [Lic0/k;

    aput-object v10, v12, v8

    aput-object v5, v12, v6

    invoke-virtual {v1, v13}, Lhc0/b;->A(Lcom/touchtype/common/languagepacks/f0;)V

    move v5, v8

    :goto_1
    if-ge v5, v11, :cond_4

    aget-object v10, v12, v5

    iget-object v14, v1, Lhc0/b;->h:Landroidx/lifecycle/g2;

    if-eqz v14, :cond_3

    iget-object v14, v14, Landroidx/lifecycle/g2;->a:Ljava/lang/Object;

    check-cast v14, Ljava/util/ArrayList;

    if-eqz v14, :cond_3

    new-instance v15, Lhc0/o;

    move/from16 v16, v8

    new-instance v8, Lic0/m;

    iget-object v11, v13, Lcom/touchtype/common/languagepacks/f0;->a:Ljava/lang/Object;

    check-cast v11, Llc0/b;

    invoke-direct {v8, v11, v10}, Lic0/m;-><init>(Llc0/b;Lic0/k;)V

    invoke-direct {v15, v2, v8}, Lhc0/o;-><init>(Lcm0/d;Ljava/lang/Object;)V

    invoke-virtual {v14, v15}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_2

    :cond_3
    move/from16 v16, v8

    :goto_2
    add-int/lit8 v5, v5, 0x1

    move/from16 v8, v16

    const/4 v11, 0x2

    goto :goto_1

    :cond_4
    move/from16 v16, v8

    new-array v5, v6, [Lic0/a;

    aput-object v3, v5, v16

    invoke-virtual {v1, v2, v5}, Lhc0/b;->x(Lcm0/d;[Lic0/a;)V

    new-array v5, v6, [Lic0/a;

    aput-object v3, v5, v16

    invoke-virtual {v1, v2, v5}, Lhc0/b;->h(Lcm0/d;[Lic0/a;)V

    invoke-interface {v7}, Lfb0/r1;->d()Z

    move-result v3

    if-nez v3, :cond_5

    new-array v3, v6, [Lic0/a;

    aput-object v9, v3, v16

    const/16 v5, 0x96

    invoke-virtual {v1, v5, v2, v3}, Lhc0/b;->q(ILcm0/d;[Lic0/a;)V

    :cond_5
    invoke-virtual {v0, v1, v2, v4, v6}, Lfc0/k1;->f(Lhc0/b;Lcm0/d;Lfc0/m1;Z)V

    return-void
.end method
