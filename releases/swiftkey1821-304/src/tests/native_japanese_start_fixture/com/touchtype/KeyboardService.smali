.class public final Lcom/touchtype/KeyboardService;
.super Lcom/touchtype/Hilt_KeyboardService;
.implements Landroidx/lifecycle/q0;
.implements Lxa/g;

.method public final onStartInputView(Landroid/view/inputmethod/EditorInfo;Z)V
    .locals 1

    const-string v0, "attribute"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    iget-object v0, p0, Lcom/touchtype/KeyboardService;->j:Li70/x3;

    if-eqz v0, :cond_0

    invoke-virtual {v0, p1, p2}, Li70/x3;->m(Landroid/view/inputmethod/EditorInfo;Z)V

    return-void

    :cond_0
    const-string p1, "delegate"

    invoke-static {p1}, Lkotlin/jvm/internal/m;->l(Ljava/lang/String;)V

    const/4 p1, 0x0

    throw p1
.end method

.method public final onFinishInputView(Z)V
    .locals 1

    iget-object v0, p0, Lcom/touchtype/KeyboardService;->j:Li70/x3;

    if-eqz v0, :cond_0

    invoke-virtual {v0, p1}, Li70/x3;->b(Z)V

    return-void

    :cond_0
    const-string p1, "delegate"

    invoke-static {p1}, Lkotlin/jvm/internal/m;->l(Ljava/lang/String;)V

    const/4 p1, 0x0

    throw p1
.end method

.method public final onWindowHidden()V
    .locals 1

    iget-object v0, p0, Lcom/touchtype/KeyboardService;->j:Li70/x3;

    if-eqz v0, :cond_0

    invoke-virtual {v0}, Li70/x3;->d()V

    return-void

    :cond_0
    const-string v0, "delegate"

    invoke-static {v0}, Lkotlin/jvm/internal/m;->l(Ljava/lang/String;)V

    const/4 v0, 0x0

    throw v0
.end method

.method public final onStartInput(Landroid/view/inputmethod/EditorInfo;Z)V
    .locals 1

    const-string v0, "attribute"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    iput-object p1, p0, Lcom/touchtype/KeyboardService;->i:Landroid/view/inputmethod/EditorInfo;

    iget-object v0, p0, Lcom/touchtype/KeyboardService;->j:Li70/x3;

    if-eqz v0, :cond_0

    invoke-virtual {v0, p1, p2}, Li70/x3;->h(Landroid/view/inputmethod/EditorInfo;Z)V

    return-void

    :cond_0
    const-string p1, "delegate"

    invoke-static {p1}, Lkotlin/jvm/internal/m;->l(Ljava/lang/String;)V

    const/4 p1, 0x0

    throw p1
.end method

.method public final onFinishInput()V
    .locals 1

    iget-object v0, p0, Lcom/touchtype/KeyboardService;->j:Li70/x3;

    if-eqz v0, :cond_0

    invoke-virtual {v0}, Li70/x3;->u()V

    return-void

    :cond_0
    const-string v0, "delegate"

    invoke-static {v0}, Lkotlin/jvm/internal/m;->l(Ljava/lang/String;)V

    const/4 v0, 0x0

    throw v0
.end method
