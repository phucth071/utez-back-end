package com.hcmute.utezbe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.utezbe.entity.embeddedId.AssignmentSubmissionId;
import lombok.*;

import jakarta.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "student", "assignment"})
@Table(name = "assignment_submission")
@Builder
public class AssignmentSubmission extends Auditable {

    @EmbeddedId
    private AssignmentSubmissionId id;

    @Column(name = "score")
    private Double score;

    @Column(name = "text_submission", columnDefinition = "TEXT")
    private String textSubmission;

    @Column(name = "file_submission_url", columnDefinition = "TEXT")
    private String fileSubmissionUrl;

    @Column(name = "link_submission", columnDefinition = "TEXT")
    private String linkSubmission;

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "assignment_id", foreignKey = @ForeignKey(name = "FK_assignment_submission_assignment"),
    insertable = false, updatable = false)
    @JsonIgnore
    private Assignment assignment;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "student_id", foreignKey = @ForeignKey(name = "FK_assignment_submission_account"),
    insertable = false, updatable = false)
    private User student;

}
