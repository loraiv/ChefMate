package com.chefmate.backend.repository;

import com.chefmate.backend.entity.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {
    List<ShoppingListItem> findByShoppingListId(Long shoppingListId);
    List<ShoppingListItem> findByShoppingListIdAndPurchased(Long shoppingListId, Boolean purchased);
    void deleteByShoppingListId(Long shoppingListId);
}