Mastermind
==========

Mastermind is a two-player logic game played with pegs. The setter chooses four pegs from among the following colours:
blue, green, purple, orange, yellow, and pink. Repeats are allowed. The aim of the game is for the guesser to guess the
four pegs that the setter chose. The guesser does not guess blindly; rather, the setter marks each move made by the guesser
to indicate how close the move is to the answer. Two colours of pegs are used for this purpose: a white peg means that
a peg is the correct colour in the correct place, while a red peg means that a peg is the correct colour
but in the wrong place. There is no order to the white and red pegs, so the guesser cannot tell which pegs in the move they
correspond to. If the guesser gets the correct answer in eight moves or less then they win, otherwise the setter wins. 

Here is an example game. The setter chooses `blue yellow pink pink` and the guesser plays the following moves.

```
1. blue green purple orange [W]
2. purple orange yellow pink [RW]
3. blue yellow blue pink [WWW]
4. blue yellow pink pink [WWWW]
```

The score pegs are shown in brackets after each move. The first move gets a single white peg, since only one of the
colours out of `blue green purple orange` appears in the setter's choice (blue), and it appears in the same position.
On the second move, yellow and pink appear, but only pink is in the correct position, so the move gets a white peg and
a red peg. On the third move, only the second blue peg is incorrect.

The game is over in four moves since the guesser gets the correct answer on the fourth move.

An Algorithm
============

The program presented here plays the part of the guesser. A human can play the part of the setter, although the computer
can play itself too. Currently the program wins 100% of the time, so it's not much fun for a human player. It would be
possible to make it lose sometimes by adding a constraint only a fixed percentage of the time (see below).

The algorithm works as follows:

1. The program maintains a list of constraints over the solution space of 6<sup>4</sup> = 1296 solutions.
2. The first two moves explore the solution space statically. The moves are (where colours are labelled 0 to 5):
    * 0, 1, 2, 3
    * 2, 3, 4, 5
3. The remaining moves search the space of remaining solutions.
4. Each move and its associated score is used to constrain the solution space.

To understand how a score assigned to a move generates constraints,
consider the simple case where the score is one white peg and no red pegs. For the example game earlier, the first move
is `blue green purple orange` which gets a single `W`. From the point of view of the guesser, the `W` could be in any of
the four positions:

```
W * * *
* W * *
* * W *
* * * W
```

This is represented by a constraint of the form:

```
peg[0] == W OR peg[1] == W OR peg[2] == W OR peg[3] == W
```

Actually, it's important to consider what no match means. We'll denote this by `N`.

```
(peg[0] == W AND peg[1] == N AND peg[2] == N AND peg[3] == N) OR
(peg[0] == N AND peg[1] == W AND peg[2] == N AND peg[3] == N) OR
(peg[0] == N AND peg[1] == N AND peg[2] == W AND peg[3] == N) OR
(peg[0] == N AND peg[1] == N AND peg[2] == N AND peg[3] == W)
```

If we translate this into actual colours from the move, we get:

```
(peg[0] == blue AND peg[1] != green AND peg[2] != purple AND peg[3] != orange) OR
(peg[0] != blue AND peg[1] == green AND peg[2] != purple AND peg[3] != orange) OR
(peg[0] != blue AND peg[1] != green AND peg[2] == purple AND peg[3] != orange) OR
(peg[0] != blue AND peg[1] != green AND peg[2] != purple AND peg[3] == orange)
```

We can say even more than this, since in the case of `W` being the first peg (blue) we know that green doesn't appear
_anywhere_. If it did then it would generate a corresponding `W` or `R`. The first clause in the OR becomes:

```
(peg[0] == blue AND
  (peg[0] != green AND peg[1] != green AND peg[2] != green AND peg[3] != green) AND
  (peg[0] != purple AND peg[1] != purple AND peg[2] != purple AND peg[3] != purple) AND
  (peg[0] != orange AND peg[1] != orange AND peg[2] != orange AND peg[3] != orange)) OR
...
```

Although there's no `R` in this case, the corresponding constraint states that the colour appears in one of the positions
_except_ where that `R` appears. Furthermore, it may not not appear in a `W` position, although it may appear in another
`R` position, or in an `N` position.

In general, each permutation of the score is turned into a constraint in a similar way, and the resulting constraints
are all OR'd together. For example, if the score is `RW`, then the permutations of (`R`, `W`, `N`, `N`) are taken.

Constraints from Differences
============================

To win the game, it's sufficient to only consider the constraints generated from each move. However, when I first wrote
the program I missed the `N` constraints that ruled out some colours _not_ appearing. So I added constraints generated
by looking at the _difference_ between pairs of moves. For example, if the first two moves were:

```
1. blue green purple orange [W]
2. yellow green purple orange [R]
```

then we know that since the two moves differ only in a single peg (the first) the change from a white peg to a red peg
means that the blue peg from the first move is `W`, while the yellow peg from the second peg is `R`. 

Playing a Game
==============

To play a game type the following at the command line:

```bash
mvn compile
mvn exec:java -Dexec.mainClass=com.tom_e_white.mastermind.Game -q
```

Then follow the instructions shown in the console.
