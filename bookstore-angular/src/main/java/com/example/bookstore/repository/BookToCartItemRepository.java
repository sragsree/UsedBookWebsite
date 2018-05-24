/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.repository;

import org.springframework.data.repository.CrudRepository;

import com.example.bookstore.domain.BookToCartItem;
import com.example.bookstore.domain.CartItem;

/**
 * @author ss186235
 * @version $Id: $
 */
public interface BookToCartItemRepository extends CrudRepository<BookToCartItem, Long>{
    void deleteByCartItem(CartItem cartItem);
}
