package com.tom_e_white.mastermind;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * A move represents four coloured pegs in a particular order.
 */
public class Move {

    /**
     * Colours of pegs for the moves.
     */
    public static enum Peg {
        Blue, Green, Purple, Orange, Yellow, Pink
    }

    private List<Integer> pegs;
    
    public Move(int a, int b, int c, int d) {
        pegs = Arrays.asList(a, b, c, d);
    }

    public Move(List<Integer> pegs) {
        Preconditions.checkArgument(pegs.size() == Game.NUM_POSITIONS);
        this.pegs = pegs;
    }

    public int get(int pos) {
        return pegs.get(pos);
    }

    public boolean hasDistinctColours() {
        return Sets.newHashSet(pegs).size() == Game.NUM_POSITIONS;
    }

    public Set<Integer> diff(Move move) {
        Set<Integer> positions = Sets.newHashSet();
        for (int pos : Game.ALL_POS) {
            if (get(pos) != move.get(pos)) {
                positions.add(pos);
            }
        }
        return positions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Move move = (Move) o;

        return pegs.equals(move.pegs);
    }

    @Override
    public int hashCode() {
        return pegs.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i : pegs) {
            sb.append(Peg.values()[i]).append(" ");
        }
        return sb.toString();

    }
}
