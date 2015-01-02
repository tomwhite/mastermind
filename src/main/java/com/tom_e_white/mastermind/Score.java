package com.tom_e_white.mastermind;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.tom_e_white.mastermind.Score.Peg.*;

/**
 * A score represents white and red pegs (in no particular order) to indicate how well a move matches the answer.
 */
public class Score {
    
    public enum Peg {
        RED,
        WHITE,
        NONE // neither RED nor WHITE - i.e. wrong colour
    }
    
    public static final Score ALL_WHITE = new Score(WHITE, WHITE, WHITE, WHITE);
    
    private Multiset<Peg> pegs;
    
    public Score(Peg... pegs) {
        this(Arrays.asList(pegs));
    }

    public Score(List<Peg> pegs) {
        Preconditions.checkArgument(pegs.size() <= Game.NUM_POSITIONS);
        this.pegs = EnumMultiset.create(pegs, Peg.class);
        while (this.pegs.size() < Game.NUM_POSITIONS) {
            this.pegs.add(NONE);
        }
    }

    public static Score score(Move secret, Move move) {
        List<Peg> pegs = Lists.newArrayList();
        List<Boolean> matched = Arrays.asList(false, false, false, false);
        List<Boolean> used = Arrays.asList(false, false, false, false);
        for (int i = 0; i < Game.NUM_POSITIONS; i++) {
            if (move.get(i) == secret.get(i)) {
                pegs.add(WHITE);
                matched.set(i, true);
                used.set(i, true);
            }
        }
        for (int i = 0; i < Game.NUM_POSITIONS; i++) {
            if (matched.get(i)) {
                continue;
            }
            for (int j = 0; j < Game.NUM_POSITIONS; j++) {
                if (i != j && !used.get(j) && move.get(i) == secret.get(j)) {
                    pegs.add(RED);
                    used.set(j, true);
                    break;
                }
            }
        }
        return new Score(pegs);
    }

    public int count(Peg peg) {
        return pegs.count(peg);
    }

    public Set<List<Peg>> combinations() {
        return Sets.newLinkedHashSet(Collections2.permutations(pegs));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Score score = (Score) o;

        return pegs.equals(score.pegs);
    }

    @Override
    public int hashCode() {
        return pegs.hashCode();
    }

    @Override
    public String toString() {
        return pegs.toString();
    }
}
