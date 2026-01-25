package com.chefmate.backend.repository;

import com.chefmate.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByRecipeIdOrderByCreatedAtDesc(Long recipeId);
    
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.recipe.id = :recipeId AND c.parentComment IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findByRecipeIdAndParentCommentIsNullOrderByCreatedAtDesc(@Param("recipeId") Long recipeId);
    
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.parentComment.id = :parentCommentId ORDER BY c.createdAt ASC")
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(@Param("parentCommentId") Long parentCommentId);
    
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.parentComment.id IN :parentCommentIds ORDER BY c.parentComment.id, c.createdAt ASC")
    List<Comment> findByParentCommentIdsOrderByCreatedAtAsc(@Param("parentCommentIds") List<Long> parentCommentIds);
    
    void deleteByRecipeId(Long recipeId);
    
    // Delete all comments by a user
    void deleteByUser_Id(Long userId);
    
    // Get all comment IDs by a user
    @Query("SELECT c.id FROM Comment c WHERE c.user.id = :userId")
    List<Long> findCommentIdsByUserId(@Param("userId") Long userId);
    
    // Delete replies that reference comments by a specific user
    void deleteByParentComment_User_Id(Long userId);
}


