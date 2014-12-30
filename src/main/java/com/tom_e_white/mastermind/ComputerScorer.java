package com.tom_e_white.mastermind;

import com.google.common.collect.Multiset;

import java.util.List;

public class ComputerScorer implements Scorer {
    private List<Integer> secret;

    public ComputerScorer(List<Integer> secret) {
        this.secret = secret;
    }

    @Override
    public Multiset<Scores.Score> score(List<Integer> move) {
        return Scores.score(secret, move);
    }
}
