package com.narrowtux.fmm;

import org.junit.Test;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SatTests {
    @Test
    public void testSat4J() {
        ISolver solver = SolverFactory.newDefault();
        try {
            solver.addExactly(new VecInt(new int[]{2, 1, 3, 4}), 1);
            solver.addExactly(new VecInt(new int[]{5, 7, 6, 8}), 1);
            assertTrue(solver.isSatisfiable());

            System.out.println(Arrays.toString(solver.model()));

        } catch (ContradictionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}