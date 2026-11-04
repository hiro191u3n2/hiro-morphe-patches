.class public final Lec0/w;
.super Lm40/a;
.source "SourceFile"

.method public final d(Lm40/x;)Ljava/lang/Object;
    .locals 11

    iget-object v0, p0, Lec0/w;->d:Lm40/b;

    invoke-interface {v0}, Lm40/b;->getUserFacingText()Ljava/lang/String;

    move-result-object v0

    iget-object v1, p0, Lec0/w;->e:Lec0/x;

    iget-object v2, v1, Lec0/x;->h:Lfb0/e2;

    iget v2, v2, Lfb0/e2;->a:I

    iget-object v5, p0, Lec0/w;->c:Lec0/y;

    iget-object v3, p0, Lec0/w;->a:Lwb0/j;

    invoke-virtual {v5, p1, v3, v2}, Lec0/y;->i(Lm40/b;Lwb0/j;I)V

    iget-object v1, v1, Lec0/x;->a:Lec0/f;

    iget-object p1, p1, Lm40/x;->b:Lm40/b;

    invoke-interface {p1}, Lm40/b;->getTokens()Ljava/util/List;

    move-result-object v8

    invoke-interface {p1}, Lm40/b;->getTrailingSeparator()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v1}, Lec0/f;->r0()V

    invoke-virtual {v1, v0}, Lec0/f;->j0(Ljava/lang/String;)V

    iget-object v2, v1, Lec0/f;->j:Lr40/d;

    iget-object v9, v1, Lec0/f;->i:Lde0/g;

    iget-object v3, v9, Lde0/g;->g:Ljava/lang/Object;

    check-cast v3, Lr40/d;

    invoke-virtual {v3}, Ljava/util/AbstractCollection;->size()I

    move-result v3

    invoke-virtual {v2, v3}, Lr40/d;->c(I)V

    new-instance v3, Lr40/c;

    invoke-virtual {v2}, Lr40/d;->k()Lr40/c;

    move-result-object v4

    const/4 v10, 0x0

    if-nez v4, :cond_0

    move v4, v10

    goto :goto_0

    :cond_0
    invoke-virtual {v4}, Lr40/c;->c()I

    move-result v4

    :goto_0
    if-eqz p1, :cond_2

    invoke-virtual {p1}, Ljava/lang/String;->isEmpty()Z

    move-result p1

    if-nez p1, :cond_1

    goto :goto_1

    :cond_1
    move v6, v10

    goto :goto_2

    :cond_2
    :goto_1
    const/4 p1, 0x1

    move v6, p1

    :goto_2
    const/4 v7, 0x0

    invoke-direct/range {v3 .. v8}, Lr40/c;-><init>(ILr40/b;ZZLjava/util/List;)V

    iget-object p1, v9, Lde0/g;->b:Ljava/lang/Object;

    check-cast p1, Ljava/lang/String;

    sget-object v4, Lec0/f;->w:Ldr/o3;

    invoke-virtual {v4, p1}, Ldr/o3;->contains(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_5

    iget v4, v3, Lr40/c;->e:I

    invoke-virtual {v0}, Ljava/lang/String;->length()I

    move-result v0

    invoke-virtual {p1}, Ljava/lang/String;->length()I

    move-result p1

    sub-int/2addr v0, p1

    invoke-interface {v8}, Ljava/util/List;->size()I

    move-result p1

    invoke-static {v4, v0, p1}, Lec0/f;->U(III)V

    invoke-virtual {v2, v3}, Ljava/util/AbstractCollection;->add(Ljava/lang/Object;)Z

    iget-object p1, v9, Lde0/g;->f:Ljava/lang/Object;

    check-cast p1, Lr40/d;

    invoke-virtual {p1}, Ljava/util/AbstractCollection;->iterator()Ljava/util/Iterator;

    move-result-object p1

    :goto_3
    invoke-interface {p1}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_4

    invoke-interface {p1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lr40/c;

    invoke-virtual {v2}, Lr40/d;->k()Lr40/c;

    move-result-object v3

    if-nez v3, :cond_3

    move v3, v10

    goto :goto_4

    :cond_3
    invoke-virtual {v3}, Lr40/c;->c()I

    move-result v3

    :goto_4
    iput v3, v0, Lr40/c;->a:I

    invoke-virtual {v2, v0}, Ljava/util/AbstractCollection;->add(Ljava/lang/Object;)Z

    goto :goto_3

    :cond_4
    const/4 p1, 0x0

    invoke-virtual {v1}, Lec0/f;->o0()I

    move-result v0

    invoke-virtual {v1, p1, v0}, Lec0/f;->h0(Lec0/y;I)V

    invoke-virtual {v1}, Lec0/f;->q0()V

    sget-object p1, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    return-object p1

    :cond_5
    new-instance v0, Ljava/lang/IllegalStateException;

    const-string v1, "Invalid punctuation text: \""

    const-string v2, "\""

    invoke-static {v1, p1, v2}, La6/c;->l(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    invoke-direct {v0, p1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw v0
.end method

.method public final i(Lm40/c0;)Ljava/lang/Object;
    .locals 19

    move-object/from16 v0, p0

    move-object/from16 v2, p1

    iget-object v1, v0, Lec0/w;->e:Lec0/x;

    iget-object v3, v1, Lec0/x;->h:Lfb0/e2;

    sget-object v7, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    iget-object v4, v0, Lec0/w;->d:Lm40/b;

    invoke-static {v4}, Lm40/r;->c(Lm40/b;)Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/String;->length()I

    move-result v5

    iget-object v10, v0, Lec0/w;->c:Lec0/y;

    iget-object v6, v0, Lec0/w;->b:Ljava/lang/String;

    if-lez v5, :cond_3

    invoke-static {v6}, Lk1/z;->r(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v2}, Lm40/c0;->getTrailingSeparator()Ljava/lang/String;

    move-result-object v6

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v2}, Lm40/c0;->size()I

    move-result v6

    invoke-virtual {v10, v2, v6}, Lec0/y;->k(Lm40/c0;I)V

    iget v3, v3, Lfb0/e2;->a:I

    new-instance v13, Lec0/y;

    invoke-direct {v13}, Lec0/y;-><init>()V

    invoke-interface {v2}, Lm40/b;->getCorrectionSpanReplacementText()Ljava/lang/String;

    move-result-object v6

    invoke-virtual {v13, v6}, Lec0/y;->t(Ljava/lang/String;)V

    iget-object v6, v0, Lec0/w;->a:Lwb0/j;

    invoke-virtual {v13, v2, v6, v3}, Lec0/y;->i(Lm40/b;Lwb0/j;I)V

    iget-object v1, v1, Lec0/x;->a:Lec0/f;

    invoke-virtual {v2}, Lm40/c0;->getTokens()Ljava/util/List;

    move-result-object v16

    const/4 v3, 0x0

    invoke-static {v4, v3}, Lr40/g;->d(Ljava/lang/String;Z)Lr40/g;

    move-result-object v6

    invoke-static {v6}, Ldr/l1;->u(Ljava/lang/Object;)Ldr/e3;

    move-result-object v6

    invoke-virtual {v2}, Lm40/c0;->getTrailingSeparator()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1}, Lec0/f;->r0()V

    new-instance v8, Ljava/lang/StringBuilder;

    invoke-direct {v8}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v8, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v8, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v1, v5}, Lec0/f;->j0(Ljava/lang/String;)V

    iget-object v5, v1, Lec0/f;->j:Lr40/d;

    const/4 v8, 0x1

    invoke-virtual {v5, v8}, Lr40/d;->c(I)V

    invoke-virtual {v5}, Lr40/d;->k()Lr40/c;

    move-result-object v9

    if-nez v9, :cond_0

    move v12, v3

    goto :goto_0

    :cond_0
    invoke-virtual {v9}, Lr40/c;->c()I

    move-result v9

    move v12, v9

    :goto_0
    invoke-static {v2}, Lda0/a;->S0(Ljava/lang/String;)Z

    move-result v9

    xor-int/lit8 v14, v9, 0x1

    new-instance v11, Lr40/c;

    const/4 v15, 0x0

    invoke-direct/range {v11 .. v16}, Lr40/c;-><init>(ILr40/b;ZZLjava/util/List;)V

    invoke-virtual {v5, v11}, Ljava/util/AbstractCollection;->add(Ljava/lang/Object;)Z

    invoke-virtual {v11}, Lr40/c;->c()I

    move-result v13

    invoke-static {v2}, Lda0/a;->S0(Ljava/lang/String;)Z

    move-result v9

    if-nez v9, :cond_1

    new-instance v12, Lr40/c;

    filled-new-array {v2}, [Ljava/lang/String;

    move-result-object v2

    new-instance v9, Ljava/util/ArrayList;

    invoke-direct {v9, v8}, Ljava/util/ArrayList;-><init>(I)V

    aget-object v2, v2, v3

    invoke-static {v2}, Lu50/c;->j(Ljava/lang/String;)Z

    move-result v3

    invoke-static {v2, v3}, Lr40/g;->d(Ljava/lang/String;Z)Lr40/g;

    move-result-object v2

    invoke-virtual {v9, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    const/4 v14, 0x0

    const/4 v15, 0x1

    const/16 v16, 0x0

    move-object/from16 v17, v9

    invoke-direct/range {v12 .. v17}, Lr40/c;-><init>(ILr40/b;ZZLjava/util/List;)V

    invoke-virtual {v5, v12}, Ljava/util/AbstractCollection;->add(Ljava/lang/Object;)Z

    invoke-virtual {v12}, Lr40/c;->c()I

    move-result v13

    :cond_1
    move v9, v13

    invoke-virtual {v4}, Ljava/lang/String;->length()I

    move-result v2

    if-lez v2, :cond_2

    new-instance v8, Lr40/c;

    const/4 v11, 0x1

    const/4 v12, 0x0

    move-object v13, v6

    invoke-direct/range {v8 .. v13}, Lr40/c;-><init>(ILr40/b;ZZLjava/util/List;)V

    invoke-virtual {v5, v8}, Ljava/util/AbstractCollection;->add(Ljava/lang/Object;)Z

    :cond_2
    const/4 v2, 0x0

    invoke-virtual {v1}, Lec0/f;->o0()I

    move-result v3

    invoke-virtual {v1, v2, v3}, Lec0/f;->h0(Lec0/y;I)V

    return-object v7

    :cond_3
    move-object v5, v10

    iget-object v4, v0, Lec0/w;->a:Lwb0/j;

    iget v3, v3, Lfb0/e2;->a:I

    move-object/from16 v18, v6

    move v6, v3

    move-object v3, v4

    move-object/from16 v4, v18

    invoke-static/range {v1 .. v6}, Lec0/x;->A(Lec0/x;Lm40/b;Lwb0/j;Ljava/lang/String;Lec0/y;I)V

    return-object v7
.end method
