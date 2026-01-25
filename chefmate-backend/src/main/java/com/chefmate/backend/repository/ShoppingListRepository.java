package com.chefmate.backend.repository;

import com.chefmate.backend.entity.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {
    List<ShoppingList> findByUserId(Long userId);
    List<ShoppingList> findByUserIdAndCompleted(Long userId, Boolean completed);
    
    @Query("SELECT sl FROM ShoppingList sl WHERE sl.user.id = :userId AND sl.completed = :completed ORDER BY sl.createdAt DESC")
    List<ShoppingList> findByUserIdAndCompletedOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("completed") Boolean completed);
    
    void deleteByUserId(Long userId);
}