/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.service;

import java.util.Set;

import com.example.bookstore.domain.User;
import com.example.bookstore.domain.UserBilling;
import com.example.bookstore.domain.UserPayment;
import com.example.bookstore.domain.UserShipping;
import com.example.bookstore.domain.security.UserRole;

/**
 * @author ss186235
 * @version $Id: $
 */
public interface UserService {
    
    User createUser(User user, Set<UserRole> userRoles);

    User findByUsername(String username);
    
    User findByEmail (String email);
    
    User save(User user);
    
    User findById(Long id);
    
    void updateUserPaymentInfo(UserBilling userBilling, UserPayment userPayment, User user);
    
    void updateUserBilling(UserBilling userBilling, UserPayment userPayment, User user);
    
    void setUserDefaultPayment(Long userPaymentId, User user);
    
    void updateUserShipping(UserShipping userShipping, User user);

    void setUserDefaultShipping(Long userShippingId, User user);
}
