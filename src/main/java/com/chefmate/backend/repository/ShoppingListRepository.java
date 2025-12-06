package com.chefmate.backend.repository;

import com.chefmate.backend.entity.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {
    List<ShoppingList> findByUserId(Long userId);
    List<ShoppingList> findByUserIdAndCompleted(Long userId, Boolean completed);
}