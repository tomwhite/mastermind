package com.tom_e_white.mastermind;

import com.google.common.collect.*;
import org.jacop.constraints.*;
import org.jacop.core.Domain;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.tom_e_white.mastermind.Scores.Score.NONE;
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

    private List<List<Integer>> moves;

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
    public void testScoreCombinations() {
        assertEquals(set(Lists.newArrayList(NONE, NONE, NONE, NONE)),
                Scores.scoreCombinations(ImmutableMultiset.of(NONE, NONE, NONE, NONE)));
        assertEquals(set(Lists.newArrayList(WHITE, NONE, NONE, NONE), Lists.newArrayList(NONE, WHITE, NONE, NONE),
                        Lists.newArrayList(NONE, NONE, WHITE, NONE), Lists.newArrayList(NONE, NONE, NONE, WHITE)),
                Scores.scoreCombinations(ImmutableMultiset.of(WHITE, NONE, NONE, NONE)));
        assertEquals(set(Lists.newArrayList(NONE, NONE, WHITE, RED),
                        Lists.newArrayList(NONE, NONE, RED, WHITE),
                        Lists.newArrayList(NONE, WHITE, NONE, RED),
                        Lists.newArrayList(NONE, WHITE, RED, NONE),
                        Lists.newArrayList(NONE, RED, NONE, WHITE),
                        Lists.newArrayList(NONE, RED, WHITE, NONE),
                        Lists.newArrayList(WHITE, NONE, NONE, RED),
                        Lists.newArrayList(WHITE, NONE, RED, NONE),
                        Lists.newArrayList(WHITE, RED, NONE, NONE),
                        Lists.newArrayList(RED, NONE, NONE, WHITE),
                        Lists.newArrayList(RED, NONE, WHITE, NONE),
                        Lists.newArrayList(RED, WHITE, NONE, NONE)),
                Scores.scoreCombinations(ImmutableMultiset.of(WHITE, RED, NONE, NONE)));
    }

    private Set<List<Scores.Score>> set(List<Scores.Score>... scores) {
        Set s = new LinkedHashSet<List<Scores.Score>>();
        for (List<Scores.Score> score : scores) {
            s.add(score);
        }
        return s;
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
        System.out.println("Histogram:");
        System.out.println(Multisets.copyHighestCountFirst(hist));
    }

    public int playGame() {
        System.out.println("New Game");

        moves = Lists.newArrayList();
        store = new Store();
        v = new IntVar[4];
        for (int i = 0; i < v.length; i++) {
            v[i] = new IntVar(store, "v" + i, 0, 5);
        }

        // TODO: don't just go one step from 0123 - how to improve walk through?
        // TODO: choose colours to change based on how much info they gave in earlier mutations?
        // TODO: or use constraints in order to guide next move somehow?
        //      e.g. at each step look for a move that is edit-distance 1 from a previous one, and that has
        //      different colours
        makeMove(move(0, 1, 2, 3));
        makeMove(move(0, 1, 2, 4));
        makeMove(move(0, 1, 5, 4));
        makeMove(move(0, 3, 5, 4));
        makeMove(move(2, 3, 5, 4));
        makeMove(search());
        makeMove(search());

        // at this point, stop and look at how far we've got
        // if #soln <=3 then just go through them
        // o/w find 2 solns that are diff=1 apart and run algorithm again

//        reportScoreDeltaFor(move(0, 1, 2, 3), move(0, 1, 2, 5), 3);
//        reportScoreDeltaFor(move(0, 1, 2, 3), move(0, 1, 4, 3), 2);
//        reportScoreDeltaFor(move(0, 1, 2, 3), move(0, 5, 2, 3), 1);
//        reportScoreDeltaFor(move(0, 1, 2, 3), move(4, 1, 2, 3), 0);

        return countSolutions();
    }

    private List<Integer> search() {
        Search<IntVar> search = new DepthFirstSearch<IntVar>();
        SelectChoicePoint<IntVar> select =
                new InputOrderSelect<IntVar>(store, v,
                        new IndomainMin<IntVar>());

        search.setAssignSolution(false); // don't assign variables after finding a solution, http://sourceforge.net/p/jacop-solver/discussion/1220992/thread/4caf2979/
        search.getSolutionListener().searchAll(true);
        search.getSolutionListener().recordSolutions(true);
        search.setPrintInfo(false);

        boolean result = search.labeling(store, select);

        List<Integer> solution = Lists.newArrayList();
        int numberOfSolutions = search.getSolutionListener().solutionsNo();
        for (int i = 1; i <= numberOfSolutions; i++) {
            Domain[] sol = search.getSolutionListener().getSolution(i);
            for (int j = 0; j < sol.length; j++) {
                solution.add(sol[j].valueEnumeration().nextElement()); // convert domain to int
            }
            break; // just return first to start with
        }
        if (solution.size() != 4) {
            System.out.println(solution);
        }
        return solution;
    }

    private int countSolutions() {
        Search<IntVar> search = new DepthFirstSearch<IntVar>();
        SelectChoicePoint<IntVar> select =
                new InputOrderSelect<IntVar>(store, v,
                        new IndomainMin<IntVar>());

        search.getSolutionListener().searchAll(true);
        search.getSolutionListener().recordSolutions(true);
        search.setPrintInfo(false);

        boolean result = search.labeling(store, select);

        int numberOfSolutions = search.getSolutionListener().solutionsNo();
        if (numberOfSolutions == 0) {
            throw new IllegalStateException("Zero solutions for " + secret);
        }
        if (numberOfSolutions == 6) {
            System.out.println("Alert: " + secret);
            search.printAllSolutions();
        }
//        for (int i = 1; i <= numberOfSolutions; i++) {
//            Domain[] solution = search.getSolutionListener().getSolution(i);
//            for (int j = 0; j < solution.length; j++) {
//                System.out.print(solution[j]);
//            }
//            System.out.println();
//        }
        return numberOfSolutions;
    }

    public void makeMove(List<Integer> move) {
        System.out.println("Move: " + move);
        moves.add(move);

        Multiset<Scores.Score> score = Scores.score(secret, move);

        store.impose(scoreConstraint(move, score));

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

    private PrimitiveConstraint whiteConstraint(int colour, int pos) {
        return new XeqC(v[pos], colour);
    }

    private PrimitiveConstraint redConstraint(int colour, int pos) {
        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        for (int i = 0; i < 4; i++) {
            if (i == pos) {
                continue;
            }
            constraints.add(new XeqC(v[i], colour));
        }
        return new Or(constraints);
    }

    private PrimitiveConstraint noneConstraint(int colour, int pos) {
        return new Not(new XeqC(v[pos], colour));
    }

    private PrimitiveConstraint scoreConstraint(List<Integer> move, Multiset<Scores.Score> score) {
        while (score.size() < 4) {
            score.add(NONE);
        }
        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        for (List<Scores.Score> combo : Scores.scoreCombinations(score)) {
            ArrayList<PrimitiveConstraint> moveConstraints = Lists.newArrayList();
            for (int i = 0; i < combo.size(); i++) {
                Scores.Score s = combo.get(i);
                if (s.equals(WHITE)) {
                    moveConstraints.add(whiteConstraint(move.get(i), i));
                } else if (s.equals(RED)) {
                    moveConstraints.add(redConstraint(move.get(i), i));
                } else if (s.equals(NONE)) {
                    moveConstraints.add(noneConstraint(move.get(i), i));
                }
            }
            constraints.add(new And(moveConstraints));
        }
        return new Or(constraints);
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
                if (hasDistinctColours(move1) && hasDistinctColours(move2)) {
                    eitherDontAppearAnywhereOrBothAppearIn(oldCol, newCol, diffPosNeg);
                }
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

}
