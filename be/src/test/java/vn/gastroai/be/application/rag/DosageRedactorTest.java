package vn.gastroai.be.application.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DosageRedactorTest {

    @Test
    void redactsMilligramDoseWithFrequency() {
        String redacted = DosageRedactor.redact("Amitriptyline 25mg/ngay vao buoi toi.");
        assertFalse(redacted.contains("25mg"));
        assertTrue(redacted.contains("[liều lượng cụ thể đã được ẩn"));
    }

    @Test
    void redactsPerKilogramDose() {
        String redacted = DosageRedactor.redact("Metronidazole 40mg/kg chia 3 lan.");
        assertFalse(redacted.contains("40mg"));
    }

    @Test
    void redactsTabletCountPerDay() {
        String redacted = DosageRedactor.redact("Uong 2 vien/ngay sau an.");
        assertFalse(redacted.contains("2 vien"));
    }

    @Test
    void leavesOrdinaryTextUnchanged() {
        String text = "Nen an nhieu chat xo va uong nhieu nuoc moi ngay.";
        assertEquals(text, DosageRedactor.redact(text));
    }

    @Test
    void handlesNullAndBlankGracefully() {
        assertEquals(null, DosageRedactor.redact(null));
        assertEquals("", DosageRedactor.redact(""));
    }
}
