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
    
//  List<CartItem> findByOrder(Order order);
}

