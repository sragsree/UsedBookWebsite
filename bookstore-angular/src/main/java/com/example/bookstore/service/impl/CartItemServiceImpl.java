/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookstore.domain.Book;
import com.example.bookstore.domain.BookToCartItem;
import com.example.bookstore.domain.CartItem;
import com.example.bookstore.domain.ShoppingCart;
import com.example.bookstore.domain.User;
import com.example.bookstore.repository.BookToCartItemRepository;
import com.example.bookstore.repository.CartItemRepository;
import com.example.bookstore.service.CartItemService;

/**
 * @author ss186235
 * @version $Id: $
 */
@Service
public class CartItemServiceImpl implements CartItemService{
    
    @Autowired
    private CartItemRepository cartItemRepository;
    
    @Autowired
    private BookToCartItemRepository bookToCartItemRepository;

    public CartItem addBookToCartItem(Book book, User user, int qty) {
        List<CartItem> cartItemList = findByShoppingCart(user.getShoppingCart());
        
        for (CartItem cartItem : cartItemList) {
            if (book.getId() == cartItem.getBook().getId()) {
                cartItem.setQty(cartItem.getQty()+qty);
                cartItem.setSubtotal(new BigDecimal(book.getOurPrice()).multiply(new BigDecimal(qty)));
                cartItemRepository.save(cartItem);
                return cartItem;
            }
        }
        
        CartItem cartItem = new CartItem();
        cartItem.setShoppingCart(user.getShoppingCart());
        cartItem.setBook(book);
        cartItem.setQty(qty);
        cartItem.setSubtotal(new BigDecimal(book.getOurPrice()).multiply(new BigDecimal(qty)));
        
        cartItem =cartItemRepository.save(cartItem);
        
        BookToCartItem bookToCartItem = new BookToCartItem();
        bookToCartItem.setBook(book);
        bookToCartItem.setCartItem(cartItem);
        bookToCartItemRepository.save(bookToCartItem);
        
        return cartItem;
    }
    
    @Transactional
    public void removeCartItem(CartItem cartItem) {
        bookToCartItemRepository.deleteByCartItem(cartItem);
        cartItemRepository.delete(cartItem);
    }
    
    public List<CartItem> findByShoppingCart(ShoppingCart shoppingCart) {
        return cartItemRepository.findByShoppingCart(shoppingCart);
    }
    
    public CartItem updateCartItem(CartItem cartItem) {
        BigDecimal bigDecimal = new BigDecimal(cartItem.getBook().getOurPrice()).multiply(new BigDecimal(cartItem.getQty()));
        bigDecimal = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);
        cartItem.setSubtotal(bigDecimal);
        
        cartItemRepository.save(cartItem);
        
        return cartItem;
        
    }
    
    public CartItem findById(Long id) {
        return cartItemRepository.findOne(id);
    }
    
    public CartItem save(CartItem cartItem) {
        return cartItemRepository.save(cartItem);
    }
}
