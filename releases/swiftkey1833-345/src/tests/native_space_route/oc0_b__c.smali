.method public final c(Lef0/p;)V
    .locals 3

    iget-boolean v0, p0, Loc0/b;->a:Z

    if-eqz v0, :cond_1

    sget-object v0, Lhc0/c;->d:Lhc0/c;

    sget-object v1, Lhc0/c;->a:Lhc0/c;

    sget-object v2, Lhc0/c;->b:Lhc0/c;

    invoke-static {v0, v1, v2}, Ljava/util/EnumSet;->of(Ljava/lang/Enum;Ljava/lang/Enum;Ljava/lang/Enum;)Ljava/util/EnumSet;

    move-result-object v0

    iget-object v1, p0, Loc0/b;->c:Loc0/m;

    invoke-virtual {v1, v0}, Loc0/m;->a(Ljava/util/EnumSet;)Z

    move-result v0

    if-nez v0, :cond_0

    sget-object v0, Lhc0/c;->k:Ljava/util/EnumSet;

    invoke-virtual {v1, v0}, Loc0/m;->a(Ljava/util/EnumSet;)Z

    move-result v0

    if-nez v0, :cond_0

    iget-object v0, v1, Loc0/m;->b:Lhc0/a;

    invoke-virtual {v0, p1}, Lhc0/a;->l(Lef0/p;)V

    :cond_0
    const/4 p1, 0x1

    iput-boolean p1, p0, Loc0/b;->b:Z

    :cond_1
    return-void
.end method
