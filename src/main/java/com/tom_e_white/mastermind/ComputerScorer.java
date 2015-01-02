package com.tom_e_white.mastermind;

/**
 * A computer scorer for when the computer knows the secret answer.
 */
public class ComputerScorer implements Scorer {
    private Move secret;

    public ComputerScorer(Move secret) {
        this.secret = secret;
    }

    @Override
    public Score score(Move move) {
        return Score.score(secret, move);
    }
}
