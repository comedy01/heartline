package dev.heartline.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CoreTest {
    @Test
    void trailHoldsThenDrains() {
        BarAnimator bar = new BarAnimator();
        bar.reset(20.0F);
        bar.tick(10.0F, 20.0F);
        assertEquals(20.0F, bar.trail(1.0F), 0.001F);
        assertTrue(bar.flash(0.0F) > 0.0F);
        for (int i = 0; i < 20; i++) {
            bar.tick(10.0F, 20.0F);
        }
        assertEquals(10.0F, bar.shown(1.0F), 0.001F);
        assertTrue(bar.trail(1.0F) < 20.0F);
        for (int i = 0; i < 100; i++) {
            bar.tick(10.0F, 20.0F);
        }
        assertEquals(10.0F, bar.trail(1.0F), 0.001F);
    }

    @Test
    void healGlows() {
        BarAnimator bar = new BarAnimator();
        bar.reset(10.0F);
        bar.tick(14.0F, 20.0F);
        assertTrue(bar.heal(0.0F) > 0.0F);
        assertEquals(0.0F, bar.flash(0.0F), 0.001F);
    }

    @Test
    void quickHitsStackIntoOneCombo() {
        Popups popups = new Popups();
        popups.add(1, 0, 0, 0, Popup.Kind.DAMAGE, 6.0F, true);
        for (int i = 0; i < 10; i++) {
            popups.tick();
        }
        Popup combo = popups.add(1, 0, 0, 0, Popup.Kind.CRIT, 9.0F, true);
        assertEquals(1, popups.all().size());
        assertEquals(15.0F, combo.amount(), 0.001F);
        assertEquals(2, combo.hits());
        assertEquals(Popup.Kind.CRIT, combo.kind());
    }

    @Test
    void combosStayApartWhenOffOrOtherMobOrHeal() {
        Popups popups = new Popups();
        popups.add(1, 0, 0, 0, Popup.Kind.DAMAGE, 6.0F, false);
        popups.add(1, 0, 0, 0, Popup.Kind.DAMAGE, 6.0F, false);
        popups.add(2, 0, 0, 0, Popup.Kind.DAMAGE, 6.0F, true);
        popups.add(2, 0, 0, 0, Popup.Kind.HEAL, 2.0F, true);
        assertEquals(4, popups.all().size());
    }

    @Test
    void popupsExpire() {
        Popups popups = new Popups();
        popups.add(1, 0, 0, 0, Popup.Kind.DAMAGE, 1.0F, true);
        for (int i = 0; i < Popup.LIFE; i++) {
            popups.tick();
        }
        assertTrue(popups.all().isEmpty());
    }

    @Test
    void numbersReadWell() {
        assertEquals("6", Numbers.amount(6.0F));
        assertEquals("1.4", Numbers.amount(1.42F));
        assertEquals("14 / 20", Numbers.health(HealthText.VALUE, 14.0F, 20.0F));
        assertEquals("70%", Numbers.health(HealthText.PERCENT, 14.0F, 20.0F));
        assertEquals("7 ❤", Numbers.health(HealthText.HEARTS, 14.0F, 20.0F));
        assertEquals("", Numbers.health(HealthText.OFF, 14.0F, 20.0F));
    }

    @Test
    void hitsToKill() {
        assertEquals(3, Numbers.hitsToKill(14.0F, 6.0F));
        assertEquals(1, Numbers.hitsToKill(6.0F, 6.0F));
        assertEquals(-1, Numbers.hitsToKill(6.0F, 0.0F));
    }

    @Test
    void visibilityModes() {
        assertTrue(Visibility.ALWAYS.shows(false, false, 1000, 120));
        assertFalse(Visibility.DAMAGED.shows(false, false, 1000, 120));
        assertTrue(Visibility.DAMAGED.shows(true, false, 1000, 120));
        assertTrue(Visibility.COMBAT.shows(true, false, 50, 120));
        assertFalse(Visibility.COMBAT.shows(true, false, 500, 120));
        assertTrue(Visibility.LOOKING.shows(false, true, 1000, 120));
        assertFalse(Visibility.LOOKING.shows(true, false, 0, 120));
    }

    @Test
    void healthColorsGoFromGreenToRed() {
        assertEquals(Palette.HIGH, Palette.health(1.0F));
        assertEquals(Palette.MID, Palette.health(0.5F));
        assertEquals(Palette.LOW, Palette.health(0.0F));
        assertEquals(0x80FFFFFF, Palette.alpha(0xFFFFFFFF, 128 / 255.0F));
    }
}
