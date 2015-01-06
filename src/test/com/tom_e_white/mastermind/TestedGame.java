package com.tom_e_white.mastermind;

import org.jacop.constraints.*;
import org.jacop.core.IntVar;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A subclass of Game that checks that every constraint imposed is consistent with the
 * (known) secret. If it is not then the test fails.
 */
public class TestedGame extends Game {
    private Move secret;

    public TestedGame(Move secret) {
        this.secret = secret;
    }

    protected void impose(PrimitiveConstraint constraint) {
        assertConstraint(constraint);
        super.impose(constraint);
    }

    /**
     * This ensures that we don't add a constraint that is false by failing immediately
     */
    private void assertConstraint(PrimitiveConstraint c) {
        if (secret == null) {
            return;
        }
        assertTrue(c.toString(), constraintToExpr(c));
    }

    private boolean constraintToExpr(PrimitiveConstraint c) {
        if (c instanceof XeqC) {
            IntVar x = ((XeqC) c).x;
            int pos = 0;
            for (IntVar peg : pegs) {
                if (x == peg) {
                    return secret.get(pos).ordinal() == ((XeqC) c).c;
                }
                pos++;
            }
            fail("Illegal");
        }
        if (c instanceof Not) {
            return !constraintToExpr(((Not) c).c);
        }
        if (c instanceof Or) {
            boolean ret = false;
            for (PrimitiveConstraint pc : ((Or) c).listOfC) {
                ret |= constraintToExpr(pc);
            }
            return ret;
        }
        if (c instanceof And) {
            boolean ret = true;
            for (PrimitiveConstraint pc : ((And) c).listOfC) {
                ret &= constraintToExpr(pc);
            }
            return ret;
        }
        fail("Illegal");
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " secret: " + secret;
    }
}
