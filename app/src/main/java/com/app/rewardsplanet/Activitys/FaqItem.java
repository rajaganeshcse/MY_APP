package com.app.rewardsplanet.Activitys;

public class FaqItem {
    private final String question;
    private final String answer;
    private final String category;
    private boolean expanded;

    public FaqItem(String question, String answer, String category) {
        this.question = question;
        this.answer = answer;
        this.category = category;
        this.expanded = false;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public String getCategory() {
        return category;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }
}
