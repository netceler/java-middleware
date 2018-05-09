package io.insight.example;


import io.insight.Middleware;

import java.io.IOException;

@Middleware
public interface ExampleService {
  Integer foo(Long a, Long b) throws IOException;

  void bar();
}
