package com.example.recogniselocation.thirdyearproject;

public class Point {
    private double x;
    private double y;

    Point(double givenX, double givenY)
    {
        x = givenX;
        y = givenY;
    }

    double getX() {
        return x;
    }

    double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        else
            return (this.getX() == ((Point) obj).getX() &&
                this.getY() == ((Point) obj).getY());

    }
}

