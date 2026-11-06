package com.limelight.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// NOTE: Both pickers filter with SearchField.matches, so every typed word must be in the label, in any order, ignoring case.
public class SearchFieldTest {
    @Test
    public void anEmptyQueryMatchesEverything() {
        assertTrue(SearchField.matches("Toggle Keyboard", ""));
        assertTrue(SearchField.matches("Toggle Keyboard", null));
        assertTrue(SearchField.matches("Toggle Keyboard", "   "));
    }

    @Test
    public void caseIsIgnored() {
        assertTrue(SearchField.matches("Toggle Keyboard", "KEYBOARD"));
    }

    @Test
    public void everyWordMustMatchInAnyOrder() {
        assertTrue(SearchField.matches("Toggle Keyboard", "board tog"));
        assertTrue(SearchField.matches("Toggle Keyboard", "keyboard toggle"));
        assertFalse(SearchField.matches("Toggle Keyboard", "keyboard mouse"));
    }
}
