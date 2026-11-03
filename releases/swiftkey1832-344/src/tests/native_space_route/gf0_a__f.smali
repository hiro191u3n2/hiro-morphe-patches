.method public final f(Lfc0/f;Lef0/p;)Lgf0/g;
    .locals 2

    check-cast p1, Lfc0/n2;

    invoke-virtual {p1, p2}, Lfc0/n2;->d(Lef0/p;)V

    iget-object v0, p0, Lgf0/a;->a:Lah0/a0;

    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    new-instance v1, Lgf0/d;

    invoke-direct {v1, v0, p1, p2}, Lgf0/d;-><init>(Lah0/a0;Lfc0/f;Lef0/p;)V

    return-object v1
.end method
