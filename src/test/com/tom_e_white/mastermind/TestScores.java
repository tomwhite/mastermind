package com.tom_e_white.mastermind;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.tom_e_white.mastermind.Scores.Score.*;
import static com.tom_e_white.mastermind.Scores.move;
import static com.tom_e_white.mastermind.Scores.score;
import static org.junit.Assert.assertEquals;

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

}
