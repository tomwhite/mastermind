package com.tom_e_white.mastermind;

import com.google.common.collect.*;

import java.util.*;

import static com.tom_e_white.mastermind.Scores.Score.RED;
import static com.tom_e_white.mastermind.Scores.Score.WHITE;

public class Scores {

    public enum Score { RED, WHITE }

    public static void main(String[] args) {
        reportHistogramFor(0, 1, 2, 3);
        reportHistogramFor(0, 1, 2, 2);
        reportHistogramFor(0, 1, 1, 1);
        reportHistogramFor(0, 0, 0, 0);
    }

    public static List<Integer> move(int a, int b, int c, int d) {
        return Arrays.asList(a, b, c, d);
    }

    public static Multiset<Score> score(List<Integer> secret, List<Integer> move) {
        Multiset<Score> scores = EnumMultiset.create(Score.class);
        List<Boolean> matched = Arrays.asList(false, false, false, false);
        List<Boolean> used = Arrays.asList(false, false, false, false);
        for (int i = 0; i < 4; i++) {
            if (move.get(i).equals(secret.get(i))) {
                scores.add(WHITE);
                matched.set(i, true);
                used.set(i, true);
            }
        }
        for (int i = 0; i < 4; i++) {
            if (matched.get(i)) {
                continue;
            }
            for (int j = 0; j < 4; j++) {
                if (i != j && !used.get(j) && move.get(i).equals(secret.get(j))) {
                    scores.add(RED);
                    used.set(j, true);
                    break;
                }
            }
        }
        return scores;
    }

    public static Multiset<Multiset<Score>> scoreHistogram(int a, int b, int c, int d) {
        List<Integer> secret = move(a, b, c, d);
        Multiset<Multiset<Score>> histogram = HashMultiset.create();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 6; k++) {
                    for (int l = 0; l < 6; l++) {
                        List<Integer> move = move(i, j, k, l);
                        Multiset<Score> score = score(secret, move);
                        histogram.add(score);
                        //System.out.println(i + "" + j + "" + k + "" + l + "; " + score);
                    }
                }
            }
        }
        return Multisets.copyHighestCountFirst(histogram);
    }

    public static double averageScore(Multiset<Multiset<Score>> histogram) {
        double s = 0;
        for (Multiset<Score> set : histogram) {
            for (Score score : set) {
                s += score == WHITE ? 2 : 1;
            }
        }
        return s / histogram.size();
    }

    public static void reportHistogramFor(int a, int b, int c, int d) {
        Multiset<Multiset<Score>> histogram = scoreHistogram(a, b, c, d);
        System.out.println(a + "" + b + "" + c + "" + d);
        System.out.println(histogram);
        System.out.println(averageScore(histogram));
        System.out.println();
    }

}
