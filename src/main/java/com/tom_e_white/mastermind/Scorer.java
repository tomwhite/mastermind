package com.tom_e_white.mastermind;

import com.google.common.collect.Multiset;

public interface Scorer {
    Multiset<Scores.Score> score(Move move);
}
