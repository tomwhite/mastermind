package com.tom_e_white.mastermind;

import com.google.common.collect.Multiset;

public class ComputerScorer implements Scorer {
    private Move secret;

    public ComputerScorer(Move secret) {
        this.secret = secret;
    }

    @Override
    public Multiset<Scores.Score> score(Move move) {
        return Scores.score(secret, move);
    }
}
