/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * @author ss186235
 * @version $Id: $
 */

@EnableRedisHttpSession
public class HttpSessionConfig
{
    @Bean
    public LettuceConnectionFactory connectionFactory()
    {
        return new LettuceConnectionFactory();
    }

}
