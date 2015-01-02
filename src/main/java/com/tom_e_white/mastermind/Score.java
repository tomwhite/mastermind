package com.tom_e_white.mastermind;

import com.google.common.collect.Collections2;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

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

    public Score() {
        this.pegs = EnumMultiset.create(Peg.class);
    }
    
    public Score(Peg... pegs) {
        this.pegs = EnumMultiset.create(Arrays.asList(pegs), Peg.class);
    }

    public static Score score(Move secret, Move move) {
        Score score = new Score();
        List<Boolean> matched = Arrays.asList(false, false, false, false);
        List<Boolean> used = Arrays.asList(false, false, false, false);
        for (int i = 0; i < Game.NUM_POSITIONS; i++) {
            if (move.get(i) == secret.get(i)) {
                score.add(WHITE);
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
                    score.add(RED);
                    used.set(j, true);
                    break;
                }
            }
        }
        return score;
    }

    public void add(Peg peg) {
        pegs.add(peg);
    }

    public int size() {
        return pegs.size();
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
