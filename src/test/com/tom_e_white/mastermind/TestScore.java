package com.tom_e_white.mastermind;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.tom_e_white.mastermind.Score.Peg.*;
import static com.tom_e_white.mastermind.Score.score;
import static org.junit.Assert.assertEquals;

public class TestScore {

    @Test
    public void testScore() {
        assertEquals(new Score(WHITE, WHITE, WHITE, WHITE),
                score(move(0, 1, 2, 3), move(0, 1, 2, 3)));
        assertEquals(new Score(WHITE, WHITE, WHITE),
                score(move(0, 1, 2, 2), move(0, 1, 2, 3)));
        assertEquals(new Score(WHITE, WHITE),
                score(move(0, 2, 2, 2), move(0, 1, 2, 3)));
        assertEquals(new Score(WHITE),
                score(move(2, 2, 2, 2), move(0, 1, 2, 3)));
        assertEquals(new Score(RED, RED, WHITE),
                score(move(0, 2, 2, 3), move(1, 2, 3, 2)));
        assertEquals(new Score(RED, RED, RED),
                score(move(0, 2, 2, 3), move(1, 0, 3, 2)));
        assertEquals(new Score(),
                score(move(0, 1, 2, 2), move(3, 3, 4, 5)));
    }

    @Test
    public void testPermutations() {
        assertEquals(set(Lists.newArrayList(NONE, NONE, NONE, NONE)),
                new Score(NONE, NONE, NONE, NONE).permutations());
        assertEquals(set(Lists.newArrayList(WHITE, NONE, NONE, NONE), Lists.newArrayList(NONE, WHITE, NONE, NONE),
                        Lists.newArrayList(NONE, NONE, WHITE, NONE), Lists.newArrayList(NONE, NONE, NONE, WHITE)),
                new Score(WHITE, NONE, NONE, NONE).permutations());
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
                new Score(WHITE, RED, NONE, NONE).permutations());
    }
    
    private Move move(int a, int b, int c, int d) {
        return new Move(a, b, c, d);
    }

    private Set<List<Score.Peg>> set(List<Score.Peg>... scores) {
        Set s = new LinkedHashSet<List<Score.Peg>>();
        for (List<Score.Peg> score : scores) {
            s.add(score);
        }
        return s;
    }

}
