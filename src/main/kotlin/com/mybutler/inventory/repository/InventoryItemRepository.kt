package com.mybutler.inventory.repository

import com.mybutler.inventory.entity.Category
import com.mybutler.inventory.entity.InventoryItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface InventoryItemRepository : JpaRepository<InventoryItem, Long> {
    fun findAllByUserId(userId: Long): List<InventoryItem>

    fun findAllByUserIdOrderByUpdatedAtDesc(userId: Long): List<InventoryItem>

    fun findByUserId(userId: Long, pageable: Pageable): Page<InventoryItem>

    fun findByUserIdAndCategory(userId: Long, category: Category, pageable: Pageable): Page<InventoryItem>
}
