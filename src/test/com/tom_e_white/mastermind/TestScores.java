package com.tom_e_white.mastermind;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static com.tom_e_white.mastermind.Scores.Score.RED;
import static com.tom_e_white.mastermind.Scores.Score.WHITE;
import static com.tom_e_white.mastermind.Scores.move;
import static com.tom_e_white.mastermind.Scores.score;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestScores {

    @Test
    public void test() {
        assertEquals(ImmutableMultiset.of(WHITE, WHITE, WHITE, WHITE),
                score(move(0, 1, 2, 3), move(0, 1, 2, 3)));
        assertEquals(ImmutableMultiset.of(WHITE, WHITE, WHITE),
                score(move(0, 1, 2, 2), move(0, 1, 2, 3)));
        assertEquals(ImmutableMultiset.of(WHITE, WHITE),
                score(move(0, 2, 2, 2), move(0, 1, 2, 3)));
        assertEquals(ImmutableMultiset.of(WHITE),
                score(move(2, 2, 2, 2), move(0, 1, 2, 3)));
        assertEquals(ImmutableMultiset.of(RED, RED, WHITE),
                score(move(0, 2, 2, 3), move(1, 2, 3, 2)));
        assertEquals(ImmutableMultiset.of(RED, RED, RED),
                score(move(0, 2, 2, 3), move(1, 0, 3, 2)));
        assertEquals(ImmutableMultiset.of(),
                score(move(0, 1, 2, 2), move(3, 3, 4, 5)));
    }

    @Test
    public void testScoreDeltaFor() {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 6; k++) {
                    for (int l = 0; l < 6; l++) {
                        List<Integer> secret = move(i, j, k, l);
                        reportScoreDeltaFor(secret, move(0, 1, 2, 3), move(0, 1, 2, 4), 3);
                    }
                }
            }
        }
    }

    public static void reportScoreDeltaFor(List<Integer> secret, List<Integer> move1, List<Integer> move2, int diffPos) {
        Set<Integer> diffPosNeg = Sets.newTreeSet(Sets.newHashSet(0, 1, 2, 3));
        diffPosNeg.remove(diffPos);

        Scores.ScoreDelta scoreDelta = Scores.scoreDelta(Scores.score(secret, move1), Scores.score(secret, move2));
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
            System.out.println(newCol + " appears in pos " + diffPos);
            assertEquals(newCol + " appears in pos " + diffPos, (long) secret.get(diffPos), newCol);
            if (rd == 0) {
                System.out.println(oldCol + " does not appear anywhere");
                assertFalse(oldCol + " does not appear anywhere", secret.contains(oldCol));
            } else if (rd == -1) {
                System.out.println(oldCol + " appears in pos " + diffPosNeg);
                boolean contains = false;
                for (int i : diffPosNeg) {
                    contains = contains || secret.get(i).equals(oldCol);
                }
                assertTrue(oldCol + " appears in pos " + diffPosNeg, contains);
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
}
