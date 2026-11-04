.method public final c(Lef0/p;)V
    .locals 2

    iget-object v0, p0, Loc0/m;->a:Lfc0/d2;

    const/4 v1, 0x0

    invoke-interface {v0, v1}, Lfc0/d2;->z(Z)V

    invoke-interface {v0, v1}, Lfc0/d2;->r(Z)V

    iget-object v0, p0, Loc0/m;->g:Loc0/j;

    invoke-interface {v0, p1}, Loc0/j;->f(Lef0/p;)Z

    move-result v0

    if-nez v0, :cond_0

    iget-object v0, p0, Loc0/m;->b:Lhc0/a;

    invoke-virtual {v0, p1}, Lhc0/a;->c(Lef0/p;)V

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

    invoke-interface {v1, p1}, Loc0/n;->c(Lef0/p;)V

    goto :goto_0

    :cond_0
    return-void
.end method
