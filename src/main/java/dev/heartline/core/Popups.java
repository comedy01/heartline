package dev.heartline.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class Popups {
    public static final int MAX = 64;

    private final List<Popup> popups = new ArrayList<>();
    private int counter;

    public Popup add(int entityId, double x, double y, double z, Popup.Kind kind, float amount, boolean combo) {
        if (combo) {
            for (int i = popups.size() - 1; i >= 0; i--) {
                Popup popup = popups.get(i);
                if (popup.merges(entityId, kind)) {
                    popup.merge(kind, amount);
                    return popup;
                }
            }
        }
        if (popups.size() >= MAX) {
            popups.remove(0);
        }
        float drift = ((counter++ * 3) % 5 - 2) * 0.18F;
        Popup popup = new Popup(entityId, x, y, z, drift, kind, amount);
        popups.add(popup);
        return popup;
    }

    public void tick() {
        Iterator<Popup> it = popups.iterator();
        while (it.hasNext()) {
            if (it.next().tick()) {
                it.remove();
            }
        }
    }

    public void clear() {
        popups.clear();
    }

    public List<Popup> all() {
        return Collections.unmodifiableList(popups);
    }
}
