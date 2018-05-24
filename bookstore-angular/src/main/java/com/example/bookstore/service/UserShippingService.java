/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.service;

import com.example.bookstore.domain.UserShipping;

/**
 * @author ss186235
 * @version $Id: $
 */
public interface UserShippingService {
    
    UserShipping findById(Long id);
    
    void removeById(Long id);

}
