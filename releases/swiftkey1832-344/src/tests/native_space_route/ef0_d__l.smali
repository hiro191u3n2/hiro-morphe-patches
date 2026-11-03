.method public l(ILfc0/f;Lkj0/f;)V
    .locals 8

    iget-object v0, p3, Lkj0/f;->c:Ljava/lang/Object;

    check-cast v0, Landroid/view/MotionEvent;

    invoke-virtual {v0}, Landroid/view/MotionEvent;->getActionIndex()I

    move-result v0

    iget-object v1, p3, Lkj0/f;->c:Ljava/lang/Object;

    check-cast v1, Landroid/view/MotionEvent;

    invoke-virtual {v1}, Landroid/view/MotionEvent;->getActionMasked()I

    move-result v1

    iget-object v2, p3, Lkj0/f;->c:Ljava/lang/Object;

    check-cast v2, Landroid/view/MotionEvent;

    invoke-virtual {v2, p1}, Landroid/view/MotionEvent;->getPointerId(I)I

    move-result v2

    new-instance v3, Lef0/p;

    invoke-direct {v3, p3, p1}, Lef0/p;-><init>(Lkj0/f;I)V

    iget-object v4, p3, Lkj0/f;->b:Ljava/lang/Object;

    check-cast v4, Llh0/a;

    const/4 v5, 0x3

    if-ne v1, v5, :cond_1

    iget-object p1, p0, Lef0/d;->a:Ljava/lang/Object;

    check-cast p1, Ltj0/e;

    iget-object p2, p1, Ltj0/e;->c:Ljava/lang/Object;

    check-cast p2, Ljava/util/HashMap;

    invoke-virtual {p2}, Ljava/util/HashMap;->keySet()Ljava/util/Set;

    move-result-object p2

    invoke-interface {p2}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object p2

    :goto_0
    invoke-interface {p2}, Ljava/util/Iterator;->hasNext()Z

    move-result p3

    if-eqz p3, :cond_0

    invoke-interface {p2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object p3

    check-cast p3, Ljava/lang/Integer;

    invoke-virtual {p3}, Ljava/lang/Integer;->intValue()I

    move-result p3

    invoke-virtual {p1, v4, p3}, Ltj0/e;->E(Llh0/a;I)V

    goto :goto_0

    :cond_0
    iget-object p1, p0, Lef0/d;->a:Ljava/lang/Object;

    check-cast p1, Ltj0/e;

    invoke-virtual {p1, v4, v2}, Ltj0/e;->D(Llh0/a;I)V

    iget-object p1, p0, Lef0/d;->d:Ljava/lang/Object;

    check-cast p1, Lef0/n;

    invoke-static {v4}, Lkotlin/jvm/internal/m;->c(Ljava/lang/Object;)V

    invoke-interface {p1, v4}, Lef0/n;->f(Llh0/a;)V

    iget-object p1, p0, Lef0/d;->e:Ljava/lang/Object;

    check-cast p1, Lh60/d;

    iget-object p1, p1, Lh60/d;->b:Ljava/lang/Object;

    check-cast p1, Lfz/h;

    invoke-virtual {p1}, Lfz/h;->b()Z

    move-result p1

    if-eqz p1, :cond_13

    iget-object p1, p0, Lef0/d;->e:Ljava/lang/Object;

    check-cast p1, Lh60/d;

    invoke-virtual {p1, v2}, Lh60/d;->c(I)V

    return-void

    :cond_1
    if-eq p1, v0, :cond_2

    iget-object p1, p0, Lef0/d;->a:Ljava/lang/Object;

    check-cast p1, Ltj0/e;

    invoke-virtual {p1, p2, v3, v2}, Ltj0/e;->U(Lfc0/f;Lef0/p;I)V

    return-void

    :cond_2
    iget-object p1, p0, Lef0/d;->e:Ljava/lang/Object;

    check-cast p1, Lh60/d;

    iget-object p1, p1, Lh60/d;->b:Ljava/lang/Object;

    check-cast p1, Lfz/h;

    invoke-virtual {p1}, Lfz/h;->b()Z

    move-result p1

    const/4 v0, 0x5

    const/4 v5, 0x0

    if-eqz p1, :cond_7

    if-eqz v1, :cond_3

    if-eq v1, v0, :cond_3

    goto :goto_1

    :cond_3
    iget-object p1, p0, Lef0/d;->e:Ljava/lang/Object;

    check-cast p1, Lh60/d;

    iget-object v6, p1, Lh60/d;->b:Ljava/lang/Object;

    check-cast v6, Lfz/h;

    invoke-virtual {v6}, Lfz/h;->b()Z

    move-result v6

    if-eqz v6, :cond_7

    if-nez p2, :cond_4

    goto :goto_1

    :cond_4
    iget-object v6, p1, Lh60/d;->c:Ljava/lang/Object;

    check-cast v6, Lwm0/x1;

    invoke-virtual {v6}, Lwm0/x1;->getValue()Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Lef0/k;

    instance-of v7, v6, Lef0/m;

    if-eqz v7, :cond_5

    check-cast v6, Lef0/m;

    iget v7, v6, Lef0/m;->a:I

    if-ne v7, v2, :cond_5

    iget-object v6, v6, Lef0/m;->b:Lfc0/f;

    if-ne v6, p2, :cond_5

    goto :goto_1

    :cond_5
    invoke-virtual {p1, v2}, Lh60/d;->c(I)V

    move-object v6, p2

    check-cast v6, Lfc0/n2;

    iput-boolean v5, v6, Lfc0/n2;->f:Z

    iget-object p1, p1, Lh60/d;->c:Ljava/lang/Object;

    check-cast p1, Lwm0/x1;

    :cond_6
    invoke-virtual {p1}, Lwm0/x1;->getValue()Ljava/lang/Object;

    move-result-object v6

    move-object v7, v6

    check-cast v7, Lef0/k;

    new-instance v7, Lef0/m;

    invoke-direct {v7, v2, p2}, Lef0/m;-><init>(ILfc0/f;)V

    invoke-virtual {p1, v6, v7}, Lwm0/x1;->h(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v6

    if-eqz v6, :cond_6

    :cond_7
    :goto_1
    if-eqz v1, :cond_14

    const/4 p1, 0x1

    if-eq v1, p1, :cond_11

    const/4 v6, 0x2

    if-eq v1, v6, :cond_d

    if-eq v1, v0, :cond_14

    const/4 p1, 0x6

    if-eq v1, p1, :cond_11

    const/4 p1, 0x7

    const-string v0, "mMotionEvent"

    if-eq v1, p1, :cond_c

    const/16 p1, 0x9

    if-eq v1, p1, :cond_b

    const/16 p1, 0xa

    if-eq v1, p1, :cond_8

    goto/16 :goto_4

    :cond_8
    iget-object p1, p0, Lef0/d;->b:Ljava/lang/Object;

    check-cast p1, Lfz/h;

    invoke-virtual {p1}, Lfz/h;->b()Z

    move-result p1

    if-eqz p1, :cond_a

    if-eqz p2, :cond_a

    iget-object p1, p0, Lef0/d;->a:Ljava/lang/Object;

    check-cast p1, Ltj0/e;

    invoke-virtual {p1, p2, v3, v2}, Ltj0/e;->U(Lfc0/f;Lef0/p;I)V

    move-object p1, p2

    check-cast p1, Lfc0/n2;

    iget-boolean v1, p1, Lfc0/n2;->f:Z

    if-eqz v1, :cond_9

    iget-object v1, p0, Lef0/d;->a:Ljava/lang/Object;

    check-cast v1, Ltj0/e;

    invoke-virtual {v1, v4, v2}, Ltj0/e;->D(Llh0/a;I)V

    iput-boolean v5, p1, Lfc0/n2;->f:Z

    goto :goto_2

    :cond_9
    iget-object p1, p0, Lef0/d;->a:Ljava/lang/Object;

    check-cast p1, Ltj0/e;

    invoke-virtual {p1, p2, v3, v2}, Ltj0/e;->Q(Lfc0/f;Lef0/p;I)V

    :goto_2
    iget-object p1, p0, Lef0/d;->c:Ljava/lang/Object;

    check-cast p1, Lyd0/c;

    iget-object p3, p3, Lkj0/f;->c:Ljava/lang/Object;

    check-cast p3, Landroid/view/MotionEvent;

    invoke-static {p3, v0}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-interface {p1, p2, p3}, Lyd0/c;->a(Lfc0/f;Landroid/view/MotionEvent;)V

    :cond_a
    iget-object p1, p0, Lef0/d;->e:Ljava/lang/Object;

    check-cast p1, Lh60/d;

    iget-object p1, p1, Lh60/d;->b:Ljava/lang/Object;

    check-cast p1, Lfz/h;

    invoke-virtual {p1}, Lfz/h;->b()Z

    move-result p1

    if-eqz p1, :cond_13

    iget-object p1, p0, Lef0/d;->e:Ljava/lang/Object;

    check-cast p1, Lh60/d;

    invoke-virtual {p1, v2}, Lh60/d;->c(I)V

    return-void

    :cond_b
    iget-object p1, p0, Lef0/d;->b:Ljava/lang/Object;

    check-cast p1, Lfz/h;

    invoke-virtual {p1}, Lfz/h;->b()Z

    move-result p1

    if-eqz p1, :cond_13

    if-eqz p2, :cond_13

    iget-object p1, p0, Lef0/d;->a:Ljava/lang/Object;

    check-cast p1, Ltj0/e;

    invoke-virtual {p1, p2, v3, v2}, Ltj0/e;->G(Lfc0/f;Lef0/p;I)V

    iget-object p1, p0, Lef0/d;->c:Ljava/lang/Object;

    check-cast p1, Lyd0/c;

    iget-object p3, p3, Lkj0/f;->c:Ljava/lang/Object;

    check-cast p3, Landroid/view/MotionEvent;

    invoke-static {p3, v0}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-interface {p1, p2, p3}, Lyd0/c;->a(Lfc0/f;Landroid/view/MotionEvent;)V

    return-void

    :cond_c
    iget-object p1, p0, Lef0/d;->b:Ljava/lang/Object;

    check-cast p1, Lfz/h;

    invoke-virtual {p1}, Lfz/h;->b()Z

    move-result p1

    if-eqz p1, :cond_13

    if-eqz p2, :cond_13

    invoke-static {v4}, Lkotlin/jvm/internal/m;->c(Ljava/lang/Object;)V

    invoke-virtual {p0, v2, p2, v4, v5}, Lef0/d;->b(ILfc0/f;Ljava/lang/Object;Z)Z

    iget-object p1, p0, Lef0/d;->a:Ljava/lang/Object;

    check-cast p1, Ltj0/e;

    invoke-virtual {p1, p2, v3, v2}, Ltj0/e;->U(Lfc0/f;Lef0/p;I)V

    iget-object p1, p0, Lef0/d;->c:Ljava/lang/Object;

    check-cast p1, Lyd0/c;

    iget-object p3, p3, Lkj0/f;->c:Ljava/lang/Object;

    check-cast p3, Landroid/view/MotionEvent;

    invoke-static {p3, v0}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-interface {p1, p2, p3}, Lyd0/c;->a(Lfc0/f;Landroid/view/MotionEvent;)V

    return-void

    :cond_d
    invoke-static {v4}, Lkotlin/jvm/internal/m;->c(Ljava/lang/Object;)V

    invoke-virtual {p0, v2, p2, v4, p1}, Lef0/d;->b(ILfc0/f;Ljava/lang/Object;Z)Z

    move-result p1

    if-eqz p1, :cond_e

    iget-object p1, p0, Lef0/d;->a:Ljava/lang/Object;

    check-cast p1, Ltj0/e;

    invoke-virtual {p1, p2, v3, v2}, Ltj0/e;->U(Lfc0/f;Lef0/p;I)V

    :cond_e
    iget-object p1, p0, Lef0/d;->e:Ljava/lang/Object;

    check-cast p1, Lh60/d;

    iget-object p1, p1, Lh60/d;->b:Ljava/lang/Object;

    check-cast p1, Lfz/h;

    invoke-virtual {p1}, Lfz/h;->b()Z

    move-result p1

    if-eqz p1, :cond_13

    iget-object p1, p0, Lef0/d;->e:Ljava/lang/Object;

    check-cast p1, Lh60/d;

    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    iget-object p3, p1, Lh60/d;->c:Ljava/lang/Object;

    check-cast p3, Lwm0/x1;

    invoke-virtual {p3}, Lwm0/x1;->getValue()Ljava/lang/Object;

    move-result-object p3

    check-cast p3, Lef0/k;

    instance-of v0, p3, Lef0/j;

    if-eqz v0, :cond_f

    goto :goto_4

    :cond_f
    instance-of v0, p3, Lef0/m;

    if-eqz v0, :cond_10

    check-cast p3, Lef0/m;

    iget v0, p3, Lef0/m;->a:I

    if-ne v0, v2, :cond_13

    iget-object p3, p3, Lef0/m;->b:Lfc0/f;

    if-eq p3, p2, :cond_13

    invoke-virtual {p1, v2}, Lh60/d;->c(I)V

    return-void

    :cond_10
    new-instance p1, Lnl0/k;

    invoke-direct {p1}, Ljava/lang/RuntimeException;-><init>()V

    throw p1

    :cond_11
    iget-object p1, p0, Lef0/d;->a:Ljava/lang/Object;

    check-cast p1, Ltj0/e;

    invoke-virtual {p1, p2, v3, v2}, Ltj0/e;->U(Lfc0/f;Lef0/p;I)V

    if-eqz p2, :cond_12

    check-cast p2, Lfc0/n2;

    iget-boolean p1, p2, Lfc0/n2;->f:Z

    if-eqz p1, :cond_12

    iget-object p1, p0, Lef0/d;->a:Ljava/lang/Object;

    check-cast p1, Ltj0/e;

    invoke-virtual {p1, v4, v2}, Ltj0/e;->D(Llh0/a;I)V

    iput-boolean v5, p2, Lfc0/n2;->f:Z

    goto :goto_3

    :cond_12
    iget-object p1, p0, Lef0/d;->a:Ljava/lang/Object;

    check-cast p1, Ltj0/e;

    invoke-virtual {p1, v3, v2}, Ltj0/e;->Z(Lef0/p;I)V

    :goto_3
    iget-object p1, p0, Lef0/d;->d:Ljava/lang/Object;

    check-cast p1, Lef0/n;

    invoke-static {v4}, Lkotlin/jvm/internal/m;->c(Ljava/lang/Object;)V

    invoke-interface {p1, v4}, Lef0/n;->f(Llh0/a;)V

    iget-object p1, p0, Lef0/d;->e:Ljava/lang/Object;

    check-cast p1, Lh60/d;

    iget-object p1, p1, Lh60/d;->b:Ljava/lang/Object;

    check-cast p1, Lfz/h;

    invoke-virtual {p1}, Lfz/h;->b()Z

    move-result p1

    if-eqz p1, :cond_13

    iget-object p1, p0, Lef0/d;->e:Ljava/lang/Object;

    check-cast p1, Lh60/d;

    invoke-virtual {p1, v2}, Lh60/d;->c(I)V

    :cond_13
    :goto_4
    return-void

    :cond_14
    iget-object p1, p0, Lef0/d;->a:Ljava/lang/Object;

    check-cast p1, Ltj0/e;

    invoke-virtual {p1, p2, v3, v2}, Ltj0/e;->G(Lfc0/f;Lef0/p;I)V

    return-void
.end method
