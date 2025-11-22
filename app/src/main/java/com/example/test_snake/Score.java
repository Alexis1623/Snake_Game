package com.example.test_snake;

public class Score {
    private int rank;
    private String name;
    private int score;

    public Score(int rank, String name, int score) {
        this.rank = rank;
        this.name = name;
        this.score = score;
    }

    public int getRank() {
        return rank;
    }

    public void setPosition(int position) {
        this.rank = position;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }
}
