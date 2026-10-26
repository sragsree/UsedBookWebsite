/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.example.bookstore.domain.Book;

public interface BookRepository extends CrudRepository<Book, Long>{
    List<Book> findByTitleContaining(String keyword);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from Book b where b.id = :id")
    java.util.Optional<Book> lockById(@org.springframework.data.repository.query.Param("id") Long id);
}
