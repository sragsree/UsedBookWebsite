package com.example.bookstore.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class UploadConfig implements WebMvcConfigurer {
  @Value("${bookstore.upload-dir}")
  private String dir;

  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/image/uploads/**")
        .addResourceLocations(Path.of(dir).toAbsolutePath().toUri().toString() + "/");
  }
}
