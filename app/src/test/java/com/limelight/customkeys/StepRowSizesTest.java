package com.limelight.customkeys;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StepRowSizesTest {
    @Test
    public void sevenStepChipsMakeRowsOfFourAndThree() {
        assertArrayEquals(new int[] {4, 3}, CustomKeyEditor.stepRowSizes(KeyStep.Type.values().length));
    }

    @Test
    public void everyChipLandsInOneRowAndNoRowEndsAlone() {
        for (int count = 2; count <= 12; count++) {
            int[] rows = CustomKeyEditor.stepRowSizes(count);
            assertEquals(count, rows[0] + rows[1]);
            assertEquals(true, rows[0] >= rows[1] && rows[0] - rows[1] <= 1);
        }
    }
}
