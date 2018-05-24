/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.domain.security;

import java.io.Serializable;

import org.springframework.security.core.GrantedAuthority;

/**
 * @author ss186235
 * @version $Id: $
 */
public class Authority implements GrantedAuthority, Serializable
{
    private static final long serialVersionUID = 123123L;
    
    private final String authority; 
    
    public Authority(String authority) 
    {
        this.authority = authority;
    }

    /** 
     * @inheritDoc
     */
    @Override
    public String getAuthority()
    {
        return authority;
    }

}
