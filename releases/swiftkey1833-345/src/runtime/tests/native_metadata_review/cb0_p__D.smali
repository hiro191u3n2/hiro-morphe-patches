.method public D()Z
    .locals 1

    iget-object v0, p0, Lcb0/p;->a:Ljava/lang/Object;

    check-cast v0, Lcom/microsoft/fluency/Prediction;

    invoke-virtual {v0}, Lcom/microsoft/fluency/Prediction;->isPrefix()Z

    move-result v0

    return v0
.end method
