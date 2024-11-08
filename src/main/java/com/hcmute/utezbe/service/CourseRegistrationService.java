package com.hcmute.utezbe.service;

import com.hcmute.utezbe.auth.AuthService;
import com.hcmute.utezbe.entity.Course;
import com.hcmute.utezbe.entity.CourseRegistration;
import com.hcmute.utezbe.entity.enumClass.Role;
import com.hcmute.utezbe.entity.enumClass.State;
import com.hcmute.utezbe.exception.AccessDeniedException;
import com.hcmute.utezbe.exception.ResourceNotFoundException;
import com.hcmute.utezbe.repository.CourseRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseRegistrationService {
    private final CourseRegistrationRepository repository;
    private final UserService userService;

    public Optional<CourseRegistration> getCourseRegistrationByEmailAndCourseId(String email, Long courseId) {
        return repository.findByEmailAndCourseId(email, courseId);
    }


    public CourseRegistration save(CourseRegistration courseRegistration) {
        if (courseRegistration.getState() == null) {
            courseRegistration.setState(State.ACCEPTED);
        }
        Optional<CourseRegistration> otp = getCourseRegistrationByEmailAndCourseId(courseRegistration.getEmail(), courseRegistration.getCourse().getId());
        if (otp.isPresent()) {
            throw new ResourceNotFoundException("You have already registered this course!");
        }
        return repository.save(courseRegistration);
    }

    public void delete(CourseRegistration courseRegistration) {
        repository.delete(courseRegistration);
    }

    public Page<CourseRegistration> getCourseRegistrationsByStudentId(Long studentId, Pageable pageable) {
        Page<CourseRegistration> courseRegistrations = repository.findByEmail(userService.getUserById(studentId).getEmail(), pageable);
        if (courseRegistrations.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return courseRegistrations;
    }


    public Page<CourseRegistration> getCourseRegistrationsByCourseId(Long courseId, Pageable pageable) {
        Page<CourseRegistration> courseRegistrations = repository.findByCourseId(courseId, pageable);
        if (courseRegistrations.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return courseRegistrations;
    }



    public List<CourseRegistration> getAllCourseRegistrations() {
        if (!AuthService.isUserHaveRole(Role.TEACHER) && !AuthService.isUserHaveRole(Role.ADMIN)) {
            throw new AccessDeniedException("You do not have permission to do this action!");
        }
        return repository.findAll();
    }
    public Page<CourseRegistration> getAllCourseRegistrations(Pageable pageable) {
        if (!AuthService.isUserHaveRole(Role.TEACHER) && !AuthService.isUserHaveRole(Role.ADMIN)) {
            throw new AccessDeniedException("You do not have permission to do this action!");
        }
        return repository.findAll(pageable);
    }

    public CourseRegistration toggleCourseRegistration(Long studentId, Long courseId) {
        if (!AuthService.isUserHaveRole(Role.TEACHER) && !AuthService.isUserHaveRole(Role.ADMIN)) {
            throw new AccessDeniedException("You do not have permission to do this action!");
        }
        Optional<CourseRegistration> otp = getCourseRegistrationByEmailAndCourseId(userService.getUserById(studentId).getEmail(), courseId);
        if (otp.isPresent()) {
            CourseRegistration courseRegistration = otp.get();
            if (courseRegistration.getState().equals("PENDING")) {
                courseRegistration.setState(State.ACCEPTED);
            } else {
                courseRegistration.setState(State.PENDING);
            }
            return save(courseRegistration);
        }
        return null;
    }

    public List<CourseRegistration> registerCourse(Long courseId, List<String> emails) {
        if (!AuthService.isUserHaveRole(Role.TEACHER) && !AuthService.isUserHaveRole(Role.ADMIN)) {
            throw new AccessDeniedException("You do not have permission to do this action!");
        }
        List<CourseRegistration> courseRegistrations = emails.stream()
                .map(email -> CourseRegistration.builder()
                        .course(Course.builder().id(courseId).build())
                        .email(email)
                        .state(State.ACCEPTED)
                        .build())
                .toList();
        return repository.saveAll(courseRegistrations);
    }
}
