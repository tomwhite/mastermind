package com.tom_e_white.mastermind;

import com.google.common.collect.*;
import org.junit.Test;

import java.util.List;

import static com.tom_e_white.mastermind.Scores.move;

public class TestGame {

    @Test
    public void testAllGames() {
        int numLost = 0;
        Multiset<Integer> solutionsHist = HashMultiset.create();
        Multiset<Integer> totalMovesHist = TreeMultiset.create();
        Multiset<List<Multiset<Scores.Score>>> scoresHist = HashMultiset.create();
        List<List<List<Integer>>> lostMoves = Lists.newArrayList();
        List<List<Multiset<Scores.Score>>> lostScores = Lists.newArrayList();
        double totalMoves = 0;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 6; k++) {
                    for (int l = 0; l < 6; l++) {
                        List<Integer> secret = move(i, j, k, l);
                        Game game = new Game(secret);
                        Result result = game.playGame(new ComputerScorer(secret));
                        solutionsHist.add(result.getSolutionsCount());
                        totalMovesHist.add(result.getMoves().size());
                        List<Multiset<Scores.Score>> scores = result.getScores();
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
        for (List<Multiset<Scores.Score>> scores : lostScores) {
            System.out.println(scoresHist.count(scores));
        }
        System.out.println("Moves: " + totalMovesHist);
        System.out.println("Avg moves: " +(totalMoves/1296));
    }

}
