package com.example.recogniselocation.thirdyearproject;

/**
 * Created by LaUrE on 13/11/2017.
 */

public class Point {
    private double x;
    private double y;

    public Point(double givenX, double givenY)
    {
        x = givenX;
        y = givenY;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}

