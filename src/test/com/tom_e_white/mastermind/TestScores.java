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

    private List<List<Integer>> moves = Lists.newArrayList();

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
        // TODO: or use constraints in order to guide next move somehow?
        //      e.g. at each step look for a move that is edit-distance 1 from a previous one, and that has
        //      different colours - or refine rule so it doesn't assume colours are different...
        makeMove(move(0, 1, 2, 3));
        makeMove(move(0, 1, 2, 4));
        makeMove(move(0, 1, 5, 4));
        makeMove(move(0, 3, 5, 4));
        makeMove(move(2, 3, 5, 4));

//        reportScoreDeltaFor(move(0, 1, 2, 3), move(0, 1, 2, 4), 3);
//        reportScoreDeltaFor(move(0, 1, 2, 3), move(0, 1, 5, 3), 2);
//        reportScoreDeltaFor(move(0, 1, 2, 3), move(0, 4, 2, 3), 1);
//        reportScoreDeltaFor(move(0, 1, 2, 3), move(5, 1, 2, 3), 0);

        // at this point, stop and look at how far we've got
        // if #soln <=3 then just go through them
        // o/w find 2 solns that are diff=1 apart and run algorithm again

//        reportScoreDeltaFor(move(0, 1, 2, 3), move(0, 1, 2, 5), 3);
//        reportScoreDeltaFor(move(0, 1, 2, 3), move(0, 1, 4, 3), 2);
//        reportScoreDeltaFor(move(0, 1, 2, 3), move(0, 5, 2, 3), 1);
//        reportScoreDeltaFor(move(0, 1, 2, 3), move(4, 1, 2, 3), 0);

        Search<IntVar> search = new DepthFirstSearch<IntVar>();
        SelectChoicePoint<IntVar> select =
                new InputOrderSelect<IntVar>(store, v,
                        new IndomainMin<IntVar>());

        search.getSolutionListener().searchAll(true);
        search.getSolutionListener().recordSolutions(true);
        search.setPrintInfo(false);

        boolean result = search.labeling(store, select);

        int numberOfSolutions = search.getSolutionListener().solutionsNo();
        if (numberOfSolutions == 23) {
            System.out.println("Alert: " + secret);
            search.printAllSolutions();
        }
        return numberOfSolutions;

    }

    public void makeMove(List<Integer> move) {
        moves.add(move);

        Set<Integer> allPos = Sets.newTreeSet(Sets.newHashSet(0, 1, 2, 3));

        Multiset<Scores.Score> score = Scores.score(secret, move);

        int rc = score.count(RED);
        int wc = score.count(WHITE);

        if (wc == 0 && rc == 0) {
            doesNotAppearAnywhere(move.get(0));
            doesNotAppearAnywhere(move.get(1));
            doesNotAppearAnywhere(move.get(2));
            doesNotAppearAnywhere(move.get(3));
        } else if (wc == 1 && rc == 0) {
            store.impose(appearsInConstraint(move, allPos));
        } else if (wc == 2 && rc == 0) {
            ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
            constraints.add(appearsInBothConstraint(move.get(0), 0, move.get(1), 1));
            constraints.add(appearsInBothConstraint(move.get(0), 0, move.get(2), 2));
            constraints.add(appearsInBothConstraint(move.get(0), 0, move.get(3), 3));
            constraints.add(appearsInBothConstraint(move.get(1), 1, move.get(2), 2));
            constraints.add(appearsInBothConstraint(move.get(1), 1, move.get(3), 3));
            constraints.add(appearsInBothConstraint(move.get(2), 2, move.get(3), 3));
            store.impose(new Or(constraints));
        } else if (wc == 0 && rc == 1) {
            ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
            constraints.add(appearsInConstraint(move.get(0), Sets.newHashSet(1, 2, 3)));
            constraints.add(appearsInConstraint(move.get(1), Sets.newHashSet(0, 2, 3)));
            constraints.add(appearsInConstraint(move.get(2), Sets.newHashSet(0, 1, 3)));
            constraints.add(appearsInConstraint(move.get(3), Sets.newHashSet(0, 1, 2)));
            store.impose(new Or(constraints));
        } else if (wc == 4) {
            appearsIn(move.get(0), 0);
            appearsIn(move.get(1), 1);
            appearsIn(move.get(2), 2);
            appearsIn(move.get(3), 3);
        }

        if (wc == 0) {
            doesNotAppearIn(move.get(0), 0);
            doesNotAppearIn(move.get(1), 1);
            doesNotAppearIn(move.get(2), 2);
            doesNotAppearIn(move.get(3), 3);
        }

        if (moves.size() <= 1) {
            return;
        }

        for (List<Integer> previousMove : moves) {
            int diffPos = diff(previousMove, move);
            if (diffPos != -1) {
                reportScoreDeltaFor(previousMove, move, diffPos);
                break; // TODO: remove this so we can check all moves edit dist 1 away
            }
        }
    }

    private int diff(List<Integer> move1, List<Integer> move2) {
        int diffPos = -1;
        for (int i = 0; i < 4; i++) {
            if (move1.get(i) != move2.get(i)) {
                if (diffPos == -1) {
                    diffPos = i;
                } else {
                    return -1; // more than one diff
                }
            }
        }
        return diffPos;
    }

    private boolean hasDistinctColours(List<Integer> move) {
        return Sets.newHashSet(move).size() == 4;
    }


    public void reportScoreDeltaFor(List<Integer> move1, List<Integer> move2, int diffPos) {
        Set<Integer> diffPosNeg = Sets.newTreeSet(Sets.newHashSet(0, 1, 2, 3));
        diffPosNeg.remove(diffPos);

        Multiset<Scores.Score> score1 = Scores.score(secret, move1);
        Multiset<Scores.Score> score2 = Scores.score(secret, move2);

        Scores.ScoreDelta scoreDelta = Scores.scoreDelta(score1, score2);
        System.out.println(secret);
        System.out.println(move1 + "; " + score1);
        System.out.println(move2 + "; " + score2);
        int rd = scoreDelta.getRedDelta();
        int wd = scoreDelta.getWhiteDelta();
        int oldCol = move1.get(diffPos);
        int newCol = move2.get(diffPos);
        if (wd == 0) {
            doesNotAppearIn(oldCol, diffPos);
            doesNotAppearIn(newCol, diffPos);
            if (rd == 0) {
                eitherDontAppearAnywhereOrBothAppearIn(oldCol, newCol, diffPosNeg);
            } else if (rd == 1) {
                appearsIn(newCol, diffPosNeg);
            } else if (rd == -1) {
                appearsIn(oldCol, diffPosNeg);
            }
        } else if (wd == 1) {
            appearsIn(newCol, diffPos);
            doesNotAppearIn(oldCol, diffPos);
            if (hasDistinctColours(move1) && hasDistinctColours(move2)) {
                if (rd == 0) {
                    doesNotAppearAnywhere(oldCol);
                } else if (rd == -1) {
                    appearsIn(oldCol, diffPosNeg);
                }
            }
        } else if (wd == -1) {
            appearsIn(oldCol, diffPos);
            doesNotAppearIn(newCol, diffPos);
            if (hasDistinctColours(move1) && hasDistinctColours(move2)) {
                if (rd == 0) {
                    doesNotAppearAnywhere(newCol);
                } else if (rd == 1) {
                    appearsIn(newCol, diffPosNeg);
                }
            }
        }
        System.out.println();
    }

    void appearsIn(int colour, int pos) {
        System.out.println(colour + " appears in pos " + pos);
        assertEquals(colour + " appears in pos " + pos, (long) secret.get(pos), colour);
        store.impose(new XeqC(v[pos], colour));
        System.out.println("TODO: no need to mutate " + pos + " again");
    }

    void doesNotAppearIn(int colour, int pos) {
        System.out.println(colour + " does not appear in " + pos);
        assertFalse(colour + " does not appear in " + pos, secret.get(pos).equals(colour));
        store.impose(new Not(new XeqC(v[pos], colour)));
    }

    boolean appearsIn(int colour, Set<Integer> positions) {
        System.out.println(colour + " appears in one of pos " + positions);
        store.impose(appearsInConstraint(colour, positions));
        System.out.println("TODO: we know " + colour + " appears so try it again (in one of " + positions + ")");

        boolean contains = false;
        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        for (int i : positions) {
            constraints.add(new XeqC(v[i], colour));
            contains = contains || secret.get(i).equals(colour);
        }
        store.impose(new Or(constraints));
        assertTrue(colour + " appears in one of pos " + positions, contains);
        return contains;
    }

    void doesNotAppearAnywhere(int colour) {
        System.out.println(colour + " does not appear anywhere");
        assertFalse(colour + " does not appear anywhere", secret.contains(colour));
        store.impose(new Not(new XeqC(v[0], colour)));
        store.impose(new Not(new XeqC(v[1], colour)));
        store.impose(new Not(new XeqC(v[2], colour)));
        store.impose(new Not(new XeqC(v[3], colour)));
        System.out.println("TODO: no need to try " + colour + " again");
    }

    void eitherDontAppearAnywhereOrBothAppearIn(int col1, int col2, Set<Integer> positions) {
        System.out.println("EITHER " + col1 + " and " + col2 + " don't appear anywhere OR " + col1 + " and " + col2 + " both appear in pos " + positions);

        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        constraints.add(new Not(new XeqC(v[0], col1)));
        constraints.add(new Not(new XeqC(v[1], col1)));
        constraints.add(new Not(new XeqC(v[2], col1)));
        constraints.add(new Not(new XeqC(v[3], col1)));
        constraints.add(new Not(new XeqC(v[0], col2)));
        constraints.add(new Not(new XeqC(v[1], col2)));
        constraints.add(new Not(new XeqC(v[2], col2)));
        constraints.add(new Not(new XeqC(v[3], col2)));

        store.impose(new Or(new And(constraints),
                new And(appearsInConstraint(col1, positions), appearsInConstraint(col2, positions))));
    }

    ////

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

    PrimitiveConstraint appearsInBothConstraint(int col1, int pos1, int col2, int pos2) {
        return new And(new XeqC(v[pos1], col1), new XeqC(v[pos2], col2));
    }

}
