package com.tom_e_white.mastermind;

import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Prompts a human to score moves for when a human is playing the computer.
 */
public class HumanScorer implements Scorer {

    private BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    @Override
    public Score score(Move move) {
        System.out.println("My move: " + move);
        System.out.println("Enter white and red pegs (in any order). For example, rrw for two reds and a white.");
        try {
            String line = br.readLine();
            List<Score.Peg> pegs = Lists.newArrayList();
            // add whites first
            for (char c : line.toCharArray()) {
                if (c == 'w' || c == 'W') {
                    pegs.add(Score.Peg.WHITE);
                }
            }
            for (char c : line.toCharArray()) {
                if (c == 'r' || c == 'R') {
                    pegs.add(Score.Peg.RED);
                }
            }
            return new Score(pegs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
