package com.hcmute.utezbe.repository;

import com.hcmute.utezbe.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findById(Long id);

    List<Topic> findAllByForumId(Long forumId);

}
