
package com.example.student.model;

import java.util.List;
import java.util.Map;

public class Student {

    private Long id;
    private String name;
    private Map<String, Double> scores;
    private List<String> skills;

    public Student() {
    }

    public Student(Long id, String name,
                   Map<String, Double> scores,
                   List<String> skills) {
        this.id = id;
        this.name = name;
        this.scores = scores;
        this.skills = skills;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Double> getScores() {
        return scores;
    }

    public void setScores(Map<String, Double> scores) {
        this.scores = scores;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public double getAverageScore() {
        if (scores == null || scores.isEmpty()) {
            return 0.0;
        }

        return scores.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
}
