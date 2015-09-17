package com.narrowtux.fmm;

import org.junit.Test;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.util.Arrays;

import static junit.framework.Assert.assertTrue;

public class SatTests {
    @Test
    public void testSat4J() throws Exception {
        ISolver solver = SolverFactory.newSAT();
        try {
            solver.addExactly(new VecInt(new int[]{2, 1, 3, 4}), 1);
            solver.addExactly(new VecInt(new int[]{5, 7, 6, 8}), 1);
            assertTrue(solver.isSatisfiable());

            System.out.println(Arrays.toString(solver.model()));

            solver = SolverFactory.newDefault();
            solver.addExactly(new VecInt(new int[]{1, 2, 3, 4, 5, 6}), 1);
            solver.isSatisfiable();
            System.out.println(Arrays.toString(solver.model()));
            VecInt literals = new VecInt(solver.model());
            solver.reset();
            solver = SolverFactory.newDefault();
            solver.addExactly(new VecInt(new int[]{1, 2, 3, 4, 5, 6}), 1);
            solver.addBlockingClause(literals);
            solver.isSatisfiable();
            System.out.println(Arrays.toString(solver.model()));

        } catch (ContradictionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}