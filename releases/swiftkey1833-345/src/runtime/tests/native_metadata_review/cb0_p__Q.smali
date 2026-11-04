.method public Q()Z
    .locals 2

    iget-object v0, p0, Lcb0/p;->a:Ljava/lang/Object;

    check-cast v0, Lcom/microsoft/fluency/Prediction;

    invoke-virtual {v0}, Lcom/microsoft/fluency/Prediction;->isPrefix()Z

    move-result v1

    if-nez v1, :cond_0

    invoke-virtual {v0}, Lcom/microsoft/fluency/Prediction;->isVerbatim()Z

    move-result v0

    if-eqz v0, :cond_0

    const/4 v0, 0x1

    return v0

    :cond_0
    const/4 v0, 0x0

    return v0
.end method
