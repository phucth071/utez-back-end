package com.hcmute.utezbe.controller;

import com.hcmute.utezbe.dto.AssignmentSubmissionDto;
import com.hcmute.utezbe.entity.AssignmentSubmission;
import com.hcmute.utezbe.exception.ResourceNotFoundException;
import com.hcmute.utezbe.response.Response;
import com.hcmute.utezbe.service.AssignmentService;
import com.hcmute.utezbe.service.AssignmentSubmissionService;
import com.hcmute.utezbe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/assignment-submissions")
@RequiredArgsConstructor
public class AssignmentSubmissionController {

    private final AssignmentSubmissionService assignmentSubmissionService;
    private final AssignmentService assignmentService;
    private final UserService userService;

    @GetMapping("")
    public Response getAllAssignmentSubmission() {
        try {
            return Response.builder().code(HttpStatus.OK.value()).success(true).message("Get all assignment submission successfully!").data(assignmentSubmissionService.getAllAssignmentSubmissions()).build();
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/pageable")
    public Response getAllAssignmentSubmissionsPageable(Pageable pageable) {
        try {
            return Response.builder().code(HttpStatus.OK.value()).success(true).message("Get all assignment submission pageable successfully!").data(assignmentSubmissionService.getAllAssignmentSubmissionsPageable(pageable)).build();
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/{assignmentId}/{studentId}")
    public Response getAssignmentSubmissionById(@PathVariable("assignmentId") Long assignmentId, @PathVariable("studentId") Long studentId) {
        try {
            Optional<AssignmentSubmission> assignmentSubmission = assignmentSubmissionService.getAssignmentSubmissionById(assignmentId, studentId);
            if (assignmentSubmission.isPresent()) {
                return Response.builder().code(HttpStatus.OK.value()).success(true).message("Get assignment submission successfully!").data(assignmentSubmission.get()).build();
            } else {
                throw new ResourceNotFoundException();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("")
    public Response createAssignmentSubmission(@RequestBody AssignmentSubmissionDto assignmentSubmissionDto) {
        try {
            AssignmentSubmission assignmentSubmission = AssignmentSubmission.builder()
                    .score(assignmentSubmissionDto.getScore())
                    .textSubmission(assignmentSubmissionDto.getTextSubmission())
                    .fileSubmissionUrl(assignmentSubmissionDto.getFileSubmissionUrl())
                    .linkSubmission(assignmentSubmissionDto.getLinkSubmission())
                    .assignment(assignmentService.getAssignmentById(assignmentSubmissionDto.getAssignmentId()).get())
                    .student(userService.getUserById(assignmentSubmissionDto.getStudentId()))
                    .build();
            return Response.builder().code(HttpStatus.CREATED.value()).success(true).message("Create assignment submission successfully!").data(assignmentSubmissionService.saveAssignmentSubmission(assignmentSubmission)).build();
        } catch (Exception e) {
            throw e;
        }
    }

    @PatchMapping("/{assignmentId}/{studentId}")
    public Response editAssignmentSubmission(@PathVariable("assignmentId") Long assignmentId, @PathVariable("studentId") Long studentId, @RequestBody AssignmentSubmissionDto assignmentSubmissionDto) {
        try {
            Optional<AssignmentSubmission> assignmentSubmissionOpt = assignmentSubmissionService.getAssignmentSubmissionById(assignmentId, studentId);
            if (assignmentSubmissionOpt.isPresent()) {
                AssignmentSubmission existingSubmission = assignmentSubmissionOpt.get();
                AssignmentSubmission updatedSubmission = convertAssignmentSubmissionDTO(assignmentSubmissionDto, existingSubmission);
                AssignmentSubmission savedSubmission = assignmentSubmissionService.updateAssignmentSubmission(existingSubmission, updatedSubmission);
                return Response.builder().code(HttpStatus.OK.value()).success(true).message("Assignment submission updated successfully!").data(savedSubmission).build();
            } else {
                throw new ResourceNotFoundException();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    @DeleteMapping("/{assignmentId}/{studentId}")
    public Response deleteAssignmentSubmission(@PathVariable("assignmentId") Long assignmentId, @PathVariable("studentId") Long studentId) {
        try {
            assignmentSubmissionService.deleteAssignmentSubmission(assignmentId, studentId);
            return Response.builder().code(HttpStatus.OK.value()).success(true).message("Assignment submission deleted successfully!").data(null).build();
        } catch (Exception e) {
            throw e;
        }
    }

    private AssignmentSubmission convertAssignmentSubmissionDTO(AssignmentSubmissionDto assignmentSubmissionDto, AssignmentSubmission existingSubmission) {
        if (assignmentSubmissionDto.getScore() != null) existingSubmission.setScore(assignmentSubmissionDto.getScore());
        if (assignmentSubmissionDto.getTextSubmission() != null) existingSubmission.setTextSubmission(assignmentSubmissionDto.getTextSubmission());
        if (assignmentSubmissionDto.getFileSubmissionUrl() != null) existingSubmission.setFileSubmissionUrl(assignmentSubmissionDto.getFileSubmissionUrl());
        if (assignmentSubmissionDto.getLinkSubmission() != null) existingSubmission.setLinkSubmission(assignmentSubmissionDto.getLinkSubmission());
        return existingSubmission;
    }

}
