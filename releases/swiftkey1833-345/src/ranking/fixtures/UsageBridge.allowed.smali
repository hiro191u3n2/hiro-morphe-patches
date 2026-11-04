.method private static allowed(Lhiro/swiftkey/UsageBridge$Session;Lwb0/n0;)Z
    .locals 3

    const/4 v0, 0x0

    if-eqz p0, :cond_7

    sget-object v1, Lhiro/swiftkey/UsageBridge;->session:Lhiro/swiftkey/UsageBridge$Session;

    if-ne p0, v1, :cond_7

    iget-boolean v1, p0, Lhiro/swiftkey/UsageBridge$Session;->editorAllowed:Z

    if-nez v1, :cond_0

    goto :goto_2

    :cond_0
    iget-object v1, p0, Lhiro/swiftkey/UsageBridge$Session;->owner:Ljava/lang/ref/WeakReference;

    invoke-virtual {v1}, Ljava/lang/ref/WeakReference;->get()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lwb0/x;

    if-eqz v1, :cond_6

    iget-object v2, v1, Lwb0/x;->c:Lwb0/n0;

    if-eqz v2, :cond_6

    if-eqz p1, :cond_1

    iget-object v2, v1, Lwb0/x;->c:Lwb0/n0;

    if-ne v2, p1, :cond_6

    :cond_1
    iget-object p1, v1, Lwb0/x;->i:Lcom/microsoft/tokenshare/m;

    if-eqz p1, :cond_6

    iget-object p1, v1, Lwb0/x;->i:Lcom/microsoft/tokenshare/m;

    iget-object p1, p1, Lcom/microsoft/tokenshare/m;->b:Ljava/lang/Object;

    instance-of p1, p1, Lvb0/b;

    if-nez p1, :cond_2

    goto :goto_1

    :cond_2
    iget-object p1, v1, Lwb0/x;->i:Lcom/microsoft/tokenshare/m;

    iget-object p1, p1, Lcom/microsoft/tokenshare/m;->b:Ljava/lang/Object;

    check-cast p1, Lvb0/b;

    iget-object p1, p1, Lvb0/b;->b:Lvb0/c;

    if-eqz p1, :cond_5

    invoke-virtual {p1}, Lvb0/c;->b()Z

    move-result p1

    if-nez p1, :cond_5

    iget-object p1, v1, Lwb0/x;->e:Lwb0/z0;

    if-eqz p1, :cond_5

    iget-object p1, v1, Lwb0/x;->e:Lwb0/z0;

    iget-object p1, p1, Lwb0/z0;->c:Lah0/w0;

    if-eqz p1, :cond_5

    iget-object p1, v1, Lwb0/x;->e:Lwb0/z0;

    iget-object p1, p1, Lwb0/z0;->c:Lah0/w0;

    invoke-virtual {p1}, Lah0/w0;->u0()Z

    move-result p1

    if-eqz p1, :cond_3

    goto :goto_0

    :cond_3
    sget-object p1, Lhiro/swiftkey/UsageBridge;->session:Lhiro/swiftkey/UsageBridge$Session;

    if-ne p0, p1, :cond_4

    const/4 v0, 0x1

    :cond_4
    return v0

    :cond_5
    :goto_0
    return v0

    :cond_6
    :goto_1
    return v0

    :cond_7
    :goto_2
    return v0
.end method
