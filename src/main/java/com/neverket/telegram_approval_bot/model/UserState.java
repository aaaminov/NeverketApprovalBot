package com.neverket.telegram_approval_bot.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_state")
public class UserState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.ORDINAL) // хранит индекс (0, 1, 2, ...)
    private UserBotState state;

    @OneToOne
    @JoinColumn(name = "request_in_progress_id")
    private Request requestInProgress;

    private int currentApprovalLevel;

    public UserState() {
    }

    public UserState(User user, UserBotState state, Request requestInProgress, int currentApprovalLevel) {
        this.user = user;
        this.state = state;
        this.requestInProgress = requestInProgress;
        this.currentApprovalLevel = currentApprovalLevel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserBotState getState() {
        return state;
    }

    public void setState(UserBotState state) {
        this.state = state;
    }

    public Request getRequestInProgress() {
        return requestInProgress;
    }

    public void setRequestInProgress(Request requestInProgress) {
        this.requestInProgress = requestInProgress;
    }

    public int getCurrentApprovalLevel() {
        return currentApprovalLevel;
    }

    public void setCurrentApprovalLevel(int currentApprovalLevel) {
        this.currentApprovalLevel = currentApprovalLevel;
    }

    @Override
    public String toString() {
        return "UserState{" +
                "id=" + id +
                ", user=" + user +
                ", state=" + state +
                ", requestInProgress=" + requestInProgress +
                ", currentApprovalLevel=" + currentApprovalLevel +
                '}';
    }
}
