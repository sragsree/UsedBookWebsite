/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.repository;

import java.util.List;


import org.springframework.data.repository.CrudRepository;

import com.example.bookstore.domain.CartItem;
import com.example.bookstore.domain.ShoppingCart;

/**
 * @author ss186235
 * @version $Id: $
 */

public interface CartItemRepository extends CrudRepository<CartItem, Long> {
    List<CartItem> findByShoppingCart(ShoppingCart shoppingCart);
    List<CartItem> findByModeAndOrderIsNotNullOrderByIdDesc(String mode);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from CartItem c where c.id = :id")
    java.util.Optional<CartItem> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    
//  List<CartItem> findByOrder(Order order);
}

