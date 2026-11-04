.method public final d(Lef0/p;)V
    .locals 2

    iget-object v0, p0, Loc0/m;->a:Lfc0/d2;

    const/4 v1, 0x1

    invoke-interface {v0, v1}, Lfc0/d2;->z(Z)V

    iput-boolean v1, p0, Loc0/m;->e:Z

    iget-object v0, p0, Loc0/m;->d:Landroid/graphics/PointF;

    invoke-virtual {p1}, Lef0/p;->g()Landroid/graphics/PointF;

    move-result-object v1

    invoke-virtual {v0, v1}, Landroid/graphics/PointF;->set(Landroid/graphics/PointF;)V

    iget-object v0, p0, Loc0/m;->g:Loc0/j;

    invoke-interface {v0, p1}, Loc0/j;->j(Lef0/p;)Z

    move-result v0

    if-nez v0, :cond_0

    iget-object v0, p0, Loc0/m;->b:Lhc0/a;

    invoke-virtual {v0, p1}, Lhc0/a;->d(Lef0/p;)V

    iget-object v0, p0, Loc0/m;->f:Ljava/util/ArrayList;

    invoke-virtual {v0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :goto_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_0

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Loc0/u;

    invoke-interface {v1, p1}, Loc0/n;->d(Lef0/p;)V

    goto :goto_0

    :cond_0
    return-void
.end method
