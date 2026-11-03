.method public final g(Lcom/touchtype_fluency/service/s0;)Ljava/lang/Object;
    .locals 14

    sget-object v0, Lol0/y;->a:Lol0/y;

    const-string v1, "predictor"

    invoke-static {p1, v1}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    iget-boolean v1, p0, Lib0/r;->a:Z

    if-eqz v1, :cond_0

    iget-object v1, p0, Lib0/r;->b:Lcb0/p;

    iget-object v1, v1, Lcb0/p;->a:Ljava/lang/Object;

    check-cast v1, Ljava/util/HashMap;

    invoke-virtual {v1}, Ljava/util/HashMap;->clear()V

    :cond_0
    iget-object v1, p1, Lcom/touchtype_fluency/service/s0;->a:Lcom/touchtype_fluency/service/l;

    iget-object v1, v1, Lcom/touchtype_fluency/service/l;->q:Lk40/l;

    sget-object v2, Lk40/l;->a:Lk40/l;

    if-ne v1, v2, :cond_1

    iget-object p1, p0, Lib0/r;->b:Lcb0/p;

    iget-object p1, p1, Lcb0/p;->a:Ljava/lang/Object;

    check-cast p1, Ljava/util/HashMap;

    invoke-virtual {p1}, Ljava/util/HashMap;->clear()V

    return-object v0

    :cond_1
    iget-object v1, p0, Lib0/r;->g:Ldt/p;

    iget-object v1, v1, Ldt/p;->d:Ljava/lang/Object;

    check-cast v1, Ljava/util/Map;

    new-instance v2, Ljava/util/HashMap;

    invoke-direct {v2}, Ljava/util/HashMap;-><init>()V

    iget-object v3, p0, Lib0/r;->b:Lcb0/p;

    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    invoke-interface {v1}, Ljava/util/Map;->entrySet()Ljava/util/Set;

    move-result-object v4

    invoke-interface {v4}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v4

    :cond_2
    :goto_0
    invoke-interface {v4}, Ljava/util/Iterator;->hasNext()Z

    move-result v5

    if-eqz v5, :cond_3

    invoke-interface {v4}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Ljava/util/Map$Entry;

    iget-object v6, v3, Lcb0/p;->a:Ljava/lang/Object;

    check-cast v6, Ljava/util/HashMap;

    invoke-interface {v5}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v7

    invoke-virtual {v6, v7}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Lib0/n;

    if-eqz v6, :cond_2

    invoke-interface {v5}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v7

    check-cast v7, Ll40/c;

    iget-object v8, v6, Lib0/n;->b:Ll40/c;

    invoke-static {v8, v7}, Ljava/util/Objects;->equals(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v7

    if-eqz v7, :cond_2

    invoke-interface {v5}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Ll40/d;

    iget-object v6, v6, Lib0/n;->a:Ljava/util/List;

    invoke-virtual {v2, v5, v6}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    goto :goto_0

    :cond_3
    invoke-interface {v1}, Ljava/util/Map;->entrySet()Ljava/util/Set;

    move-result-object v3

    invoke-interface {v3}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v3

    const/4 v4, 0x0

    move v5, v4

    :cond_4
    :goto_1
    invoke-interface {v3}, Ljava/util/Iterator;->hasNext()Z

    move-result v6

    if-eqz v6, :cond_7

    invoke-interface {v3}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Ljava/util/Map$Entry;

    invoke-interface {v6}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v7

    check-cast v7, Ll40/d;

    invoke-interface {v6}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Ll40/c;

    invoke-virtual {v2, v7}, Ljava/util/HashMap;->containsKey(Ljava/lang/Object;)Z

    move-result v8

    if-nez v8, :cond_4

    invoke-static {v6}, Lkotlin/jvm/internal/m;->c(Ljava/lang/Object;)V

    const-string v5, "error"

    const-string v8, "UpdateCandidatesCallable"

    :try_start_0
    invoke-virtual {p0, p1, v6}, Lib0/r;->a(Lcom/touchtype_fluency/service/s0;Ll40/c;)Lcom/microsoft/fluency/Predictions;

    move-result-object v9

    iget-object v10, p0, Lib0/r;->g:Ldt/p;

    iget-object v10, v10, Ldt/p;->a:Ljava/lang/Object;

    check-cast v10, Lib0/m;

    sget-object v11, Lib0/m;->d:Lib0/m;

    if-eq v10, v11, :cond_5

    sget-object v11, Lib0/m;->h:Lib0/m;

    if-ne v10, v11, :cond_6

    goto :goto_2

    :catch_0
    move-exception v6

    goto :goto_3

    :catch_1
    move-exception v6

    goto :goto_4

    :cond_5
    :goto_2
    invoke-interface {v9}, Ljava/util/Collection;->isEmpty()Z

    move-result v10

    if-nez v10, :cond_6

    invoke-interface {v9, v4}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v10

    check-cast v10, Lcom/microsoft/fluency/Prediction;

    invoke-virtual {v10}, Lcom/microsoft/fluency/Prediction;->getProbability()D

    move-result-wide v10

    const-wide/16 v12, 0x0

    cmpg-double v10, v10, v12

    if-nez v10, :cond_6

    move-object v9, v0

    :cond_6
    iget-object v10, p0, Lib0/r;->c:Lm40/c;

    invoke-static {v9, v6, v10}, Lm40/r;->d(Ljava/util/List;Ll40/c;Lm40/c;)Ljava/util/ArrayList;

    move-result-object v5
    :try_end_0
    .catch Lk40/x; {:try_start_0 .. :try_end_0} :catch_1
    .catch Lcom/microsoft/fluency/ParameterOutOfRangeException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_6

    :goto_3
    invoke-static {v8, v5, v6}, Lb20/d;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V

    goto :goto_5

    :goto_4
    invoke-static {v8, v5, v6}, Lb20/d;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V

    :goto_5
    move-object v5, v0

    :goto_6
    invoke-virtual {v2, v7, v5}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    const/4 v5, 0x1

    goto :goto_1

    :cond_7
    iget-object p1, p0, Lib0/r;->h:Lmb0/c;

    iget-object v0, p0, Lib0/r;->g:Ldt/p;

    iget-object v3, v0, Ldt/p;->e:Ljava/lang/Object;

    check-cast v3, Lib0/b;

    iget-object v0, v0, Ldt/p;->f:Ljava/lang/Object;

    check-cast v0, Lmb0/f1;

    invoke-interface {p1, v3, v0}, Lmb0/c;->a(Lib0/b;Lmb0/f1;)Lmb0/b;

    move-result-object p1

    sget-object v0, Ll40/d;->a:Ll40/d;

    invoke-virtual {v2, v0}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/util/List;

    if-nez v0, :cond_8

    sget-object p1, Ljava/util/Collections;->EMPTY_LIST:Ljava/util/List;

    goto :goto_7

    :cond_8
    invoke-interface {p1, v0}, Lmb0/b;->a(Ljava/util/List;)Ljava/util/List;

    move-result-object p1

    :goto_7
    if-eqz v5, :cond_a

    invoke-static {p1}, Lkotlin/jvm/internal/m;->c(Ljava/lang/Object;)V

    iget-object v3, p0, Lib0/r;->g:Ldt/p;

    iget-object v3, v3, Ldt/p;->d:Ljava/lang/Object;

    check-cast v3, Ljava/util/Map;

    sget-object v4, Ll40/d;->a:Ll40/d;

    invoke-interface {v3, v4}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ll40/c;

    invoke-static {p1, v3}, Lhiro/swiftkey/WordCandidates;->enhance(Ljava/util/List;Ll40/c;)Ljava/util/List;

    move-result-object p1

    invoke-static {p1, v3}, Lhiro/swiftkey/EmojiCandidates;->enhance(Ljava/util/List;Ll40/c;)Ljava/util/List;

    move-result-object p1

    iget-object v0, p0, Lib0/r;->e:Ln50/a;

    new-instance v3, Landroidx/lifecycle/n;

    const/4 v4, 0x2

    invoke-direct {v3, v4, p0, p1}, Landroidx/lifecycle/n;-><init>(ILjava/lang/Object;Ljava/lang/Object;)V

    invoke-virtual {v0, v3}, Ln50/a;->execute(Ljava/lang/Runnable;)V

    iget-object v0, p0, Lib0/r;->b:Lcb0/p;

    iget-object v0, v0, Lcb0/p;->a:Ljava/lang/Object;

    check-cast v0, Ljava/util/HashMap;

    invoke-virtual {v0}, Ljava/util/HashMap;->clear()V

    invoke-interface {v1}, Ljava/util/Map;->entrySet()Ljava/util/Set;

    move-result-object v1

    invoke-interface {v1}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :goto_8
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v3

    if-eqz v3, :cond_a

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/util/Map$Entry;

    invoke-interface {v3}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ll40/d;

    invoke-interface {v3}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ll40/c;

    invoke-virtual {v2, v4}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Ljava/util/List;

    if-eqz v5, :cond_9

    new-instance v6, Lib0/n;

    invoke-direct {v6, v3, v5}, Lib0/n;-><init>(Ll40/c;Ljava/util/List;)V

    invoke-virtual {v0, v4, v6}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    goto :goto_8

    :cond_9
    new-instance p1, Ljava/lang/IllegalArgumentException;

    const-string v0, "Candidates must be supplied for all subrequests"

    invoke-direct {p1, v0}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    throw p1

    :cond_a
    invoke-static {p1}, Lkotlin/jvm/internal/m;->c(Ljava/lang/Object;)V

    return-object p1
.end method
