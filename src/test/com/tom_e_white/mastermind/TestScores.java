package com.tom_e_white.mastermind;

import com.google.common.collect.*;
import org.jacop.constraints.*;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.*;
import org.junit.Test;

import java.util.ArrayList;
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

    private List<Integer> secret;
    private Store store;
    private IntVar[] v;

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
        Multiset<Integer> hist = HashMultiset.create();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 6; k++) {
                    for (int l = 0; l < 6; l++) {
                        secret = move(i, j, k, l);
                        hist.add(playGame());
                    }
                }
            }
        }
        System.out.println(Multisets.copyHighestCountFirst(hist));
    }

    public int playGame() {
        System.out.println("New Game");

        store = new Store();
        v = new IntVar[4];
        for (int i = 0; i < v.length; i++) {
            v[i] = new IntVar(store, "v" + i, 0, 5);
        }

        // Keep a list of mutations to try
        // If a position is found then remove all the mutations from that position
        // If a new colour is found to be tried, then add it at the front of the list for that position
        List<List<Integer>> mutations = Lists.newArrayList(
                (List<Integer>) Lists.newArrayList(4, 1),
                Lists.newArrayList(5, 2),
                Lists.newArrayList(4, 3),
                Lists.newArrayList(5, 0)
        );

        // TODO: don't just go one step from 0123 - how to improve walk through?
        // TODO: choose colours to change based on how much info they gave in earlier mutations?
        reportScoreDeltaFor(move(0, 1, 2, 3), move(0, 1, 2, 4), 3);
        reportScoreDeltaFor(move(0, 1, 2, 3), move(0, 1, 5, 3), 2);
        reportScoreDeltaFor(move(0, 1, 2, 3), move(0, 4, 2, 3), 1);
        reportScoreDeltaFor(move(0, 1, 2, 3), move(5, 1, 2, 3), 0);

//        reportScoreDeltaFor(secret, move(0, 1, 2, 3), move(0, 1, 2, 5), 3, store, v);
//        reportScoreDeltaFor(secret, move(0, 1, 2, 3), move(0, 1, 4, 3), 2, store, v);
//        reportScoreDeltaFor(secret, move(0, 1, 2, 3), move(0, 5, 2, 3), 1, store, v);


        Search<IntVar> search = new DepthFirstSearch<IntVar>();
        SelectChoicePoint<IntVar> select =
                new InputOrderSelect<IntVar>(store, v,
                        new IndomainMin<IntVar>());

        search.getSolutionListener().searchAll(true);
        search.getSolutionListener().recordSolutions(true);
        search.setPrintInfo(false);

        boolean result = search.labeling(store, select);

        //search.printAllSolutions();

        int numberOfSolutions = search.getSolutionListener().solutionsNo();
        if (numberOfSolutions == 14) {
            System.out.println("Alert: " + secret);
        }
        return numberOfSolutions;

    }

    public void reportScoreDeltaFor(List<Integer> move1, List<Integer> move2, int diffPos) {
        Set<Integer> allPos = Sets.newTreeSet(Sets.newHashSet(0, 1, 2, 3));

        Set<Integer> diffPosNeg = Sets.newTreeSet(Sets.newHashSet(0, 1, 2, 3));
        diffPosNeg.remove(diffPos);

        Multiset<Scores.Score> score1 = Scores.score(secret, move1);
        Multiset<Scores.Score> score2 = Scores.score(secret, move2);

        int rc = score2.count(RED);
        int wc = score2.count(WHITE);

        if (wc == 0 && rc == 0) {
            // TODO: colours don't appear anywhere
        } else if (wc == 1 && rc == 0) {
            store.impose(appearsInConstraint(move2, allPos));
        }

        Scores.ScoreDelta scoreDelta = Scores.scoreDelta(score1, score2);
        System.out.println(secret);
        System.out.println(move1 + "; " + score1);
        System.out.println(move2 + "; " + score2);
        int rd = scoreDelta.getRedDelta();
        int wd = scoreDelta.getWhiteDelta();
        int oldCol = move1.get(diffPos);
        int newCol = move2.get(diffPos);
        if (wd == 0) {
            System.out.println(oldCol + " does not appear in " + diffPos);
            assertFalse(oldCol + " does not appear in " + diffPos, secret.get(diffPos).equals(oldCol));
            store.impose(new Not(new XeqC(v[diffPos], oldCol)));

            System.out.println(newCol + " does not appear in " + diffPos);
            assertFalse(newCol + " does not appear in " + diffPos, secret.get(diffPos).equals(newCol));
            store.impose(new Not(new XeqC(v[diffPos], newCol)));

            if (rd == 0) {
                System.out.println("EITHER " + oldCol + " and " + newCol + " don't appear anywhere OR " + oldCol + " and " + newCol + " both appear in pos " + diffPosNeg);
                store.impose(new Or(doNotAppearAnywhere(oldCol, newCol), bothAppearIn(oldCol, newCol, diffPosNeg)));
                // TODO: is this condition a pointer to try another mutation at this position? <- good idea
            } else if (rd == 1) {
                System.out.println(newCol + " appears in pos " + diffPosNeg);
                assertTrue(newCol + " appears in pos " + diffPosNeg, appearsIn(newCol, diffPosNeg));
                store.impose(appearsInConstraint(newCol, diffPosNeg));
                System.out.println("TODO: we know " + newCol + " appears so try it again (in one of diffPosNeg)");
            } else if (rd == -1) {
                System.out.println(oldCol + " appears in pos " + diffPosNeg);
                assertTrue(oldCol + " appears in pos " + diffPosNeg, appearsIn(oldCol, diffPosNeg));
                store.impose(appearsInConstraint(oldCol, diffPosNeg));
                System.out.println("TODO: we know " + oldCol + " appears so try it again");
            }
        } else if (wd == 1) {
            System.out.println(newCol + " appears in pos " + diffPos);
            assertEquals(newCol + " appears in pos " + diffPos, (long) secret.get(diffPos), newCol);
            store.impose(new XeqC(v[diffPos], newCol));
            System.out.println("TODO: no need to mutate " + diffPos + " again");
            if (rd == 0) {
                System.out.println(oldCol + " does not appear anywhere");
                assertFalse(oldCol + " does not appear anywhere", secret.contains(oldCol));
                store.impose(new Not(new XeqC(v[0], oldCol)));
                store.impose(new Not(new XeqC(v[1], oldCol)));
                store.impose(new Not(new XeqC(v[2], oldCol)));
                store.impose(new Not(new XeqC(v[3], oldCol)));
                System.out.println("TODO: no need to try " + oldCol + " again");
            } else if (rd == -1) {
                System.out.println(oldCol + " appears in pos " + diffPosNeg);
                assertTrue(oldCol + " appears in pos " + diffPosNeg, appearsIn(oldCol, diffPosNeg));
                store.impose(appearsInConstraint(oldCol, diffPosNeg));

                System.out.println(oldCol + " does not appear in " + diffPos);
                assertFalse(oldCol + " does not appear in " + diffPos, secret.get(diffPos).equals(oldCol));
                store.impose(new Not(new XeqC(v[diffPos], oldCol)));
            }
        } else if (wd == -1) {
            System.out.println(oldCol + " appears in pos " + diffPos);
            assertEquals(oldCol + " appears in pos " + diffPos, (long) secret.get(diffPos), oldCol);
            store.impose(new XeqC(v[diffPos], oldCol));
            System.out.println("TODO: no need to mutate " + diffPos + " again");
            if (rd == 0) {
                System.out.println(newCol + " does not appear anywhere");
                assertFalse(newCol + " does not appear anywhere", secret.contains(newCol));
                store.impose(new Not(new XeqC(v[0], newCol)));
                store.impose(new Not(new XeqC(v[1], newCol)));
                store.impose(new Not(new XeqC(v[2], newCol)));
                store.impose(new Not(new XeqC(v[3], newCol)));
                System.out.println("TODO: no need to try " + newCol + " again");
            } else if (rd == 1) {
                System.out.println(newCol + " appears in pos " + diffPosNeg);
                assertTrue(newCol + " appears in pos " + diffPosNeg, appearsIn(newCol, diffPosNeg));
                store.impose(appearsInConstraint(newCol, diffPosNeg));

                System.out.println(newCol + " does not appear in " + diffPos);
                assertFalse(newCol + " does not appear in " + diffPos, secret.get(diffPos).equals(newCol));
                store.impose(new Not(new XeqC(v[diffPos], newCol)));
            }
        }
        System.out.println();
    }

    boolean appearsIn(int colour, Set<Integer> positions) {
        boolean contains = false;
        for (int i : positions) {
            contains = contains || secret.get(i).equals(colour);
        }
        return contains;
    }

    PrimitiveConstraint appearsInConstraint(int colour, Set<Integer> positions) {
        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        for (int i : positions) {
            constraints.add(new XeqC(v[i], colour));
        }
        return new Or(constraints);
    }

    PrimitiveConstraint appearsInConstraint(List<Integer> move, Set<Integer> positions) {
        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        for (int i : positions) {
            constraints.add(new XeqC(v[i], move.get(i)));
        }
        return new Or(constraints);
    }

    PrimitiveConstraint doNotAppearAnywhere(int col1, int col2) {
        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        constraints.add(new Not(new XeqC(v[0], col1)));
        constraints.add(new Not(new XeqC(v[1], col1)));
        constraints.add(new Not(new XeqC(v[2], col1)));
        constraints.add(new Not(new XeqC(v[3], col1)));
        constraints.add(new Not(new XeqC(v[0], col2)));
        constraints.add(new Not(new XeqC(v[1], col2)));
        constraints.add(new Not(new XeqC(v[2], col2)));
        constraints.add(new Not(new XeqC(v[3], col2)));
        return new And(constraints);
    }

    PrimitiveConstraint bothAppearIn(int col1, int col2, Set<Integer> positions) {
        return new And(appearsInConstraint(col1, positions), appearsInConstraint(col2, positions));
    }

}
