package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FinTipAngleSettingTest {
    @Test void acceptsNegativeYamlNumberAndQuotedString() {
        assertEquals(-6.0, FinTipAngleSetting.effective(-6.0), 1e-8);
        assertEquals(-6.0, FinTipAngleSetting.effective("-6.0"), 1e-8);
        assertEquals(-6.0, FinTipAngleSetting.validate("-6"), 1e-8);
        assertEquals(-6.0, FinTipAngleSetting.validate("−6"), 1e-8);
        assertEquals(-6.0, FinTipAngleSetting.validate("－6"), 1e-8);
    }
    @Test void supportsFullSignedRangeAndClampsConfig() {
        assertEquals(-15.0, FinTipAngleSetting.validate(-15), 1e-8);
        assertEquals(15.0, FinTipAngleSetting.validate(15), 1e-8);
        assertEquals(0.0, FinTipAngleSetting.validate(0), 1e-8);
        assertEquals(-15.0, FinTipAngleSetting.effective(-100), 1e-8);
        assertEquals(15.0, FinTipAngleSetting.effective(100), 1e-8);
    }
    @Test void rejectsBadCommandsAndFallsBackForMalformedConfig() {
        assertThrows(IllegalArgumentException.class, () -> FinTipAngleSetting.validate("-16"));
        assertThrows(IllegalArgumentException.class, () -> FinTipAngleSetting.validate("16"));
        assertThrows(IllegalArgumentException.class, () -> FinTipAngleSetting.validate("nan"));
        assertThrows(IllegalArgumentException.class, () -> FinTipAngleSetting.validate(null));
        assertEquals(6.0, FinTipAngleSetting.effective("invalid"), 1e-8);
        assertEquals(6.0, FinTipAngleSetting.effective(null), 1e-8);
    }
}
