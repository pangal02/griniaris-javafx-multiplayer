package gr.uop.model;

public class Dice {
    private int value;

    public Dice() {
        roll();
    }

    public int roll() {
        value = (int) (Math.random() * 6) + 1;
        return value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("Dice{value=%d}", value);
    }
}
