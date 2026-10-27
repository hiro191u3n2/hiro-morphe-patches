.class public final Lwb0/x;
.super Ljava/lang/Object;
.source "SourceFile"

.method public final y(Llh0/a;)V
    .locals 9

    iget-object v0, p0, Lwb0/x;->f:Lec0/x;

    const-string v1, "breadcrumb"

    invoke-static {p1, v1}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    :try_start_0
    iget-object v1, p0, Lwb0/x;->e:Lwb0/z0;

    iget-boolean v1, v1, Lwb0/z0;->h:Z
    :try_end_0
    .catch Lwb0/u0; {:try_start_0 .. :try_end_0} :catch_2

    if-nez v1, :cond_0

    :try_start_1
    sget-object v1, Ljava/util/Collections;->EMPTY_MAP:Ljava/util/Map;

    invoke-virtual {v0}, Lec0/x;->F()Lec0/l;

    move-result-object v2

    invoke-interface {v2}, Lec0/l;->get()Lah0/a0;

    move-result-object v2

    invoke-virtual {p0, v2}, Lwb0/x;->k0(Lah0/a0;)V

    invoke-static {v1}, Lkotlin/jvm/internal/m;->c(Ljava/lang/Object;)V

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Lec0/x;->L(Z)V
    :try_end_1
    .catch Lwb0/u0; {:try_start_1 .. :try_end_1} :catch_0

    goto :goto_0

    :catch_0
    move-exception v0

    move-object p1, v0

    move-object v1, p0

    goto :goto_2

    :cond_0
    :goto_0
    :try_start_2
    new-instance v0, Lyb0/n0;

    invoke-direct {v0, p1}, Lyb0/n0;-><init>(Llh0/a;)V

    invoke-virtual {p0, v0}, Lwb0/x;->i0(Lyb0/a;)V
    :try_end_2
    .catch Lwb0/u0; {:try_start_2 .. :try_end_2} :catch_2

    const/4 v7, -0x1

    const/4 v8, -0x1

    const/4 v3, -0x1

    const/4 v4, -0x1

    const/4 v5, -0x1

    const/4 v6, -0x1

    move-object v1, p0

    move-object v2, p1

    :try_start_3
    invoke-virtual/range {v1 .. v8}, Lwb0/x;->v0(Llh0/a;IIIIII)V
    :try_end_3
    .catch Lwb0/u0; {:try_start_3 .. :try_end_3} :catch_1

    return-void

    :catch_1
    move-exception v0

    :goto_1
    move-object p1, v0

    goto :goto_2

    :catch_2
    move-exception v0

    move-object v1, p0

    goto :goto_1

    :goto_2
    const-string v0, "error"

    const-string v2, "onEnterKey"

    invoke-static {v2, v0, p1}, Lb20/d;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V

    invoke-virtual {p0, v2, p1}, Lwb0/x;->p0(Ljava/lang/String;Lwb0/u0;)V

    return-void
.end method
