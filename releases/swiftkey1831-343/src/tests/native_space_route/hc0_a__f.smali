.method public final f(Llh0/a;Ljava/util/ArrayList;)V
    .locals 1

    if-eqz p2, :cond_1

    invoke-virtual {p2}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v0

    if-eqz v0, :cond_0

    goto :goto_1

    :cond_0
    iget-object v0, p0, Lhc0/a;->a:Lfc0/d2;

    invoke-static {p2, v0}, Lhc0/o;->a(Ljava/util/Collection;Lfc0/d2;)Ljava/util/List;

    move-result-object p2

    invoke-interface {p2}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p2

    :goto_0
    invoke-interface {p2}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_1

    invoke-interface {p2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lic0/a;

    invoke-interface {v0, p1}, Lic0/a;->b(Llh0/a;)V

    goto :goto_0

    :cond_1
    :goto_1
    return-void
.end method
