.method public static rank(Ljava/util/List;Ll40/c;Z)Ljava/util/List;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List<",
            "Lm40/b;",
            ">;",
            "Ll40/c;",
            "Z)",
            "Ljava/util/List<",
            "Lm40/b;",
            ">;"
        }
    .end annotation

    :try_start_0
    sget-object v0, Lhiro/swiftkey/FrequencyLearning;->store:Lhiro/swiftkey/FrequencyLearning$Store;

    if-eqz p0, :cond_d

    invoke-interface {p0}, Ljava/util/List;->isEmpty()Z

    move-result v1

    if-nez v1, :cond_d

    if-eqz p1, :cond_d

    iget-boolean v1, p1, Ll40/c;->f:Z

    if-nez v1, :cond_d

    iget-object v1, p1, Ll40/c;->d:Lcom/microsoft/fluency/ResultsFilter$PredictionSearchType;

    sget-object v2, Lcom/microsoft/fluency/ResultsFilter$PredictionSearchType;->JAPANESE:Lcom/microsoft/fluency/ResultsFilter$PredictionSearchType;

    if-eq v1, v2, :cond_0

    goto/16 :goto_4

    :cond_0
    iget-object v1, p1, Ll40/c;->k:Ljava/lang/String;

    invoke-static {v1}, Lhiro/swiftkey/FrequencyLearning;->reading(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    if-nez v1, :cond_1

    return-object p0

    :cond_1
    invoke-static {p0, p1, v1}, Lhiro/swiftkey/KatakanaGlossCleanup;->cleanCandidates(Ljava/util/List;Ll40/c;Ljava/lang/String;)Ljava/util/List;

    move-result-object p0

    if-eqz p2, :cond_c

    if-eqz v0, :cond_c

    invoke-interface {p0}, Ljava/util/List;->isEmpty()Z

    move-result p2

    if-eqz p2, :cond_2

    goto/16 :goto_3

    :cond_2
    new-instance p2, Ljava/util/LinkedHashMap;

    invoke-direct {p2}, Ljava/util/LinkedHashMap;-><init>()V

    invoke-interface {p0}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v2

    :goto_0
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    move-result v3

    if-eqz v3, :cond_5

    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Lm40/b;

    if-nez v3, :cond_3

    goto :goto_0

    :cond_3
    invoke-interface {v3}, Lm40/b;->getUserFacingText()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {p2, v4}, Ljava/util/LinkedHashMap;->containsKey(Ljava/lang/Object;)Z

    move-result v5

    if-nez v5, :cond_4

    invoke-virtual {p2, v4, v3}, Ljava/util/LinkedHashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    :cond_4
    goto :goto_0

    :cond_5
    invoke-virtual {p2}, Ljava/util/LinkedHashMap;->keySet()Ljava/util/Set;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Lhiro/swiftkey/FrequencyLearning$Store;->ranked(Ljava/lang/String;Ljava/util/Set;)Ljava/util/List;

    move-result-object v0

    invoke-interface {v0}, Ljava/util/List;->isEmpty()Z

    move-result v2

    if-eqz v2, :cond_6

    return-object p0

    :cond_6
    new-instance v2, Ljava/util/ArrayList;

    invoke-interface {p0}, Ljava/util/List;->size()I

    move-result v3

    invoke-interface {v0}, Ljava/util/List;->size()I

    move-result v4

    add-int/2addr v3, v4

    invoke-direct {v2, v3}, Ljava/util/ArrayList;-><init>(I)V

    new-instance v3, Ljava/util/HashSet;

    invoke-direct {v3}, Ljava/util/HashSet;-><init>()V

    invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :goto_1
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v4

    if-eqz v4, :cond_8

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lhiro/swiftkey/FrequencyLearning$Entry;

    iget-object v5, v4, Lhiro/swiftkey/FrequencyLearning$Entry;->surface:Ljava/lang/String;

    invoke-virtual {p2, v5}, Ljava/util/LinkedHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Lm40/b;

    if-nez v5, :cond_7

    new-instance v5, Lm40/h0;

    iget-object v6, v4, Lhiro/swiftkey/FrequencyLearning$Entry;->surface:Ljava/lang/String;

    iget-object v7, p1, Ll40/c;->k:Ljava/lang/String;

    invoke-direct {v5, v6, v7, p1}, Lm40/h0;-><init>(Ljava/lang/String;Ljava/lang/String;Ll40/c;)V

    const-string v6, ""

    invoke-virtual {v5, v6}, Lm40/h0;->setTrailingSeparator(Ljava/lang/String;)V

    nop

    :cond_7
    invoke-virtual {v2, v5}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    iget-object v4, v4, Lhiro/swiftkey/FrequencyLearning$Entry;->surface:Ljava/lang/String;

    invoke-virtual {v3, v4}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    goto :goto_1

    :cond_8
    invoke-interface {p0}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p2

    :goto_2
    invoke-interface {p2}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_b

    invoke-interface {p2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lm40/b;

    if-eqz v0, :cond_9

    invoke-interface {v0}, Lm40/b;->getUserFacingText()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_a

    :cond_9
    invoke-virtual {v2, v0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_a
    goto :goto_2

    :cond_b
    invoke-static {v2, p1, v1}, Lhiro/swiftkey/KatakanaGlossCleanup;->cleanCandidates(Ljava/util/List;Ll40/c;Ljava/lang/String;)Ljava/util/List;

    move-result-object p0
    :try_end_0
    .catch Ljava/lang/RuntimeException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/LinkageError; {:try_start_0 .. :try_end_0} :catch_0

    return-object p0

    :cond_c
    :goto_3
    return-object p0

    :cond_d
    :goto_4
    return-object p0

    :catch_0
    move-exception p1

    return-object p0

    :catch_1
    move-exception p1

    return-object p0
.end method
