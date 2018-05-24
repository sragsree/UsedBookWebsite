/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.service;

import com.example.bookstore.domain.ShoppingCart;

/**
 * @author ss186235
 * @version $Id: $
 */
public interface ShoppingCartService {
    
    ShoppingCart updateShoppingCart(ShoppingCart shoppingCart);
    
    void clearShoppingCart(ShoppingCart shoppingCart);

}
