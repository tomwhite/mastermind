package com.tom_e_white.mastermind;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class HumanScorer implements Scorer {

    private BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    @Override
    public Multiset<Scores.Score> score(List<Integer> move) {
        System.out.println("My move: " + toString(move));
        System.out.println("Enter white and red pegs (in any order). For example, rrw for two reds and a white.");
        try {
            String line = br.readLine();
            HashMultiset<Scores.Score> score = HashMultiset.create();
            // add whites first
            for (char c : line.toCharArray()) {
                if (c == 'w' || c == 'W') {
                    score.add(Scores.Score.WHITE);
                }
            }
            for (char c : line.toCharArray()) {
                if (c == 'r' || c == 'R') {
                    score.add(Scores.Score.RED);
                }
            }
            return score;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String toString(List<Integer> move) {
        StringBuilder sb = new StringBuilder();
        for (int i : move) {
            sb.append(Peg.values()[i]).append(" ");
        }
        return sb.toString();
    }
}
