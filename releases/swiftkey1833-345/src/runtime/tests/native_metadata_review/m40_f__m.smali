.method public m(Lm40/k0;)Ljava/lang/Object;
    .locals 1

    iget v0, p0, Lm40/f;->a:I

    packed-switch v0, :pswitch_data_0

    invoke-super {p0, p1}, Lm40/a;->m(Lm40/k0;)Ljava/lang/Object;

    move-result-object p1

    return-object p1

    :pswitch_0
    sget-object p1, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    return-object p1

    nop

    :pswitch_data_0
    .packed-switch 0x1
        :pswitch_0
    .end packed-switch
.end method
