Mastermind is a two-player logic game played with pegs. The setter chooses four pegs from among the following colours:
blue, green, purple, orange, yellow, and pink. Repeats are allowed. The aim of the game is for the guesser to guess the
four pegs that the setter chose. The guesser does not guess blindly; rather, the setter marks each move made by the guesser
to indicate how close the move is to the answer. Two colours of pegs are used for this purpose: a white peg means that
a peg is the correct colour in the correct place, while a red peg means that a peg is the correct colour
but in the wrong place. There is no order to the white and red pegs, so the guesser cannot tell which pegs in the move they
correspond to. If the guesser gets the correct answer in eight moves or less then they win, otherwise the setter wins. 

[Example]

There are 6^4 = 1296 possible combinations of four pegs that the setter can choose from.

The program presented here plays the part of the guesser. A human can play the part of the setter, although the computer
can play itself too. Currently the program wins all but 10 possible games: a rate of 99.2%. It should be possible to
win 100% of games, but I haven't managed this yet.

The algorithm works as follows:

* The program maintains a list of constraints over the solution space of 1296 solutions.
* The first four moves explore the solution space statically. The idea is that moves with four distinct colours are played, each of which differs in one position to a previous move. The moves are (where colours are labelled 0 to 5):
    * 0, 1, 2, 3
    *  4, 1, 2, 3
    * 4, 5, 2, 3
    * 4, 5, 0, 3
* The remaining moves search the space of remaining solutions.
* Each move and its associated score is used to constrain the solution space. For example...
* Each move is compared to previous moves, and the _difference_ between it and each previous move is used to constrain
the space further.

Areas for improvement

* The static moves could be replaced by dynamic move