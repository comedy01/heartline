package dev.heartline.core;

public final class Popup {
    public static final int LIFE = 40;
    public static final int FADE = 10;
    public static final int COMBO_WINDOW = 30;

    public enum Kind {
        DAMAGE,
        CRIT,
        HEAL
    }

    private final int entityId;
    private final double x;
    private final double y;
    private final double z;
    private final float drift;
    private Kind kind;
    private float amount;
    private int hits;
    private int age;
    private int bump;

    Popup(int entityId, double x, double y, double z, float drift, Kind kind, float amount) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.drift = drift;
        this.kind = kind;
        this.amount = amount;
        this.hits = 1;
        this.bump = 4;
    }

    boolean merges(int entityId, Kind kind) {
        return this.entityId == entityId && age < COMBO_WINDOW && (kind == Kind.HEAL) == (this.kind == Kind.HEAL);
    }

    void merge(Kind kind, float amount) {
        this.amount += amount;
        this.hits++;
        this.age = 0;
        this.bump = 4;
        if (kind == Kind.CRIT) {
            this.kind = Kind.CRIT;
        }
    }

    boolean tick() {
        age++;
        if (bump > 0) {
            bump--;
        }
        return age >= LIFE;
    }

    public int entityId() {
        return entityId;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float drift() {
        return drift;
    }

    public Kind kind() {
        return kind;
    }

    public float amount() {
        return amount;
    }

    public int hits() {
        return hits;
    }

    public float rise(float partial) {
        float t = Math.min(age + partial, LIFE) / LIFE;
        return 0.9F * (1.0F - (1.0F - t) * (1.0F - t));
    }

    public float alpha(float partial) {
        float left = LIFE - (age + partial);
        return Math.max(0.0F, Math.min(1.0F, left / FADE));
    }

    public float pop(float partial) {
        return 1.0F + 0.5F * Math.max(0.0F, bump - partial) / 4.0F;
    }
}
