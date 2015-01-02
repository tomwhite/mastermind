package com.tom_e_white.mastermind;

import com.google.common.collect.Multiset;

import java.util.List;

/**
 * The result of the game. Contains information about the moves played, the scores, who won etc.
 */
public class Result {
    private int solutionsCount;
    private boolean won;
    private List<Move> moves;
    private List<Multiset<Scores.Score>> scores;

    public Result(int solutionsCount, boolean won, List<Move> moves, List<Multiset<Scores.Score>> scores) {
        this.solutionsCount = solutionsCount;
        this.won = won;
        this.moves = moves;
        this.scores = scores;
    }

    public int getSolutionsCount() {
        return solutionsCount;
    }

    public boolean hasWon() {
        return won;
    }

    public List<Move> getMoves() {
        return moves;
    }

    public List<Multiset<Scores.Score>> getScores() {
        return scores;
    }
}
