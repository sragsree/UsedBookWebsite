/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.service.impl;

import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.bookstore.domain.BillingAddress;
import com.example.bookstore.domain.Book;
import com.example.bookstore.domain.CartItem;
import com.example.bookstore.domain.Order;
import com.example.bookstore.domain.Payment;
import com.example.bookstore.domain.ShippingAddress;
import com.example.bookstore.domain.ShoppingCart;
import com.example.bookstore.domain.User;
import com.example.bookstore.repository.BillingAddressRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.PaymentRepository;
import com.example.bookstore.repository.ShippingAddressRepository;
import com.example.bookstore.service.BookService;
import com.example.bookstore.service.CartItemService;
import com.example.bookstore.service.OrderService;
import com.example.bookstore.utility.MailConstructor;

/**
 * @author ss186235
 * @version $Id: $
 */
@Service
public class OrderServiceImpl implements OrderService{
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private BillingAddressRepository billingAddressRepository;
    
    @Autowired
    private ShippingAddressRepository shippingAddressRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private CartItemService cartItemService;
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private MailConstructor mailConstructor;
    
    public synchronized  Order createOrder(
            ShoppingCart shoppingCart,
            ShippingAddress shippingAddress,
            BillingAddress billingAddress,
            Payment payment,
            String shippingMethod,
            User user
            ){
        Order order = new Order();
        order.setBillingAddress(billingAddress);
        order.setOrderStatus("created");
        order.setPayment(payment);
        order.setShippingAddress(shippingAddress);
        order.setShippingMethod(shippingMethod);
        
        List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);
        
        for (CartItem cartItem : cartItemList) {
            Book book = cartItem.getBook();
            cartItem.setOrder(order);
            book.setInStockNumber(book.getInStockNumber()-cartItem.getQty());
        }
        
        order.setCartItemList(cartItemList);
        order.setOrderDate(Calendar.getInstance().getTime());
        order.setOrderTotal(shoppingCart.getGrandTotal());
        shippingAddress.setOrder(order);
        billingAddress.setOrder(order);
        payment.setOrder(order);
        order.setUser(user);
        order = orderRepository.save(order);
        
        return order;
    }
    
    public Order findOne(Long id) {
        return orderRepository.findOne(id);
    }

}
