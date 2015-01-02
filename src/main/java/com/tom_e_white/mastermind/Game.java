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

/**
 * Plays a game of Mastermind.
 */
public class Game {

    private static final int MAX_MOVES = 8;
    public static final int NUM_POSITIONS = 4;
    private static final Set<Integer> ALL_POS = ImmutableSet.copyOf(Sets.newLinkedHashSet(Lists.newArrayList(0, 1, 2, 3)));

    private static final int REPORT_NUM_SOLUTIONS = 0;
    private static final boolean VERBOSE = false;
    
    private Scorer scorer;
    private List<List<Integer>> moves;
    private Map<List<Integer>, Multiset<Scores.Score>> scores;
    private Store store;
    protected IntVar[] pegs;

    @SuppressWarnings("unchecked")
    public Result play(Scorer scorer) {
        this.scorer = scorer;
        moves = Lists.newArrayList();
        scores = Maps.newHashMap();
        store = new Store();
        pegs = new IntVar[NUM_POSITIONS];
        for (int pos = 0; pos < pegs.length; pos++) {
            pegs[pos] = new IntVar(store, "peg" + pos, 0, 5);
        }
        List<List<Integer>> staticMoves = Lists.newArrayList(
            move(0, 1, 2, 3),
            move(4, 1, 2, 3)
        );
        int moveCount = 0;
        while (moveCount < MAX_MOVES - 1) {
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

    /**
     * Find the first solution and return it.
     */
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
            throw new IllegalStateException("No solutions found for " + this);
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
            break;
        }
        return solution;
    }

    /**
     * Return the number of possible solutions at this point in the game.
     */
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
            throw new IllegalStateException("No solutions found for " + this);
        }
        int numberOfSolutions = search.getSolutionListener().solutionsNo();
        if (numberOfSolutions == REPORT_NUM_SOLUTIONS && verbose) {
            reportGame(search);
        }
        return numberOfSolutions;
    }

    private void reportGame(Search<IntVar> search) {
        System.out.println("Report: " + this);
        for (List<Integer> move : moves) {
            System.out.println(move + "; " + scores.get(move));
        }
        search.printAllSolutions();
    }

    /**
     * Make the given move, and return the number of solutions.
     */
    private int makeMove(List<Integer> move) {
        moves.add(move);

        Multiset<Scores.Score> score = scorer.score(move);
        scores.put(move, score);

        impose(scoreConstraint(move, score));

        if (moves.size() > 1) {
            for (List<Integer> previousMove : moves) {
                imposeDiffConstraints(previousMove, move);
            }
        }
        return countSolutions(false);
    }

    /**
     * Impose a constraint.
     */
    protected void impose(PrimitiveConstraint constraint) {
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
        for (int p : possiblePos) {
            if (p == pos) {
                continue;
            }
            constraints.add(new XeqC(pegs[p], colour));
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
        for (int pos : ALL_POS) {
            constraints.add(noneConstraint(colour, pos));
        }
        return new And(constraints);
    }

    /**
     * A constraint for a normal move.
     */
    private PrimitiveConstraint scoreConstraint(List<Integer> move, Multiset<Scores.Score> score) {
        while (score.size() < ALL_POS.size()) {
            score.add(NONE);
        }
        // if no whites or reds then none of the colours appear anywhere
        if (score.count(NONE) == NUM_POSITIONS) {
            ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
            for (int pos : ALL_POS) {
                constraints.add(noneConstraint(move.get(pos)));
            }
            return new And(constraints);
        }
        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        for (List<Scores.Score> combo : Scores.scoreCombinations(score)) {
            ArrayList<PrimitiveConstraint> moveConstraints = Lists.newArrayList();
            Set<Integer> possibleRedPos = Sets.newLinkedHashSet(Lists.newArrayList(0, 1, 2, 3));
            int offset = 0;
            for (int pos : ALL_POS) {
                Scores.Score s = combo.get(offset);
                int col = move.get(pos);
                if (s.equals(WHITE)) {
                    moveConstraints.add(whiteConstraint(col, pos));
                    possibleRedPos.remove(pos); // red can't actually appear where white does
                } else if (s.equals(NONE)) {
                    if (hasDistinctColours(move)) {
                        // if move's colours are all different then col doesn't appear anywhere
                        moveConstraints.add(noneConstraint(col));
                    } else {
                        moveConstraints.add(noneConstraint(col, pos));
                    }
                }
                offset++;
            }
            offset = 0;
            for (int pos : ALL_POS) {
                Scores.Score s = combo.get(offset);
                if (s.equals(RED)) {
                    moveConstraints.add(redConstraint(move.get(pos), pos, possibleRedPos));
                }
                offset++;
            }
            constraints.add(new And(moveConstraints));
        }
        return new Or(constraints);
    }

    private Set<Integer> diff(List<Integer> move1, List<Integer> move2) {
        Set<Integer> positions = Sets.newHashSet();
        for (int pos : ALL_POS) {
            if (!move1.get(pos).equals(move2.get(pos))) {
                positions.add(pos);
            }
        }
        return positions;
    }

    private boolean hasDistinctColours(List<Integer> move) {
        return Sets.newHashSet(move).size() == NUM_POSITIONS;
    }

    private void imposeDiffConstraints(List<Integer> move1, List<Integer> move2) {
        Set<Integer> diff = diff(move1, move2);
        if (VERBOSE) {
            System.out.println(this);
            System.out.println(move1 + "; " + scores.get(move1));
            System.out.println(move2 + "; " + scores.get(move2));
        }
        if (diff.size() == 1) {
            imposeDiff1Constraints(move1, move2, diff);
        } else if (diff.size() == 2) {
            // gives a slight improvement, but not strictly needed
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
            if (rd == 1) {
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) throws IOException {
        System.out.println("MASTERMIND");
        System.out.println("**********");
        System.out.println("Choose a secret combination of four pegs, then press enter. It's helpful to play along with a real set.");
        System.in.read();
        System.out.println("OK. I will try to deduce the four pegs you have chosen.");

        Scorer scorer = new HumanScorer();
        Game game = new Game();
        Result result = game.play(scorer);
        if (result.hasWon()) {
            System.out.println("I won! Thanks for playing.");
        } else {
            System.out.println("You won! Congratulations.");
        }
    }

}
