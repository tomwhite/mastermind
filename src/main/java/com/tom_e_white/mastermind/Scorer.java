package com.tom_e_white.mastermind;

import com.google.common.collect.Multiset;

import java.util.List;

public interface Scorer {
    Multiset<Scores.Score> score(List<Integer> move);
}
