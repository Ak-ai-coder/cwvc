package org.example.model;

public class CategoryWeight {
    private String category;
    private double weight;

    public CategoryWeight(String category, double weight) {
        this.category = category;
        this.weight = weight;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void updateWeight(double multiplier) {
        this.weight *= multiplier;
    }
}