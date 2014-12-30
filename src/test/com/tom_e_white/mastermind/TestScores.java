package com.tom_e_white.mastermind;

import com.google.common.collect.*;
import org.jacop.constraints.*;
import org.jacop.core.Domain;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.*;
import org.junit.Test;

import java.util.*;

import static com.tom_e_white.mastermind.Scores.Score.*;
import static com.tom_e_white.mastermind.Scores.move;
import static com.tom_e_white.mastermind.Scores.score;
import static org.junit.Assert.*;

public class TestScores {

    private List<Integer> secret;
    private Store store;
    private IntVar[] v;

    private Scorer scorer;
    private List<List<Integer>> moves;
    private Map<List<Integer>, Multiset<Scores.Score>> scores;

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
                        hist.add(playGame(new ComputerScorer(secret)));
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

    public int playGame(Scorer scorer) {
        //System.out.println("New Game");

        moves = Lists.newArrayList();
        scores = Maps.newHashMap();
        this.scorer = scorer;
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
        // TODO: or, if first move is 1 red or less, then switch numbers (don't use as many 2s or 3s)
        makeMove(move(0, 1, 2, 3));
        makeMove(move(4, 1, 2, 3));
        makeMove(move(4, 5, 2, 3));
        makeMove(move(4, 5, 0, 3));
        makeMove(search());
        makeMove(search());
        makeMove(search());

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
        // TODO: look for a solution that is close to others in set (not previous moves)
        // TODO: or, for first search try to start with a move that is different to previous ones
        // (doesn't repeat colours in positions that have been tried before) - i.e. aim to get some whites
        int numberOfSolutions = search.getSolutionListener().solutionsNo();
        for (int i = 1; i <= numberOfSolutions; i++) {
            solution.clear();
            Domain[] sol = search.getSolutionListener().getSolution(i);
            for (int j = 0; j < sol.length; j++) {
                solution.add(sol[j].valueEnumeration().nextElement()); // convert domain to int
            }
            if (moves.contains(solution)) {
                continue;
            }
            //System.out.println("Min dist: " + minDist(solution));
            //System.out.println("Distinct?: " + hasDistinctColours(solution));
            break; // just return first to start with (unless already played it)
        }
        assertEquals(4, solution.size());
        return solution;
    }

    private int minDist(List<Integer> move) {
        int min = Integer.MAX_VALUE;
        for (List<Integer> previousMove : moves) {
            int dist = dist(previousMove, move);
            if (dist < min) {
                min = dist;
            }
        }
        return min;
    }

    private int weight(List<Integer> move) {
        return (minDist(move) == 1 ? 1 : 0) +
                 (hasDistinctColours(move) ? 1 : 0);
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
            reportGame(search);
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

    private void reportGame(Search<IntVar> search) {
        System.out.println("Report: " + secret);
        for (List<Integer> move : moves) {
            System.out.println(move + "; " + scores.get(move));
        }
        search.printAllSolutions();
    }

    public void makeMove(List<Integer> move) {
        //System.out.println("Move: " + move);
        moves.add(move);

        Multiset<Scores.Score> score = scorer.score(move);
        scores.put(move, score);

        PrimitiveConstraint constraint = scoreConstraint(move, score);
        assertConstraint(constraint);
        store.impose(constraint);

        if (moves.size() <= 1) {
            return;
        }

        for (List<Integer> previousMove : moves) {
            Set<Integer> diff = diff(previousMove, move);
            if (diff.size() == 1) {
                reportScoreDeltaFor(previousMove, move, Iterables.getOnlyElement(diff));
            } else if (diff.size() == 2) {
                reportScoreDeltaFor(previousMove, move, diff);
            } else if (diff.size() == 3) {
                reportScoreDeltaFor3(previousMove, move, diff);
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

    private Set<Integer> ALL_POS = Sets.newLinkedHashSet(Lists.newArrayList(0, 1, 2, 3));

    private PrimitiveConstraint scoreConstraint(List<Integer> move, Multiset<Scores.Score> score) {
        return scoreConstraint(move, score, ALL_POS);
    }

    private PrimitiveConstraint scoreConstraint(List<Integer> move, Multiset<Scores.Score> score, Set<Integer> positions) {
        while (score.size() < positions.size()) {
            score.add(NONE);
        }
        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        for (List<Scores.Score> combo : Scores.scoreCombinations(score)) {
            ArrayList<PrimitiveConstraint> moveConstraints = Lists.newArrayList();
            int offset = 0;
            for (int i : positions) {
                Scores.Score s = combo.get(offset);
                if (s.equals(WHITE)) {
                    moveConstraints.add(whiteConstraint(move.get(i), i));
                } else if (s.equals(RED)) {
                    moveConstraints.add(redConstraint(move.get(i), i));
                } else if (s.equals(NONE)) {
                    moveConstraints.add(noneConstraint(move.get(i), i));
                }
                offset++;
            }
            constraints.add(new And(moveConstraints));
        }
        return new Or(constraints);
    }

    // This ensures that we don't add a constraint that is false by failing immediately
    private void assertConstraint(PrimitiveConstraint c) {
        if (secret == null) {
            return;
        }
        assertTrue(c.toString(), constraintToExpr(c));
    }

    private boolean constraintToExpr(PrimitiveConstraint c) {
        if (c instanceof XeqC) {
            IntVar x = ((XeqC) c).x;
            int i = 0;
            for (IntVar var : v) {
                if (x == var) {
                    return secret.get(i) == ((XeqC) c).c;
                }
                i++;
            }
            fail("Illegal");
        }
        if (c instanceof Not) {
            return !constraintToExpr(((Not) c).c);
        }
        if (c instanceof Or) {
            boolean ret = false;
            for (PrimitiveConstraint pc : ((Or) c).listOfC) {
                ret |= constraintToExpr(pc);
            }
            return ret;
        }
        if (c instanceof And) {
            boolean ret = true;
            for (PrimitiveConstraint pc : ((And) c).listOfC) {
                ret &= constraintToExpr(pc);
            }
            return ret;
        }
        fail("Illegal");
        return false;
    }

    private Set<Integer> diff(List<Integer> move1, List<Integer> move2) {
        Set<Integer> positions = Sets.newHashSet();
        for (int i = 0; i < 4; i++) {
            if (move1.get(i) != move2.get(i)) {
                positions.add(i);
            }
        }
        return positions;
    }

    private int dist(List<Integer> move1, List<Integer> move2) {
        int dist = 0;
        for (int i = 0; i < 4; i++) {
            if (move1.get(i) != move2.get(i)) {
                dist++;
            }
        }
        return dist;
    }

    private boolean hasDistinctColours(List<Integer> move) {
        return Sets.newHashSet(move).size() == 4;
    }

    public void reportScoreDeltaFor(List<Integer> move1, List<Integer> move2, Set<Integer> diff) {
        Multiset<Scores.Score> score1 = scores.get(move1);
        Multiset<Scores.Score> score2 = scores.get(move2);

        Scores.ScoreDelta scoreDelta = Scores.scoreDelta(score1, score2);

        int rd = scoreDelta.getRedDelta();
        int wd = scoreDelta.getWhiteDelta();

//        System.out.println(secret);
//        System.out.println(move1 + "; " + score1);
//        System.out.println(move2 + "; " + score2);

        PrimitiveConstraint constraint = null;
        if (wd == 0) {
            // TODO: this is not strictly correct, consider the following where RED is not pos 0=3 or 3=2
            // [3, 5, 5, 0]
            // [2, 5, 0, 0]; [WHITE x 2]
            // [3, 5, 0, 2]; [RED, WHITE x 2]
            if (rd == 1) {
                constraint = scoreConstraint(move2, HashMultiset.create(Lists.newArrayList(RED, IGNORE)), diff);
            } else if (rd == -1) {
                constraint = scoreConstraint(move1, HashMultiset.create(Lists.newArrayList(RED, IGNORE)), diff);
            } else if (rd == 2) {
                constraint = scoreConstraint(move2, HashMultiset.create(Lists.newArrayList(RED, RED)), diff);
            } else if (rd == -2) {
                constraint = scoreConstraint(move1, HashMultiset.create(Lists.newArrayList(RED, RED)), diff);
            }
        } else if (wd == 1) {
            constraint = scoreConstraint(move2, HashMultiset.create(Lists.newArrayList(WHITE)), diff);
        } else if (wd == -1) {
            constraint = scoreConstraint(move1, HashMultiset.create(Lists.newArrayList(WHITE)), diff);
        } else if (wd == 2) {
            constraint = scoreConstraint(move2, HashMultiset.create(Lists.newArrayList(WHITE, WHITE)), diff);
        } else if (wd == -2) {
            constraint = scoreConstraint(move1, HashMultiset.create(Lists.newArrayList(WHITE, WHITE)), diff);
        }

        if (constraint != null) {
            assertConstraint(constraint);
            store.impose(constraint);
        }
    }

    public void reportScoreDeltaFor3(List<Integer> move1, List<Integer> move2, Set<Integer> diff) {
        Multiset<Scores.Score> score1 = scores.get(move1);
        Multiset<Scores.Score> score2 = scores.get(move2);

        Scores.ScoreDelta scoreDelta = Scores.scoreDelta(score1, score2);

        int rd = scoreDelta.getRedDelta();
        int wd = scoreDelta.getWhiteDelta();

//        System.out.println(secret);
//        System.out.println(move1 + "; " + score1);
//        System.out.println(move2 + "; " + score2);

        PrimitiveConstraint constraint = null;
        if (wd == 0) {
            if (rd == 1) {
                constraint = scoreConstraint(move2, HashMultiset.create(Lists.newArrayList(RED, IGNORE, IGNORE)), diff);
            } else if (rd == -1) {
                constraint = scoreConstraint(move1, HashMultiset.create(Lists.newArrayList(RED, IGNORE, IGNORE)), diff);
            } else if (rd == 2) {
                constraint = scoreConstraint(move2, HashMultiset.create(Lists.newArrayList(RED, RED, IGNORE)), diff);
            } else if (rd == -2) {
                constraint = scoreConstraint(move1, HashMultiset.create(Lists.newArrayList(RED, RED, IGNORE)), diff);
            } else if (rd == 3) {
                constraint = scoreConstraint(move2, HashMultiset.create(Lists.newArrayList(RED, RED, RED)), diff);
            } else if (rd == -3) {
                constraint = scoreConstraint(move1, HashMultiset.create(Lists.newArrayList(RED, RED, RED)), diff);
            }
        } else if (wd == 1) {
            constraint = scoreConstraint(move2, HashMultiset.create(Lists.newArrayList(WHITE, IGNORE)), diff);
        } else if (wd == -1) {
            constraint = scoreConstraint(move1, HashMultiset.create(Lists.newArrayList(WHITE, IGNORE)), diff);
        } else if (wd == 2) {
            constraint = scoreConstraint(move2, HashMultiset.create(Lists.newArrayList(WHITE, WHITE)), diff);
        } else if (wd == -2) {
            constraint = scoreConstraint(move1, HashMultiset.create(Lists.newArrayList(WHITE, WHITE)), diff);
        } else if (wd == 3) {
            constraint = scoreConstraint(move2, HashMultiset.create(Lists.newArrayList(WHITE, WHITE, WHITE)), diff);
        } else if (wd == -3) {
            constraint = scoreConstraint(move1, HashMultiset.create(Lists.newArrayList(WHITE, WHITE, WHITE)), diff);
        }

        if (constraint != null) {
            assertConstraint(constraint);
            store.impose(constraint);
        }
    }

    public void reportScoreDeltaFor(List<Integer> move1, List<Integer> move2, int diffPos) {
        Set<Integer> diffPosNeg = Sets.newTreeSet(Sets.newHashSet(0, 1, 2, 3));
        diffPosNeg.remove(diffPos);

        Multiset<Scores.Score> score1 = scores.get(move1);
        Multiset<Scores.Score> score2 = scores.get(move2);

        Scores.ScoreDelta scoreDelta = Scores.scoreDelta(score1, score2);
//        System.out.println(secret);
//        System.out.println(move1 + "; " + score1);
//        System.out.println(move2 + "; " + score2);
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
        //System.out.println();
    }

    void appearsIn(int colour, int pos) {
        //System.out.println(colour + " appears in pos " + pos);
        if (secret != null) {
            assertEquals(colour + " appears in pos " + pos, (long) secret.get(pos), colour);
        }
        store.impose(new XeqC(v[pos], colour));
        //System.out.println("TODO: no need to mutate " + pos + " again");
    }

    void doesNotAppearIn(int colour, int pos) {
        //System.out.println(colour + " does not appear in " + pos);
        if (secret != null) {
            assertFalse(colour + " does not appear in " + pos, secret.get(pos).equals(colour));
        }
        store.impose(new Not(new XeqC(v[pos], colour)));
    }

    void appearsIn(int colour, Set<Integer> positions) {
        //System.out.println(colour + " appears in one of pos " + positions);
        store.impose(appearsInConstraint(colour, positions));
        //System.out.println("TODO: we know " + colour + " appears so try it again (in one of " + positions + ")");

        if (secret != null) {
            boolean contains = false;
            for (int i : positions) {
                contains = contains || secret.get(i).equals(colour);
            }
            assertTrue(colour + " appears in one of pos " + positions, contains);
        }
    }

    void doesNotAppearAnywhere(int colour) {
        //System.out.println(colour + " does not appear anywhere");
        if (secret != null) {
            assertFalse(colour + " does not appear anywhere", secret.contains(colour));
        }
        store.impose(new Not(new XeqC(v[0], colour)));
        store.impose(new Not(new XeqC(v[1], colour)));
        store.impose(new Not(new XeqC(v[2], colour)));
        store.impose(new Not(new XeqC(v[3], colour)));
        //System.out.println("TODO: no need to try " + colour + " again");
    }

    void eitherDontAppearAnywhereOrBothAppearIn(int col1, int col2, Set<Integer> positions) {
        //System.out.println("EITHER " + col1 + " and " + col2 + " don't appear anywhere OR " + col1 + " and " + col2 + " both appear in pos " + positions);

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
