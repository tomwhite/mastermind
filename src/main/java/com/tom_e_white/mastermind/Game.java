package com.tom_e_white.mastermind;

import java.io.IOException;

public class Game {
    public static void main(String[] args) throws IOException {
        System.out.println("MASTERMIND");
        System.out.println("**********");
        System.out.println("Choose a secret combination of four pegs, then press enter. It's helpful to play along with a real set.");
        System.in.read();
        System.out.println("OK. I will try to deduce the four pegs you have chosen.");

        // print out move number and guess
        // ask for player to enter white and red pegs - e.g. rrw for two reds and a white (in any order)
        // if we get it right say so!
        // if we run out of goes say that the player won
    }
}
