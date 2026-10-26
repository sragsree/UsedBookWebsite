/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.domain.security;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.example.bookstore.domain.User;

/**
 * @author ss186235
 * @version $Id: $
 */

@Entity
@Table(name="user_role")
public class UserRole implements Serializable {
    private static final long serialVersionUID = 890345L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long userRoleId;
    
    public UserRole () {}
    
    public UserRole (User user, Role role) {
        this.user = user;
        this.role = role;
    }
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.EAGER)
    private Role role;

    public long getUserRoleId() {
        return userRoleId;
    }

    public void setUserRoleId(long userRoleId) {
        this.userRoleId = userRoleId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }   
}
