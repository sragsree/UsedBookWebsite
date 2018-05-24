/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookstore.domain.ShoppingCart;
import com.example.bookstore.domain.User;
import com.example.bookstore.domain.UserBilling;
import com.example.bookstore.domain.UserPayment;
import com.example.bookstore.domain.UserShipping;
import com.example.bookstore.domain.security.UserRole;
import com.example.bookstore.repository.RoleRepository;
import com.example.bookstore.repository.UserBillingRepository;
import com.example.bookstore.repository.UserPaymentRepository;
import com.example.bookstore.repository.UserRepository;
import com.example.bookstore.repository.UserShippingRepository;
import com.example.bookstore.service.UserService;

/**
 * @author ss186235
 * @version $Id: $
 */

@Service
public class UserServiceImpl implements UserService{
    
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserBillingRepository userBillingRepository;
    
    @Autowired
    private UserPaymentRepository userPaymentRepository;
    
    @Autowired
    private UserShippingRepository userShippingRepository;
    
    @Transactional
    public User createUser(User user, Set<UserRole> userRoles) {
        User localUser = userRepository.findByUsername(user.getUsername());
        
        if(localUser != null) {
            LOG.info("User with username {} already exist. Nothing will be done. ", user.getUsername());
        } else {
            for (UserRole ur : userRoles) {
                roleRepository.save(ur.getRole());
            }
            
            user.getUserRoles().addAll(userRoles);
            
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setUser(user);
            user.setShoppingCart(shoppingCart);
            
            user.setUserPaymentList(new ArrayList<UserPayment>());
            user.setUserShippingList(new ArrayList<UserShipping>());
            
            localUser = userRepository.save(user);
        }
        
        return localUser;
    }
    
    @Override
    public User save(User user)  {
        return userRepository.save(user);
    }
    
    @Override
    public User findById(Long id) {
        return userRepository.findOne(id);
    }
    
    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    public void updateUserPaymentInfo(UserBilling userBilling, UserPayment userPayment, User user) {
        save(user);
        userBillingRepository.save(userBilling);
        userPaymentRepository.save(userPayment);
    }
    
    @Override
    public void updateUserBilling(UserBilling userBilling, UserPayment userPayment, User user) {
        userPayment.setUser(user);
        userPayment.setUserBilling(userBilling);
        userPayment.setDefaultPayment(true);
        userBilling.setUserPayment(userPayment);
        user.getUserPaymentList().add(userPayment);
        save(user);
    }
    
    @Override
    public void setUserDefaultPayment(Long userPaymentId, User user) {
        List<UserPayment> userPaymentList = (List<UserPayment>) userPaymentRepository.findAll();
        
        for (UserPayment userPayment : userPaymentList) {
            if(userPayment.getId() == userPaymentId) {
                userPayment.setDefaultPayment(true);
                userPaymentRepository.save(userPayment);
            } else {
                userPayment.setDefaultPayment(false);
                userPaymentRepository.save(userPayment);
            }
        }
    }
    
    @Override
    public void updateUserShipping(UserShipping userShipping, User user) {
        userShipping.setUser(user);
        userShipping.setUserShippingDefault(true);
        user.getUserShippingList().add(userShipping);
        save(user);
    }
    
    @Override
    public void setUserDefaultShipping(Long userShippingId, User user) {
        List<UserShipping> userShippingList = (List<UserShipping>) userShippingRepository.findAll();
        
        for (UserShipping userShipping : userShippingList) {
            if(userShipping.getId() == userShippingId) {
                userShipping.setUserShippingDefault(true);
                userShippingRepository.save(userShipping);
            } else {
                userShipping.setUserShippingDefault(false);
                userShippingRepository.save(userShipping);
            }
        }
    }
}
