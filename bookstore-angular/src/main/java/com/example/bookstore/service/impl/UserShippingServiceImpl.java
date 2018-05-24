/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.bookstore.domain.UserShipping;
import com.example.bookstore.repository.UserShippingRepository;
import com.example.bookstore.service.UserShippingService;


@Service
public class UserShippingServiceImpl implements UserShippingService{
    
    @Autowired
    private UserShippingRepository userShippingRepository;
    
    public UserShipping findById(Long id) {
        return userShippingRepository.findOne(id);
    }
    
    public void removeById(Long id) {
        userShippingRepository.delete(id);
    }
}