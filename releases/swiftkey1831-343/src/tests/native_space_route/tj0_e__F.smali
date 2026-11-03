.method public F(Lef0/p;I)V
    .locals 2

    invoke-virtual {p0, p2}, Ltj0/e;->L(I)Lgf0/g;

    move-result-object v0

    iget-object v1, p0, Ltj0/e;->c:Ljava/lang/Object;

    check-cast v1, Ljava/util/HashMap;

    invoke-static {p2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object p2

    invoke-interface {v0, p1}, Lgf0/g;->c(Lef0/p;)Lgf0/g;

    move-result-object p1

    invoke-virtual {v1, p2, p1}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    return-void
.end method
