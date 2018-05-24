/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.service;

import com.example.bookstore.domain.BillingAddress;
import com.example.bookstore.domain.Order;
import com.example.bookstore.domain.Payment;
import com.example.bookstore.domain.ShippingAddress;
import com.example.bookstore.domain.ShoppingCart;
import com.example.bookstore.domain.User;

/**
 * @author ss186235
 * @version $Id: $
 */
public interface OrderService {
    
    Order createOrder(
            ShoppingCart shoppingCart,
            ShippingAddress shippingAddress,
            BillingAddress billingAddress,
            Payment payment,
            String shippingMethod,
            User user
    );

}

