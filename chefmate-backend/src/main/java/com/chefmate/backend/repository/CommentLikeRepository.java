package com.chefmate.backend.repository;

import com.chefmate.backend.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    List<CommentLike> findByCommentId(Long commentId);
    
    @Query("SELECT cl FROM CommentLike cl WHERE cl.comment.id IN :commentIds")
    List<CommentLike> findByCommentIds(@Param("commentIds") List<Long> commentIds);
    
    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    void deleteByCommentId(Long commentId);
}

