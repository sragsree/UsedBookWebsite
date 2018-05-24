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

import com.example.bookstore.domain.CartItem;
import com.example.bookstore.domain.ShoppingCart;
import com.example.bookstore.repository.ShoppingCartRepository;
import com.example.bookstore.service.CartItemService;
import com.example.bookstore.service.ShoppingCartService;

/**
 * @author ss186235
 * @version $Id: $
 */
@Service
public class ShoppingCartServiceImpl implements ShoppingCartService{
    
    @Autowired
    private CartItemService cartItemService;
    
    @Autowired
    private ShoppingCartRepository shoppingCartRepository;
    
    public ShoppingCart updateShoppingCart(ShoppingCart shoppingCart) {
        BigDecimal cartTotal = new BigDecimal(0);
        
        List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);
        
        for (CartItem cartItem : cartItemList) {
            if(cartItem.getBook().getInStockNumber()>0) {
                cartItemService.updateCartItem(cartItem);
                cartTotal = cartTotal.add(cartItem.getSubtotal());
            }
        }
        
        shoppingCart.setGrandTotal(cartTotal);
        
        shoppingCartRepository.save(shoppingCart);
        
        return shoppingCart;
    }
    
    public void clearShoppingCart(ShoppingCart shoppingCart) {
        List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);
        
        for(CartItem cartItem : cartItemList) {
            cartItem.setShoppingCart(null);
            cartItemService.save(cartItem);
        }
        
        shoppingCart.setGrandTotal(new BigDecimal(0));
        
        shoppingCartRepository.save(shoppingCart);
    }

}

