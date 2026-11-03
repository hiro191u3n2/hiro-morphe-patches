.method public constructor <init>(Lfc0/d2;Lhc0/a;FLfz/h;Ln50/b;)V
    .locals 6

    const-string v0, "delayedExecutor"

    invoke-static {p5, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Loc0/m;->a:Lfc0/d2;

    iput-object p2, p0, Loc0/m;->b:Lhc0/a;

    iput p3, p0, Loc0/m;->c:F

    new-instance p1, Landroid/graphics/PointF;

    invoke-direct {p1}, Landroid/graphics/PointF;-><init>()V

    iput-object p1, p0, Loc0/m;->d:Landroid/graphics/PointF;

    new-instance p1, Ljava/util/ArrayList;

    invoke-direct {p1}, Ljava/util/ArrayList;-><init>()V

    iget-object p3, p2, Lhc0/a;->g:Landroidx/lifecycle/g2;

    if-eqz p3, :cond_1

    new-instance v0, Loc0/e;

    if-eqz p3, :cond_0

    iget-object p3, p3, Landroidx/lifecycle/g2;->c:Ljava/lang/Object;

    check-cast p3, Lk40/w;

    const-string v1, "getDragCriteria(...)"

    invoke-static {p3, v1}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-direct {v0, p0, p3, p5}, Loc0/e;-><init>(Loc0/m;Lk40/w;Ln50/b;)V

    invoke-virtual {p1, v0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_0

    :cond_0
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string p2, "Should only call getDragCriteria when hasDrag is true"

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    :cond_1
    :goto_0
    iget-object p3, p2, Lhc0/a;->d:Ljava/util/List;

    if-eqz p3, :cond_2

    new-instance p3, Loc0/b;

    invoke-direct {p3, p0}, Loc0/b;-><init>(Loc0/m;)V

    invoke-virtual {p1, p3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_2
    iget-object p3, p2, Lhc0/a;->c:Lhc0/k;

    if-eqz p3, :cond_4

    new-instance v0, Loc0/p;

    if-eqz p3, :cond_3

    iget p3, p3, Lhc0/k;->a:I

    int-to-long v2, p3

    move-object v1, p0

    move-object v5, p4

    move-object v4, p5

    invoke-direct/range {v0 .. v5}, Loc0/p;-><init>(Loc0/m;JLn50/b;Lfz/h;)V

    invoke-virtual {p1, v0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_1

    :cond_3
    move-object v1, p0

    new-instance p1, Ljava/lang/IllegalStateException;

    const-string p2, "Should only call getLongPressTimeout when hasLong is true"

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    :cond_4
    move-object v1, p0

    move-object v5, p4

    move-object v4, p5

    :goto_1
    iget-object p3, p2, Lhc0/a;->h:Lcom/microsoft/tokenshare/z;

    if-eqz p3, :cond_6

    new-instance p4, Loc0/r;

    if-eqz p3, :cond_5

    iget-object p3, p3, Lcom/microsoft/tokenshare/z;->a:Ljava/lang/Object;

    check-cast p3, Lhc0/l;

    invoke-direct {p4, p0, p3, v4, v5}, Loc0/r;-><init>(Loc0/m;Lhc0/l;Ln50/b;Lfz/h;)V

    invoke-virtual {p1, p4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_2

    :cond_5
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string p2, "Should only call getRepeatBehaviour when hasRepeat is true"

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    :cond_6
    :goto_2
    iget-object p3, p2, Lhc0/a;->f:Lbb0/l;

    if-eqz p3, :cond_7

    new-instance p4, Loc0/t;

    iget-object p3, p3, Lbb0/l;->a:Ljava/lang/Object;

    check-cast p3, Lhc0/p;

    invoke-direct {p4, p0, p3}, Loc0/t;-><init>(Loc0/m;Lhc0/p;)V

    invoke-virtual {p1, p4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_7
    iget-object p3, p2, Lhc0/a;->i:Lah0/a0;

    if-eqz p3, :cond_8

    new-instance p4, Loc0/i;

    iget-object p3, p3, Lah0/a0;->a:Ljava/lang/Object;

    check-cast p3, Lhc0/i;

    invoke-direct {p4, p0, p3}, Loc0/i;-><init>(Loc0/m;Lhc0/i;)V

    invoke-virtual {p1, p4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_8
    iget-object p3, p2, Lhc0/a;->e:Ljava/util/List;

    if-eqz p3, :cond_9

    new-instance p3, Loc0/v;

    invoke-direct {p3, p0}, Loc0/v;-><init>(Loc0/m;)V

    invoke-virtual {p1, p3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_9
    iput-object p1, v1, Loc0/m;->f:Ljava/util/ArrayList;

    iget-object p1, p2, Lhc0/a;->j:Loc0/j;

    const-string p2, "getInterceptor(...)"

    invoke-static {p1, p2}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    iput-object p1, v1, Loc0/m;->g:Loc0/j;

    return-void
.end method
