package com.tom_e_white.mastermind;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import org.junit.Test;

import java.util.List;

import static com.tom_e_white.mastermind.Scores.move;

public class TestGame {

    @Test
    public void testAllGames() {
        Multiset<Integer> hist = HashMultiset.create();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 6; k++) {
                    for (int l = 0; l < 6; l++) {
                        List<Integer> secret = move(i, j, k, l);
                        Game game = new Game(secret);
                        hist.add(game.playGame(new ComputerScorer(secret)).getSolutionsCount());
                    }
                }
            }
        }
        System.out.println("Histogram:");
        hist = Multisets.copyHighestCountFirst(hist);
        System.out.println(hist);
        double total = 0;
        for (Integer i : hist.elementSet()) {
            total += (hist.count(i) * 1.0) / i;
        }
        System.out.println("Total: " + total + ", " + (100*total/1296) + "%");
    }

}
