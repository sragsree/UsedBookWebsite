/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.resource;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookstore.domain.Order;
import com.example.bookstore.domain.User;
import com.example.bookstore.service.UserService;

/**
 * @author ss186235
 * @version $Id: $
 */
@RestController
@RequestMapping("/order")
public class OrderResource {
    
    @Autowired
    private UserService userService;
    
    @RequestMapping("/getOrderList")
    public List<Order> getOrderList(Principal principal) {
        User user = userService.findByUsername(principal.getName());
        List<Order> orderList = user.getOrderList();
        
        return orderList;
    }

}
