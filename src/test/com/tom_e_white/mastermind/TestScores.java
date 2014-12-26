package com.tom_e_white.mastermind;

import com.google.common.collect.ImmutableMultiset;
import org.junit.Test;

import static com.tom_e_white.mastermind.Scores.Score.RED;
import static com.tom_e_white.mastermind.Scores.Score.WHITE;
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
}
