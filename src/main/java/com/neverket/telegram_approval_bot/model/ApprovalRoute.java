package com.neverket.telegram_approval_bot.model;

import jakarta.persistence.*;

@Entity
@Table(name = "approval_route")
public class ApprovalRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Request request;

    private int level; // уровень маршрута

    @ManyToOne
    private User reviewer;

    @Enumerated(EnumType.ORDINAL) // хранит индекс (0, 1, 2, ...)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    public ApprovalRoute() {
    }

    public ApprovalRoute(Request request, int level, User reviewer, ApprovalStatus approvalStatus) {
        this.request = request;
        this.level = level;
        this.reviewer = reviewer;
        this.approvalStatus = approvalStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
