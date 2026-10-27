.class public final Lfb0/g0;
.super Ljava/lang/Object;
.implements Landroid/content/SharedPreferences$OnSharedPreferenceChangeListener;
.implements Lcc0/h;
.implements Lfb0/d1;
.implements Landroid/view/accessibility/AccessibilityManager$TouchExplorationStateChangeListener;

.method public final b(Llh0/a;Lfb0/q1;)V
    .locals 9

    iget-object v0, p0, Lfb0/g0;->a:Lfb0/x1;

    iget-object v1, v0, Lfb0/x1;->d:Lfb0/i0;

    invoke-virtual {v0}, Lfb0/x1;->p()Lfb0/a;

    move-result-object v6

    iget-object v2, v1, Lfb0/i0;->E:Lfb0/f5;

    iget-object v3, v1, Lfb0/i0;->U:Lee0/m;

    invoke-virtual {v3}, Lee0/m;->get()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/Boolean;

    invoke-virtual {v3}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v3

    const/4 v4, 0x1

    const/4 v5, 0x0

    if-eqz v3, :cond_0

    iget-object v3, v1, Lfb0/i0;->V:Lla0/a;

    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    invoke-virtual {v1}, Lfb0/i0;->c()Z

    move-result v3

    if-eqz v3, :cond_0

    iget-object v3, v1, Lfb0/i0;->k:Lfb0/q1;

    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    iget v7, p2, Lfb0/q1;->a:I

    iget v8, v3, Lfb0/q1;->a:I

    if-ne v7, v8, :cond_0

    iget-boolean v7, p2, Lfb0/q1;->d:Z

    iget-boolean v3, v3, Lfb0/q1;->d:Z

    if-ne v7, v3, :cond_0

    move v3, v5

    goto :goto_0

    :cond_0
    move v3, v4

    :goto_0
    iput-object p2, v1, Lfb0/i0;->k:Lfb0/q1;

    iget-object p2, v1, Lfb0/i0;->p:Lcr/t;

    invoke-virtual {p2}, Lcr/t;->c()Z

    move-result p2

    if-eqz p2, :cond_1

    iget-object p2, v1, Lfb0/i0;->p:Lcr/t;

    invoke-virtual {p2}, Lcr/t;->b()Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Lqj0/a;

    invoke-virtual {v2, p2}, Lfb0/f5;->q(Lqj0/a;)V

    sget-object p2, Lcr/a;->a:Lcr/a;

    iput-object p2, v1, Lfb0/i0;->p:Lcr/t;

    :cond_1
    invoke-virtual {v2}, Lfb0/f5;->i()Lqj0/a;

    move-result-object p2

    iget-object v7, v1, Lfb0/i0;->k:Lfb0/q1;

    iget v7, v7, Lfb0/q1;->a:I

    invoke-static {v7}, Ld0/y;->d(I)I

    move-result v7

    const/4 v8, 0x2

    if-eq v7, v8, :cond_5

    const/4 v8, 0x4

    if-eq v7, v8, :cond_5

    const/4 v2, 0x5

    if-eq v7, v2, :cond_4

    const/4 v2, 0x6

    if-eq v7, v2, :cond_3

    const/4 v2, 0x7

    if-eq v7, v2, :cond_2

    goto :goto_1

    :cond_2
    iget-object v2, v1, Lfb0/i0;->T:Lah0/v;

    iget v4, v1, Lfb0/i0;->W:I

    iget-object v2, v2, Lah0/v;->a:Ljava/lang/Object;

    invoke-static {p2, v4}, Lus/e;->y(Lqj0/a;I)Lqj0/a;

    move-result-object p2

    goto :goto_1

    :cond_3
    sget-object p2, Lqj0/a;->E1:Lqj0/a;

    goto :goto_1

    :cond_4
    sget-object p2, Lqj0/a;->F1:Lqj0/a;

    goto :goto_1

    :cond_5
    invoke-virtual {p2}, Lqj0/a;->h()Z

    move-result v7

    if-nez v7, :cond_7

    new-instance v7, Lcr/c0;

    invoke-direct {v7, p2}, Lcr/c0;-><init>(Ljava/lang/Object;)V

    iput-object v7, v1, Lfb0/i0;->p:Lcr/t;

    invoke-virtual {v2}, Lfb0/f5;->l()I

    move-result p2

    const/4 v7, -0x1

    if-ne p2, v7, :cond_6

    invoke-virtual {v2}, Lfb0/f5;->b()V

    iget-object p2, v2, Lfb0/f5;->b:Ljava/lang/Iterable;

    check-cast p2, Ljava/util/ArrayList;

    invoke-virtual {p2}, Ljava/util/ArrayList;->size()I

    move-result p2

    sub-int/2addr p2, v4

    invoke-virtual {v2, p2}, Lfb0/f5;->t(I)Lqj0/a;

    move-result-object p2

    goto :goto_1

    :cond_6
    invoke-virtual {v2, p2}, Lfb0/f5;->t(I)Lqj0/a;

    move-result-object p2

    :cond_7
    :goto_1
    iput-object p2, v1, Lfb0/i0;->m:Lqj0/a;

    iput-boolean v5, v1, Lfb0/i0;->q:Z

    const/4 v4, 0x7

    move-object v5, p0

    move-object v2, p1

    invoke-virtual/range {v1 .. v6}, Lfb0/i0;->e(Llh0/a;ZILfb0/d1;Lfb0/a;)V

    invoke-virtual {v0}, Lfb0/x1;->u()V

    return-void
.end method
