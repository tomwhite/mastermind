package com.tom_e_white.mastermind;

import com.google.common.collect.*;
import org.junit.Test;

import java.util.List;

public class TestGame {

    @Test
    public void testAllGames() {
        int numLost = 0;
        Multiset<Integer> solutionsHist = HashMultiset.create();
        Multiset<Integer> totalMovesHist = TreeMultiset.create();
        Multiset<List<Score>> scoresHist = HashMultiset.create();
        List<List<Move>> lostMoves = Lists.newArrayList();
        List<List<Score>> lostScores = Lists.newArrayList();
        double totalMoves = 0;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 6; k++) {
                    for (int l = 0; l < 6; l++) {
                        Move secret = new Move(i, j, k, l);
                        Game game = new TestedGame(secret);
                        Result result = game.play(new ComputerScorer(secret));
                        solutionsHist.add(result.getSolutionsCount());
                        totalMovesHist.add(result.getMoves().size());
                        List<Score> scores = result.getScores();
                        scores.remove(scores.size() - 1); // remove last move
                        scoresHist.add(scores);
                        totalMoves += result.getMoves().size();
                        if (!result.hasWon()) {
                            numLost++;
                            lostMoves.add(result.getMoves());
                            lostScores.add(scores);
                        }
                    }
                }
            }
        }
        System.out.println("Histogram:");
        solutionsHist = Multisets.copyHighestCountFirst(solutionsHist);
        System.out.println(solutionsHist);
        double total = 0;
        for (Integer i : solutionsHist.elementSet()) {
            total += (solutionsHist.count(i) * 1.0) / i;
        }
        System.out.println("Total: " + total + ", " + (100*total/1296) + "%");
        System.out.println("Lost: " + numLost);
        for (List<Score> scores : lostScores) {
            System.out.println(scoresHist.count(scores));
        }
        System.out.println("Moves: " + totalMovesHist);
        System.out.println("Avg moves: " +(totalMoves/1296));
    }
    
    @Test
    public void testSingleGame() {
        Move secret = new Move(0, 4, 5, 5);
        Game game = new TestedGame(secret);
        Result result = game.play(new ComputerScorer(secret));
        System.out.println("Setter: " + secret);
        for (int i = 0; i < result.getMoves().size(); i++) {
            System.out.println((i + 1) + ". " + result.getMoves().get(i) + "[" + result.getScores().get(i) + "]");
        }
    }
}
