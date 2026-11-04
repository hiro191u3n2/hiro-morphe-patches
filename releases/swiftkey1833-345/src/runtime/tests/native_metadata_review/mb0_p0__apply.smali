.method public final apply(Ljava/lang/Object;)Z
    .locals 1

    check-cast p1, Lm40/b;

    if-eqz p1, :cond_0

    sget-object v0, Lm40/r;->g:Lm40/f;

    invoke-interface {p1, v0}, Lm40/b;->accept(Lm40/a;)Ljava/lang/Object;

    move-result-object p1

    const-string v0, "accept(...)"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    check-cast p1, Ljava/lang/Boolean;

    invoke-virtual {p1}, Ljava/lang/Boolean;->booleanValue()Z

    move-result p1

    if-eqz p1, :cond_0

    const/4 p1, 0x1

    return p1

    :cond_0
    const/4 p1, 0x0

    return p1
.end method
