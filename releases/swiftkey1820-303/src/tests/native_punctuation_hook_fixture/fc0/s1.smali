.class public final Lfc0/s1;
.super Ljava/lang/Object;

.method public final q(Ljava/util/List;Ljava/lang/String;Ljava/lang/String;Lfc0/m1;Lfc0/v1;)Lfb0/f2;
    .locals 4

    if-eqz p2, :cond_3

    move-object v0, p1

    check-cast v0, Ljava/util/Collection;

    invoke-interface {v0}, Ljava/util/Collection;->isEmpty()Z

    move-result v0

    if-nez v0, :cond_3

    filled-new-array {p3}, [Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Ln10/a;->F([Ljava/lang/Object;)Ljava/util/ArrayList;

    move-result-object v0

    invoke-interface {p1, p2}, Ljava/util/List;->contains(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_0

    invoke-virtual {p2, p3}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_0

    invoke-virtual {v0, p2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_0
    move-object v1, p1

    check-cast v1, Ljava/util/Collection;

    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->addAll(Ljava/util/Collection;)Z

    new-instance v1, Ljava/util/ArrayList;

    invoke-direct {v1}, Ljava/util/ArrayList;-><init>()V

    const/4 v2, 0x0

    const/4 v3, 0x1

    invoke-virtual {p0, p2, v1, v2, v3}, Lfc0/s1;->O(Ljava/lang/String;Ljava/util/List;ZZ)Ljava/util/ArrayList;

    move-result-object p2

    invoke-virtual {p2}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object p2

    :cond_1
    :goto_0
    invoke-interface {p2}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_2

    invoke-interface {p2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/lang/String;

    invoke-interface {p1, v1}, Ljava/util/List;->contains(Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_1

    invoke-static {v1, p3}, Lkotlin/jvm/internal/m;->b(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_1

    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_0

    :cond_2
    iget-object p1, p0, Lfc0/s1;->i:Lfb0/h2;

    invoke-static {v0}, Lol0/q;->f1(Ljava/lang/Iterable;)Ljava/util/List;

    move-result-object p2

    invoke-virtual {p1, p2, p5, p4}, Lfb0/h2;->a(Ljava/util/List;Lfc0/v1;Lfc0/m1;)Lfb0/f2;

    move-result-object p1

    return-object p1

    :cond_3
    const/4 p1, 0x0

    return-object p1
.end method

.method public final c(Lfc0/m1;Lfc0/v1;)Lxc0/a;
    .locals 5

    invoke-virtual {p2}, Lfc0/v1;->a()Ljava/lang/String;

    move-result-object v0

    iget-object v1, p2, Lfc0/v1;->o:Ljava/util/List;

    const/4 v2, 0x0

    invoke-virtual {p0, v0, v1, v2, v2}, Lfc0/s1;->O(Ljava/lang/String;Ljava/util/List;ZZ)Ljava/util/ArrayList;

    move-result-object v3

    const/4 v4, 0x1

    invoke-virtual {p0, v0, v1, v4, v2}, Lfc0/s1;->O(Ljava/lang/String;Ljava/util/List;ZZ)Ljava/util/ArrayList;

    move-result-object v0

    invoke-virtual {v3}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v1

    if-eqz v1, :cond_0

    invoke-virtual {v0}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v1

    if-eqz v1, :cond_0

    sget-object p1, Lxc0/d;->a:Lxc0/d;

    :goto_0
    move-object p2, p1

    goto :goto_1

    :cond_0
    invoke-virtual {v3, v0}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_1

    invoke-virtual {p0, v3, v4, p1, p2}, Lfc0/s1;->P(Ljava/util/List;ZLfc0/m1;Lfc0/v1;)Lxc0/h;

    move-result-object v1

    invoke-virtual {p0, v0, v4, p1, p2}, Lfc0/s1;->P(Ljava/util/List;ZLfc0/m1;Lfc0/v1;)Lxc0/h;

    move-result-object p1

    move-object p2, p1

    move-object p1, v1

    goto :goto_1

    :cond_1
    invoke-virtual {p0, v3, v4, p1, p2}, Lfc0/s1;->P(Ljava/util/List;ZLfc0/m1;Lfc0/v1;)Lxc0/h;

    move-result-object p1

    goto :goto_0

    :goto_1
    new-instance v0, Lxc0/a;

    invoke-direct {v0, p1, p2}, Lxc0/a;-><init>(Lxc0/h;Lxc0/h;)V

    return-object v0
.end method
