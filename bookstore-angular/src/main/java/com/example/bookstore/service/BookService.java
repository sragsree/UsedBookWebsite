/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.service;

import java.util.List;

import com.example.bookstore.domain.Book;

public interface BookService {
    
    List<Book> findAll();
    
    Book findOne(Long id);
    
    Book save(Book book);
    
    List<Book> blurrySearch(String title);
    
    void removeOne(Long id);
}