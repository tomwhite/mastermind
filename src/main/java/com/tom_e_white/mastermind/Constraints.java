package com.tom_e_white.mastermind;

import org.jacop.constraints.XeqC;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.*;

public class Constraints {

    public static void main(String[] args) {
        Store store = new Store();
        int size = 4;
        IntVar[] v = new IntVar[size];
        for (int i = 0; i < size; i++) {
            v[i] = new IntVar(store, "v" + i, 0, 5);
        }
        store.impose(new XeqC(v[1], 1));

        // XeqC a var equals a constant
        // Or(XeqC, XeqC, XeqC)
        // Not(XeqC)

        Search<IntVar> search = new DepthFirstSearch<IntVar>();
        SelectChoicePoint<IntVar> select =
                new InputOrderSelect<IntVar>(store, v,
                        new IndomainMin<IntVar>());

        search.getSolutionListener().searchAll(true);
        search.getSolutionListener().recordSolutions(true);

        boolean result = search.labeling(store, select);

        //search.printAllSolutions();
    }
}
