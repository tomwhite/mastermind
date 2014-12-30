package com.tom_e_white.mastermind;

public class Result {
    private int solutionsCount;
    private boolean won;

    public Result(int solutionsCount, boolean won) {
        this.solutionsCount = solutionsCount;
        this.won = won;
    }

    public int getSolutionsCount() {
        return solutionsCount;
    }

    public boolean hasWon() {
        return won;
    }
}
