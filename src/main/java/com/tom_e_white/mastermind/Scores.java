package com.tom_e_white.mastermind;

import com.google.common.collect.*;

import java.util.*;

import static com.tom_e_white.mastermind.Scores.Score.RED;
import static com.tom_e_white.mastermind.Scores.Score.WHITE;

public class Scores {

    public enum Score { RED, WHITE }

    public static class ScoreDelta {
        int whiteDelta;
        int redDelta;
        public ScoreDelta(int whiteDelta, int redDelta) {
            this.whiteDelta = whiteDelta;
            this.redDelta = redDelta;
        }
        public int getWhiteDelta() {
            return whiteDelta;
        }
        public int getRedDelta() {
            return redDelta;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ScoreDelta that = (ScoreDelta) o;

            if (redDelta != that.redDelta) return false;
            if (whiteDelta != that.whiteDelta) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = whiteDelta;
            result = 31 * result + redDelta;
            return result;
        }

        @Override
        public String toString() {
            return "ScoreDelta[" +
                    "W=" + whiteDelta +
                    ",R=" + redDelta +
                    ']';
        }
    }

    public static void main(String[] args) {
        reportHistogramFor(0, 1, 2, 3);
        reportHistogramFor(0, 1, 2, 2);
        reportHistogramFor(0, 1, 1, 1);
        reportHistogramFor(0, 0, 0, 0);
        List<Integer> secret = move(2,1,3,3);
        System.out.println(secret);
        System.out.println(score(secret, move(0, 1, 2, 3)));
        System.out.println(score(secret, move(0, 1, 2, 4)));
        System.out.println(scoreDelta(score(secret, move(0, 1, 2, 3)), score(secret, move(0, 1, 2, 4))));
        reportScoreDeltaHistogramFor(move(0, 1, 2, 3), move(0, 1, 2, 4));
        secret = move(1, 5, 4, 3);
        secret = randomMove();
        reportScoreDeltaFor(secret, move(0, 1, 2, 3), move(0, 1, 2, 4), 3);
    }

    static Random R = new Random();

    public static List<Integer> randomMove() {
        return move(R.nextInt(6), R.nextInt(6), R.nextInt(6), R.nextInt(6));
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

    public static Multiset<ScoreDelta> scoreDelta(List<Integer> move1, List<Integer> move2) {
        Multiset<ScoreDelta> histogram = HashMultiset.create();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 6; k++) {
                    for (int l = 0; l < 6; l++) {
                        List<Integer> secret = move(i, j, k, l);
                        histogram.add(scoreDelta(score(secret, move1), score(secret, move2)));
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
    public static void reportScoreDeltaHistogramFor(List<Integer> move1, List<Integer> move2) {
        Multiset<ScoreDelta> histogram = scoreDelta(move1, move2);
        System.out.println(move1);
        System.out.println(move2);
        System.out.println(histogram);
        System.out.println();
    }
    public static void reportScoreDeltaFor(List<Integer> secret, List<Integer> move1, List<Integer> move2, int diffPos) {
        Set<Integer> diffPosNeg = Sets.newTreeSet(Sets.newHashSet(0, 1, 2, 3));
        diffPosNeg.remove(diffPos);

        ScoreDelta scoreDelta = scoreDelta(score(secret, move1), score(secret, move2));
        System.out.println(secret);
        System.out.println(move1);
        System.out.println(move2);
        int rd = scoreDelta.getRedDelta();
        int wd = scoreDelta.getWhiteDelta();
        int oldCol = move1.get(3);
        int newCol = move2.get(3);
        if (wd == 0) {
            if (rd == 0) {
                System.out.println("EITHER " + oldCol + " and " + newCol + " don't appear anywhere OR " + oldCol + " and " + newCol + " both appear in pos " + diffPosNeg);
            } else if (rd == 1) {
                System.out.println(newCol + " appears in pos " + diffPosNeg);
            } else if (rd == -1) {
                System.out.println(oldCol + " appears in pos " + diffPosNeg);
            }
        } else if (wd == 1) {
            if (rd == 0) {
                System.out.println(newCol + " appears in pos " + diffPos);
                System.out.println(oldCol + " does not appear anywhere");
            } else if (rd == -1) {
                System.out.println(newCol + " appears in pos " + diffPos);
                System.out.println(oldCol + " appears in pos " + diffPosNeg);
            }
        } else if (wd == -1) {
            if (rd == 0) {
                System.out.println(oldCol + " appears in pos " + diffPos);
                System.out.println(newCol + " does not appear anywhere");
            } else if (rd == 1) {
                System.out.println(oldCol + " appears in pos" + diffPos);
                System.out.println(newCol + " appears in pos " + diffPosNeg);
            }
        }
        System.out.println();
    }
    public static ScoreDelta scoreDelta(Multiset<Score> score1, Multiset<Score> score2) {
        return new ScoreDelta(score2.count(WHITE) - score1.count(WHITE), score2.count(RED) - score1.count(RED));
    }

}
