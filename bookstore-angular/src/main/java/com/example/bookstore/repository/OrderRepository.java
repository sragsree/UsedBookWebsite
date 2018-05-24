/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.example.bookstore.domain.Order;
import com.example.bookstore.domain.User;

/**
 * @author ss186235
 * @version $Id: $
 */
public interface OrderRepository extends CrudRepository<Order, Long> {
    List<Order> findByUser(User user);
}

