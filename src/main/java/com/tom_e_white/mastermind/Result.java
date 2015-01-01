package com.tom_e_white.mastermind;

import com.google.common.collect.Multiset;

import java.util.List;
import java.util.Map;

public class Result {
    private int solutionsCount;
    private boolean won;
    private List<List<Integer>> moves;
    private List<Multiset<Scores.Score>> scores;

    public Result(int solutionsCount, boolean won) {
        this.solutionsCount = solutionsCount;
        this.won = won;
    }

    public Result(int solutionsCount, boolean won, List<List<Integer>> moves, List<Multiset<Scores.Score>> scores) {
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

    public List<List<Integer>> getMoves() {
        return moves;
    }

    public List<Multiset<Scores.Score>> getScores() {
        return scores;
    }
}
