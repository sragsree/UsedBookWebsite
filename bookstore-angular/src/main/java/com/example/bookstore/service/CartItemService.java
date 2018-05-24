/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.service;

import java.util.List;

import com.example.bookstore.domain.Book;
import com.example.bookstore.domain.CartItem;
import com.example.bookstore.domain.ShoppingCart;
import com.example.bookstore.domain.User;

/**
 * @author ss186235
 * @version $Id: $
 */
public interface CartItemService {
    
    CartItem addBookToCartItem(Book book, User user, int qty);
    
    List<CartItem> findByShoppingCart(ShoppingCart shoppingCart);
    
//  List<CartItem> findByOrder(Order order);
    
    CartItem updateCartItem(CartItem cartItem);
    
    void removeCartItem(CartItem cartItem);
    
    CartItem findById(Long id);
    
    CartItem save(CartItem cartItem);

}
