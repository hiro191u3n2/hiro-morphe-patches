.class public final Lma0/o;
.super Ljava/lang/Object;
.source "SourceFile"

.method public q(Lwb0/n0;Lyb0/a;)V
    .locals 9

    iget v0, p0, Lma0/o;->a:I

    packed-switch v0, :pswitch_data_0

    check-cast p2, Lyb0/e0;

    iget-object v0, p0, Lma0/o;->b:Ljava/lang/Object;

    check-cast v0, Lwb0/z0;

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Lwb0/z0;->e(Z)V

    invoke-virtual {p1}, Lwb0/n0;->C()Ldc0/a;

    move-result-object v1

    iget-object v4, p2, Lyb0/e0;->b:Lm40/b;

    invoke-interface {v4}, Lm40/b;->getCorrectionSpanReplacementText()Ljava/lang/String;

    move-result-object v2

    invoke-static {v2}, Lkotlin/jvm/internal/m;->c(Ljava/lang/Object;)V

    invoke-virtual {v2}, Ljava/lang/String;->length()I

    move-result v2

    const/4 v3, 0x0

    const-string v5, "PredictionInputEventHandler"

    if-nez v2, :cond_0

    const-string p1, "No prediction available. Ignoring..."

    invoke-static {v5, p1, v3}, Lb20/d;->k(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V

    goto :goto_0

    :cond_0
    invoke-interface {v1}, Ldc0/a;->K()Ljava/lang/String;

    move-result-object v2

    invoke-interface {v4}, Lm40/b;->subrequest()Ll40/c;

    move-result-object v6

    iget-object v6, v6, Ll40/c;->i:Ljava/lang/String;

    invoke-static {v2, v6}, Lkotlin/jvm/internal/m;->b(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_1

    const-string p1, "Prediction no longer relevant. Ignoring..."

    invoke-static {v5, p1, v3}, Lb20/d;->k(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V

    goto :goto_0

    :cond_1
    iget-object v8, p2, Lyb0/e0;->a:Llh0/a;

    iget-object v2, p0, Lma0/o;->d:Ljava/lang/Object;

    check-cast v2, Lac0/b;

    iget-object v6, p2, Lyb0/e0;->c:Lwb0/j;

    iget v7, p2, Lyb0/e0;->d:I

    move-object v5, v1

    check-cast v5, Lec0/n;

    move-object v3, p1

    invoke-virtual/range {v2 .. v7}, Lac0/b;->c(Lwb0/n0;Lm40/b;Lec0/n;Lwb0/j;I)V

    iget-boolean p1, v0, Lwb0/z0;->b0:Z

    if-eqz p1, :cond_2

    iget-object p1, p0, Lma0/o;->f:Ljava/lang/Object;

    check-cast p1, Lac0/f;

    invoke-interface {v4}, Lm40/b;->getTrailingSeparator()Ljava/lang/String;

    move-result-object p2

    const/4 v0, 0x1

    invoke-virtual {p1, v3, v5, p2, v0}, Lac0/f;->h(Lwb0/n0;Lec0/n;Ljava/lang/String;Z)V

    iget-object p1, p0, Lma0/o;->c:Ljava/lang/Object;

    check-cast p1, Ltj0/e;

    invoke-virtual {p1, v5, v8}, Ltj0/e;->c0(Lec0/n;Llh0/a;)V

    goto :goto_0

    :cond_2
    iget-object p1, p0, Lma0/o;->e:Ljava/lang/Object;

    check-cast p1, Lfo/n;

    const/4 p2, -0x1

    invoke-virtual {p1, v3, v5, v8, p2}, Lfo/n;->i(Lwb0/n0;Lec0/n;Llh0/a;I)V

    :goto_0
    return-void

    :pswitch_0
    move-object v3, p1

    check-cast p2, Lyb0/n;

    invoke-virtual {v3}, Lwb0/n0;->C()Ldc0/a;

    move-result-object p1

    iget-object v0, p2, Lyb0/n;->b:Lm40/c0;

    invoke-virtual {v0}, Lm40/c0;->size()I

    move-result v1

    const/4 v2, 0x2

    if-ge v1, v2, :cond_3

    goto :goto_1

    :cond_3
    iget-object v1, p0, Lma0/o;->b:Ljava/lang/Object;

    check-cast v1, Lec0/x;

    invoke-virtual {v1}, Lec0/x;->E()Lec0/y;

    move-result-object v1

    invoke-interface {p1}, Ldc0/a;->getContext()Lcom/microsoft/fluency/Sequence;

    move-result-object v2

    iget-object v4, v0, Lm40/c0;->c:Ll40/c;

    iget-object v4, v4, Ll40/c;->c:Lcom/microsoft/fluency/Sequence;

    invoke-static {v2, v4}, Lkotlin/jvm/internal/m;->b(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_4

    goto :goto_1

    :cond_4
    iget-object p2, p2, Lyb0/n;->a:Llh0/a;

    new-instance v2, Lm40/a0;

    invoke-direct {v2, v0}, Lm40/a0;-><init>(Lm40/c0;)V

    iget-object v4, p0, Lma0/o;->d:Ljava/lang/Object;

    check-cast v4, Lac0/b;

    iget-object v5, v4, Lac0/b;->e:Lwb0/z0;

    iget-boolean v5, v5, Lwb0/z0;->Q:Z

    if-nez v5, :cond_5

    move-object v5, p1

    check-cast v5, Lec0/n;

    invoke-static {v3, v5}, Lac0/b;->g(Lwb0/o0;Lec0/n;)V

    :cond_5
    const/4 v5, 0x1

    invoke-virtual {v4, v2, v5}, Lac0/b;->k(Lm40/b;Z)V

    check-cast p1, Lec0/n;

    invoke-virtual {v4, v2, p1}, Lac0/b;->h(Lm40/b;Lec0/n;)V

    invoke-virtual {v3, v2, p1}, Lwb0/n0;->z(Lm40/a0;Lec0/n;)Z

    invoke-virtual {v2}, Lm40/a0;->getTrailingSeparator()Ljava/lang/String;

    move-result-object v2

    iget-object v4, p0, Lma0/o;->e:Ljava/lang/Object;

    check-cast v4, Lwb0/q;

    invoke-static {v2}, Lkotlin/jvm/internal/m;->c(Ljava/lang/Object;)V

    invoke-virtual {v4, v2}, Lwb0/q;->d(Ljava/lang/String;)V

    const-string v4, ""

    invoke-virtual {v3, v2, v4}, Lwb0/n0;->n(Ljava/lang/String;Ljava/lang/String;)Z

    invoke-virtual {v1, v0, v5}, Lec0/y;->k(Lm40/c0;I)V

    invoke-virtual {v3, p1, v1}, Lwb0/n0;->g(Lec0/n;Lec0/y;)Z

    iget-object v0, p0, Lma0/o;->c:Ljava/lang/Object;

    check-cast v0, Lwb0/p1;

    const/4 v1, 0x0

    iput-boolean v1, v0, Lwb0/p1;->j:Z

    iget-object v0, p0, Lma0/o;->f:Ljava/lang/Object;

    check-cast v0, Ltj0/e;

    invoke-virtual {v0, p1, p2}, Ltj0/e;->b0(Lec0/n;Llh0/a;)V

    :goto_1
    return-void

    :pswitch_data_0
    .packed-switch 0x2
        :pswitch_0
    .end packed-switch
.end method
