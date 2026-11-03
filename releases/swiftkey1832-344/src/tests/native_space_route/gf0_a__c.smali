.method public final c(Lef0/p;)Lgf0/g;
    .locals 1

    iget-object v0, p0, Lgf0/a;->b:Lfc0/f;

    if-eqz v0, :cond_0

    check-cast v0, Lfc0/n2;

    invoke-virtual {v0, p1}, Lfc0/n2;->c(Lef0/p;)V

    :cond_0
    iget-object p1, p0, Lgf0/a;->a:Lah0/a0;

    iget-object p1, p1, Lah0/a0;->a:Ljava/lang/Object;

    check-cast p1, Lgf0/h;

    return-object p1
.end method
