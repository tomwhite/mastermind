package com.tom_e_white.mastermind;

import com.google.common.collect.*;
import org.jacop.constraints.*;
import org.jacop.core.Domain;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.*;

import java.io.IOException;
import java.util.*;

import static com.tom_e_white.mastermind.Scores.Score.*;
import static com.tom_e_white.mastermind.Scores.move;
import static org.junit.Assert.*;

public class Game {

    private static final int REPORT_NUM_SOLUTIONS = 2;
    private static final boolean VERBOSE = false;
    private Set<Integer> ALL_POS = ImmutableSet.copyOf(Sets.newLinkedHashSet(Lists.newArrayList(0, 1, 2, 3)));

    private List<Integer> secret;
    private Store store;
    private IntVar[] pegs;

    private Scorer scorer;
    private List<List<Integer>> moves;
    private Map<List<Integer>, Multiset<Scores.Score>> scores;

    public Game() {
    }

    public Game(List<Integer> secret) {
        this.secret = secret;
    }

    @SuppressWarnings("unchecked")
    public Result playGame(Scorer scorer) {
        moves = Lists.newArrayList();
        scores = Maps.newHashMap();
        this.scorer = scorer;
        store = new Store();
        pegs = new IntVar[4];
        for (int i = 0; i < pegs.length; i++) {
            pegs[i] = new IntVar(store, "peg" + i, 0, 5);
        }
        List<List<Integer>> staticMoves = Lists.newArrayList(
            move(0, 1, 2, 3),
            move(4, 1, 2, 3)
        );
        int moveCount = 0;
        while (moveCount < 7) {
            if (moveCount < staticMoves.size()) {
                makeMove(staticMoves.get(moveCount));
            } else {
                makeMove(search());
            }
            moveCount++;
            if (hasWon()) {
                break;
            }
        }
        int solutionsCount = countSolutions(true);
        if (!hasWon()) {
            makeMove(search());
        }
        List<Multiset<Scores.Score>> scoresList = Lists.newArrayList();
        for (List<Integer> move : moves) {
            scoresList.add(scores.get(move));
        }
        return new Result(solutionsCount, hasWon(), moves, scoresList);
    }

    private boolean hasWon() {
        return scores.get(moves.get(moves.size() - 1)).equals(ImmutableMultiset.of(WHITE, WHITE, WHITE, WHITE));
    }

    private List<Integer> search() {
        Search<IntVar> search = new DepthFirstSearch<IntVar>();
        SelectChoicePoint<IntVar> select =
                new InputOrderSelect<IntVar>(store, pegs,
                        new IndomainMin<IntVar>());

        search.setAssignSolution(false); // don't assign variables after finding a solution, http://sourceforge.net/p/jacop-solver/discussion/1220992/thread/4caf2979/
        search.getSolutionListener().searchAll(true);
        search.getSolutionListener().recordSolutions(true);
        search.setPrintInfo(false);

        boolean result = search.labeling(store, select);
        if (!result) {
            throw new IllegalStateException("No solutions found for " + secret);
        }

        List<Integer> solution = Lists.newArrayList();
        int numberOfSolutions = search.getSolutionListener().solutionsNo();
        for (int i = 1; i <= numberOfSolutions; i++) {
            solution.clear();
            Domain[] sol = search.getSolutionListener().getSolution(i);
            for (Domain d : sol) {
                solution.add(d.valueEnumeration().nextElement()); // convert domain to int
            }
            if (moves.contains(solution)) {
                continue;
            }
            break; // just return first to start with (unless already played it)
        }
        assertEquals(4, solution.size());
        return solution;
    }

    private int countSolutions(boolean verbose) {
        Search<IntVar> search = new DepthFirstSearch<IntVar>();
        SelectChoicePoint<IntVar> select =
                new InputOrderSelect<IntVar>(store, pegs,
                        new IndomainMin<IntVar>());

        search.setAssignSolution(false);
        search.getSolutionListener().searchAll(true);
        search.getSolutionListener().recordSolutions(true);
        search.setPrintInfo(false);

        boolean result = search.labeling(store, select);
        if (!result) {
            throw new IllegalStateException("No solutions found for " + secret);
        }
        int numberOfSolutions = search.getSolutionListener().solutionsNo();
        if (numberOfSolutions == REPORT_NUM_SOLUTIONS && verbose) {
            reportGame(search);
        }
        return numberOfSolutions;
    }

    private void reportGame(Search<IntVar> search) {
        System.out.println("Report: " + secret);
        for (List<Integer> move : moves) {
            System.out.println(move + "; " + scores.get(move));
        }
        search.printAllSolutions();
    }

    private int makeMove(List<Integer> move) {
        moves.add(move);

        Multiset<Scores.Score> score = scorer.score(move);
        scores.put(move, score);

        impose(scoreConstraint(move, score));

        if (moves.size() > 1) {
            for (List<Integer> previousMove : moves) {
                imposeDiffConstraints(previousMove, move);
                //imposeColourConstraints(previousMove, move);
            }
        }
        return countSolutions(false);
    }

    /**
     * Impose a constraint.
     */
    private void impose(PrimitiveConstraint constraint) {
        assertConstraint(constraint);
        store.impose(constraint);
    }

    /**
     * Colour appears in given position
     */
    private PrimitiveConstraint whiteConstraint(int colour, int pos) {
        return new XeqC(pegs[pos], colour);
    }

    /**
     * Colour does not appear in given position but does appear in another position
     */
    private PrimitiveConstraint redConstraint(int colour, int pos) {
        return redConstraint(colour, pos, ALL_POS);
    }

    /**
     * Colour does not appear in given position but does appear in another position (restricted from full set of positions)
     */
    private PrimitiveConstraint redConstraint(int colour, int pos, Set<Integer> possiblePos) {
        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        for (int i : possiblePos) {
            if (i == pos) {
                continue;
            }
            constraints.add(new XeqC(pegs[i], colour));
        }
        return new And(noneConstraint(colour, pos), new Or(constraints));
    }

    /**
     * Colour does not appear in given position
     */
    private PrimitiveConstraint noneConstraint(int colour, int pos) {
        return new Not(new XeqC(pegs[pos], colour));
    }

    /**
     * Colour does not appear in any position
     */
    private PrimitiveConstraint noneConstraint(int colour) {
        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        for (int i : ALL_POS) {
            constraints.add(noneConstraint(colour, i));
        }
        return new And(constraints);
    }

    /**
     * A constraint for a normal move.
     */
    private PrimitiveConstraint scoreConstraint(List<Integer> move, Multiset<Scores.Score> score) {
        return scoreConstraint(move, score, ALL_POS);
    }

    /**
     * A constraint for a subset of the positions in a move.
     */
    private PrimitiveConstraint scoreConstraint(List<Integer> move, Multiset<Scores.Score> score, Set<Integer> positions) {
        while (score.size() < positions.size()) {
            score.add(NONE);
        }
//        if (score.count(NONE) == 4) {
//            ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
//            for (int i : positions) {
//                constraints.add(noneConstraint(move.get(i)));
//            }
//            return new And(constraints);
//        }
        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        for (List<Scores.Score> combo : Scores.scoreCombinations(score)) {
            ArrayList<PrimitiveConstraint> moveConstraints = Lists.newArrayList();
            Set<Integer> possiblePos = Sets.newLinkedHashSet(Lists.newArrayList(0, 1, 2, 3));
            int offset = 0;
            for (int i : positions) {
                Scores.Score s = combo.get(offset);
                if (s.equals(WHITE)) {
                    moveConstraints.add(whiteConstraint(move.get(i), i));
                    possiblePos.remove(i); // red can't actually appear where white does
                } else if (s.equals(NONE)) {
                    if (hasDistinctColours(move)) {
                        moveConstraints.add(noneConstraint(move.get(i)));
                    } else {
                        moveConstraints.add(noneConstraint(move.get(i), i));
                    }
                }
                offset++;
            }
            offset = 0;
            for (int i : positions) {
                Scores.Score s = combo.get(offset);
                if (s.equals(RED)) {
                    moveConstraints.add(redConstraint(move.get(i), i, possiblePos));
                }
                offset++;
            }
            constraints.add(new And(moveConstraints));
        }
        return new Or(constraints);
    }

    /**
     * This ensures that we don't add a constraint that is false by failing immediately
     */
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
            for (IntVar peg : pegs) {
                if (x == peg) {
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
        for (int i : ALL_POS) {
            if (!move1.get(i).equals(move2.get(i))) {
                positions.add(i);
            }
        }
        return positions;
    }

    private boolean hasDistinctColours(List<Integer> move) {
        return Sets.newHashSet(move).size() == 4;
    }

    private void imposeDiffConstraints(List<Integer> move1, List<Integer> move2) {
        Set<Integer> diff = diff(move1, move2);
        if (VERBOSE) {
            System.out.println(secret);
            System.out.println(move1 + "; " + scores.get(move1));
            System.out.println(move2 + "; " + scores.get(move2));
        }
        if (diff.size() == 1) {
            imposeDiff1Constraints(move1, move2, diff);
        } else if (diff.size() == 2) {
            imposeDiff2Constraints(move1, move2, diff);
        }
    }
    private void imposeDiff1Constraints(List<Integer> move1, List<Integer> move2, Set<Integer> diff) {
        int diffPos = Iterables.getOnlyElement(diff);
        
        Multiset<Scores.Score> score1 = scores.get(move1);
        Multiset<Scores.Score> score2 = scores.get(move2);

        Scores.ScoreDelta scoreDelta = Scores.scoreDelta(score1, score2);
        int rd = scoreDelta.getRedDelta();
        int wd = scoreDelta.getWhiteDelta();
        int oldCol = move1.get(diffPos);
        int newCol = move2.get(diffPos);
        if (wd == 0) {
            impose(noneConstraint(oldCol, diffPos)); // oldCol does not appear in diffPos
            impose(noneConstraint(newCol, diffPos)); // newCol does not appear in diffPos
            if (rd == 0) {
                // TODO: can investigate this better
                if (hasDistinctColours(move1) && hasDistinctColours(move2)) {
                    // either oldCol and newCol don't appear anywhere, or they are both reds in diffPos
                    PrimitiveConstraint c = new And(noneConstraint(oldCol), noneConstraint(newCol));
                    impose(new Or(new And(redConstraint(oldCol, diffPos), redConstraint(newCol, diffPos)), c));
                }
            } else if (rd == 1) {
                impose(redConstraint(newCol, diffPos));
                impose(noneConstraint(oldCol));
            } else if (rd == -1) {
                impose(redConstraint(oldCol, diffPos));
                impose(noneConstraint(newCol));
            }
        } else if (wd == 1) {
            impose(whiteConstraint(newCol, diffPos));
            impose(noneConstraint(oldCol, diffPos));
            if (hasDistinctColours(move1) && hasDistinctColours(move2)) {
                if (rd == 0) {
                    impose(noneConstraint(oldCol));
                } else if (rd == -1) {
                    impose(redConstraint(oldCol, diffPos));
                }
            }
        } else if (wd == -1) {
            impose(whiteConstraint(oldCol, diffPos));
            impose(noneConstraint(newCol, diffPos));
            if (hasDistinctColours(move1) && hasDistinctColours(move2)) {
                if (rd == 0) {
                    impose(noneConstraint(newCol));
                } else if (rd == 1) {
                    impose(redConstraint(newCol, diffPos));
                }
            }
        }
    }

    private void imposeDiff2Constraints(List<Integer> move1, List<Integer> move2, Set<Integer> diff) {
        Multiset<Scores.Score> score1 = scores.get(move1);
        Multiset<Scores.Score> score2 = scores.get(move2);

        Scores.ScoreDelta scoreDelta = Scores.scoreDelta(score1, score2);

        int rc1 = score1.count(RED);
        int rc2 = score2.count(RED);
        int wd = scoreDelta.getWhiteDelta();

        // if number of whites changes (with no reds), then we know that two non-white pegs don't appear in either of the
        // two non-white positions
        // e.g. for the following we know that 5 and 3 don't appear in positions 1 and 3
        // [4, 5, 2, 3]; [WHITE x 2, NONE x 2]
        // [4, 0, 2, 0]; [WHITE x 3, NONE]
        if (rc1 == 0 && rc2 == 0) {
            if (wd > 0) {
                for (int i : diff) {
                    int col = move1.get(i);
                    for (int j : diff) {
                        impose(noneConstraint(col, j));
                    }
                }
            } else if (wd < 0) {
                for (int i : diff) {
                    int col = move2.get(i);
                    for (int j : diff) {
                        impose(noneConstraint(col, j));
                    }
                }
            }
        }
    }

    private Set<Integer> colourSet(List<Integer> move) {
        return Sets.newLinkedHashSet(move);
    }
    
    private void imposeColourConstraints(List<Integer> move1, List<Integer> move2) {
        // TODO: this fails for the following - it states that neither 1 nor 2 appear, but 2 does appear
        // [0, 0, 2, 2]
        // [0, 1, 2, 3]; [WHITE x 2, NONE x 2]
        // [0, 0, 0, 3]; [WHITE x 2, NONE x 2]
        // However, it needs to work for the following (3 doesn't appear)
        // [5, 5, 1, 2]
        // [1, 5, 1, 2]; [WHITE x 3, NONE]
        // [1, 5, 3, 2]; [RED, WHITE x 2, NONE]
        Multiset<Scores.Score> score1 = scores.get(move1);
        Multiset<Scores.Score> score2 = scores.get(move2);
        if (score1.count(WHITE) + score1.count(RED) == score2.count(WHITE) + score2.count(RED)) { // same number of colours in both moves
            Set<Integer> cols1 = colourSet(move1);
            Set<Integer> cols2 = colourSet(move2);
            if (cols1.containsAll(cols2)) {
                for (int col : Sets.difference(cols1, cols2)) {
                    impose(noneConstraint(col));
                }
            } else if (cols2.containsAll(cols1)) {
                for (int col : Sets.difference(cols2, cols1)) {
                    impose(noneConstraint(col));
                }

            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) throws IOException {
        System.out.println("MASTERMIND");
        System.out.println("**********");
        System.out.println("Choose a secret combination of four pegs, then press enter. It's helpful to play along with a real set.");
        System.in.read();
        System.out.println("OK. I will try to deduce the four pegs you have chosen.");

        Scorer scorer = new HumanScorer();
        Game game = new Game();
        Result result = game.playGame(scorer);
        if (result.hasWon()) {
            System.out.println("I won! Thanks for playing.");
        } else {
            System.out.println("You won! Congratulations.");
        }
    }

}
