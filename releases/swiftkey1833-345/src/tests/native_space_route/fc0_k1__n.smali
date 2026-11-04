.method public final n(Lhc0/b;Lcm0/d;Lfc0/g2;Lfc0/m1;)V
    .locals 9

    new-instance v0, Lfc0/d1;

    const-string v1, " "

    const/4 v2, 0x0

    invoke-direct {v0, p0, v1, v1, v2}, Lfc0/d1;-><init>(Lfc0/k1;Ljava/lang/String;Ljava/lang/String;Z)V

    new-instance v1, Lfc0/q0;

    invoke-direct {v1, p0, v2}, Lfc0/q0;-><init>(Lfc0/k1;I)V

    new-instance v3, Lic0/g;

    const/16 v4, 0x20

    invoke-direct {v3, p3, v4}, Lic0/g;-><init>(Lfc0/d2;I)V

    const/4 p3, 0x1

    new-array v4, p3, [Lic0/a;

    aput-object v3, v4, v2

    invoke-virtual {p1, p2, v4}, Lhc0/b;->l(Lcm0/d;[Lic0/a;)V

    iget-object v3, p0, Lfc0/k1;->h:Lfz/h;

    invoke-virtual {v3}, Lfz/h;->b()Z

    move-result v4

    const/4 v5, 0x3

    const/4 v6, 0x2

    sget-object v7, Loz/n4;->b:Loz/n4;

    if-eqz v4, :cond_0

    new-instance p4, Lfc0/y;

    invoke-direct {p4}, Ljava/lang/Object;-><init>()V

    sget-object v3, Lv20/c;->h:Lv20/c;

    new-instance v4, Lfc0/n;

    invoke-direct {v4, v3, p0, v7}, Lfc0/n;-><init>(Lv20/c;Lfc0/k1;Loz/n4;)V

    const/4 v3, 0x4

    new-array v3, v3, [Lic0/a;

    aput-object p4, v3, v2

    aput-object v1, v3, p3

    aput-object v0, v3, v6

    aput-object v4, v3, v5

    invoke-virtual {p1, p2, v3}, Lhc0/b;->f(Lcm0/d;[Lic0/a;)V

    return-void

    :cond_0
    new-instance v4, Lfc0/y;

    invoke-direct {v4}, Ljava/lang/Object;-><init>()V

    new-array v8, p3, [Lic0/a;

    aput-object v4, v8, v2

    invoke-virtual {p1, p2, v8}, Lhc0/b;->i(Lcm0/d;[Lic0/a;)V

    invoke-virtual {v3}, Lfz/h;->b()Z

    move-result v3

    if-eqz v3, :cond_1

    sget-object v3, Lv20/c;->h:Lv20/c;

    new-instance v4, Lfc0/n;

    invoke-direct {v4, v3, p0, v7}, Lfc0/n;-><init>(Lv20/c;Lfc0/k1;Loz/n4;)V

    new-array v7, v5, [Lic0/a;

    aput-object v1, v7, v2

    aput-object v0, v7, p3

    aput-object v4, v7, v6

    invoke-virtual {p1, p2, v7}, Lhc0/b;->i(Lcm0/d;[Lic0/a;)V

    new-instance v4, Lfc0/n;

    sget-object v7, Loz/n4;->e:Loz/n4;

    invoke-direct {v4, v3, p0, v7}, Lfc0/n;-><init>(Lv20/c;Lfc0/k1;Loz/n4;)V

    new-array v3, v5, [Lic0/a;

    aput-object v1, v3, v2

    aput-object v0, v3, p3

    aput-object v4, v3, v6

    invoke-virtual {p1, p2, v3}, Lhc0/b;->y(Lcm0/d;[Lic0/a;)V

    goto :goto_0

    :cond_1
    new-array v3, p3, [Lic0/a;

    aput-object v1, v3, v2

    invoke-virtual {p1, p2, v3}, Lhc0/b;->x(Lcm0/d;[Lic0/a;)V

    sget-object v1, Lv20/c;->h:Lv20/c;

    new-instance v3, Lfc0/n;

    invoke-direct {v3, v1, p0, v7}, Lfc0/n;-><init>(Lv20/c;Lfc0/k1;Loz/n4;)V

    new-array v4, v6, [Lic0/a;

    aput-object v0, v4, v2

    aput-object v3, v4, p3

    invoke-virtual {p1, p2, v4}, Lhc0/b;->i(Lcm0/d;[Lic0/a;)V

    new-instance v3, Le2/h0;

    invoke-direct {v3, p0, v5}, Le2/h0;-><init>(Ljava/lang/Object;I)V

    new-instance v4, Lfc0/n;

    sget-object v5, Loz/n4;->f:Loz/n4;

    invoke-direct {v4, v1, p0, v5}, Lfc0/n;-><init>(Lv20/c;Lfc0/k1;Loz/n4;)V

    new-array v1, v6, [Lic0/a;

    aput-object v0, v1, v2

    aput-object v4, v1, p3

    invoke-virtual {p1, v3, v1}, Lhc0/b;->y(Lcm0/d;[Lic0/a;)V

    :goto_0
    invoke-virtual {p0, p1, p2, p4, v2}, Lfc0/k1;->f(Lhc0/b;Lcm0/d;Lfc0/m1;Z)V

    return-void
.end method
