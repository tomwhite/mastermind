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

    private List<Integer> colours;
    
    public Move(int a, int b, int c, int d) {
        colours = Arrays.asList(a, b, c, d);
    }

    public Move(List<Integer> colours) {
        Preconditions.checkArgument(colours.size() == Game.NUM_POSITIONS);
        this.colours = colours;
    }

    public int get(int pos) {
        return colours.get(pos);
    }

    public boolean hasDistinctColours() {
        return Sets.newHashSet(colours).size() == Game.NUM_POSITIONS;
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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i : colours) {
            sb.append(Peg.values()[i]).append(" ");
        }
        return sb.toString();

    }
}
