/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.bookstore.domain.UserPayment;
import com.example.bookstore.repository.UserPaymentRepository;
import com.example.bookstore.service.UserPaymentService;

/**
 * @author ss186235
 * @version $Id: $
 */
@Service
public class UserPaymentServiceImpl implements UserPaymentService {
    @Autowired
    private UserPaymentRepository userPaymentRepository;
    
    public UserPayment findById(Long id) {
        return userPaymentRepository.findOne(id);
    }
    
    public void removeById(Long id) {
        userPaymentRepository.delete(id);
    }
    
}
