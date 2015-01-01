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

    private List<Integer> secret;
    private Store store;
    private IntVar[] v;

    private Scorer scorer;
    private List<List<Integer>> moves;
    private Map<List<Integer>, Multiset<Scores.Score>> scores;

    public Game() {
    }

    public Game(List<Integer> secret) {
        this.secret = secret;
    }

    public Result playGame(Scorer scorer) {
        moves = Lists.newArrayList();
        scores = Maps.newHashMap();
        this.scorer = scorer;
        store = new Store();
        v = new IntVar[4];
        for (int i = 0; i < v.length; i++) {
            v[i] = new IntVar(store, "v" + i, 0, 5);
        }

        List<List<Integer>> staticMoves = Lists.newArrayList(
                move(0, 1, 2, 3),
                move(4, 1, 2, 3),
                move(4, 5, 2, 3), // TODO: or 0, 5, 2, 3 if first was better than second?
                move(4, 5, 0, 3)
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
            solution.clear();
            Domain[] sol = search.getSolutionListener().getSolution(i);
            for (int j = 0; j < sol.length; j++) {
                solution.add(sol[j].valueEnumeration().nextElement()); // convert domain to int
            }
            if (moves.contains(solution)) {
                continue;
            }
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

    private int countSolutions(boolean verbose) {
        Search<IntVar> search = new DepthFirstSearch<IntVar>();
        SelectChoicePoint<IntVar> select =
                new InputOrderSelect<IntVar>(store, v,
                        new IndomainMin<IntVar>());

        search.setAssignSolution(false);
        search.getSolutionListener().searchAll(true);
        search.getSolutionListener().recordSolutions(true);
        search.setPrintInfo(false);

        boolean result = search.labeling(store, select);

        int numberOfSolutions = search.getSolutionListener().solutionsNo();
        if (numberOfSolutions == 0) {
            throw new IllegalStateException("Zero solutions for " + secret);
        }
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

        if (moves.size() <= 1) {
            return countSolutions(false);
        }

        for (List<Integer> previousMove : moves) {
            Set<Integer> diff = diff(previousMove, move);
//            System.out.println(secret);
//            System.out.println(previousMove + "; " + scores.get(previousMove));
//            System.out.println(move + "; " + scores.get(move));
            if (diff.size() == 1) {
                imposeDiff1Constraints(previousMove, move, Iterables.getOnlyElement(diff));
            } else if (diff.size() == 2) {
                imposeDiff2Constraints(previousMove, move, diff);
            } else if (diff.size() == 3) {
                imposeDiff3Constraints(previousMove, move, diff);
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
        return new XeqC(v[pos], colour);
    }

    /**
     * Colour does not appear in given position but does appear in another position
     */
    private PrimitiveConstraint redConstraint(int colour, int pos) {
        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        for (int i = 0; i < 4; i++) {
            if (i == pos) {
                continue;
            }
            constraints.add(new XeqC(v[i], colour));
        }
        return new And(noneConstraint(colour, pos), new Or(constraints));
    }

    /**
     * Colour does not appear in given position
     */
    private PrimitiveConstraint noneConstraint(int colour, int pos) {
        return new Not(new XeqC(v[pos], colour));
    }

    /**
     * Colour does not appear in any position
     */
    private PrimitiveConstraint noneConstraint(int colour) {
        ArrayList<PrimitiveConstraint> constraints = Lists.newArrayList();
        for (int i = 0; i < 4; i++) {
            constraints.add(noneConstraint(colour, i));
        }
        return new And(constraints);
    }

    private Set<Integer> ALL_POS = Sets.newLinkedHashSet(Lists.newArrayList(0, 1, 2, 3));

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

    private void imposeDiff1Constraints(List<Integer> move1, List<Integer> move2, int diffPos) {
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

        int rd = scoreDelta.getRedDelta();
        int wd = scoreDelta.getWhiteDelta();

        PrimitiveConstraint constraint = null;
        if (wd == 0) {
            // TODO: this is not strictly correct, consider the following where RED is not pos 0=3 or 3=2
            // [3, 5, 5, 0]
            // [2, 5, 0, 0]; [WHITE x 2]
            // [3, 5, 0, 2]; [RED, WHITE x 2]
//            if (rd == 1) {
//                constraint = scoreConstraint(move2, HashMultiset.create(Lists.newArrayList(RED, IGNORE)), diff);
//            } else if (rd == -1) {
//                constraint = scoreConstraint(move1, HashMultiset.create(Lists.newArrayList(RED, IGNORE)), diff);
//            } else if (rd == 2) {
//                constraint = scoreConstraint(move2, HashMultiset.create(Lists.newArrayList(RED, RED)), diff);
//            } else if (rd == -2) {
//                constraint = scoreConstraint(move1, HashMultiset.create(Lists.newArrayList(RED, RED)), diff);
//            }
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
            impose(constraint);
        }
    }

    private void imposeDiff3Constraints(List<Integer> move1, List<Integer> move2, Set<Integer> diff) {
        Multiset<Scores.Score> score1 = scores.get(move1);
        Multiset<Scores.Score> score2 = scores.get(move2);

        Scores.ScoreDelta scoreDelta = Scores.scoreDelta(score1, score2);

        int rd = scoreDelta.getRedDelta();
        int wd = scoreDelta.getWhiteDelta();

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
            impose(constraint);
        }
    }

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
