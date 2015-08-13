package com.narrowtux.fmm;

import com.narrowtux.fmm.model.MatchedVersion;
import com.narrowtux.fmm.model.Version;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by tux on 13/08/15.
 */
public class MatchedVersionTest {
    @Test
    public void testMatchedVersions() {
        assertTrue(     m(">= 0",       "0.0.0"));
        assertTrue(     m("> 0",        "0.0.0"));
        assertTrue(     m(">= 0.0",     "0.0.0"));
        assertTrue(     m("> 0.0",      "0.0.0"));
        assertFalse(    m("> 0.0.0",    "0.0.0"));
        assertFalse(    m(">= 1.2.3",   "1.2.2"));
        assertFalse(    m("> 1",        "0.0.0"));
        assertFalse(    m(">= 1",       "0.0.0"));
        assertTrue(     m(">= 0.0.0",   "0.0.0"));
        assertTrue(     m("<= 0.0.0",   "0.0.0"));
        assertTrue(     m("== 0.0.0",   "0.0.0"));
        assertFalse(    m("== 0.0.1",   "0.0.0"));
        assertTrue(     m("< 0.12",     "0.11.22")); // factorio 0.12 broke all mods on 0.11
        assertFalse(    m("< 0.12",     "0.12.0"));
        assertTrue(     m("< 0.12.2",   "0.12.0"));
        assertTrue(     m("< 0.12.0",   "0.11.22"));
        assertFalse(    m("< 0",        "0.0.0"));
    }

    @Test
    public void testToString() {
        assertEquals("> 0.1.2", MatchedVersion.valueOf("> 0.1.2").toString());
        assertEquals("> 0.1.?", MatchedVersion.valueOf("> 0.1").toString());
        assertEquals("> 0.?.?", MatchedVersion.valueOf("> 0").toString());
    }

    private static boolean m(String matched, String version) {
        return MatchedVersion.valueOf(matched).matches(Version.valueOf(version));
    }
}
