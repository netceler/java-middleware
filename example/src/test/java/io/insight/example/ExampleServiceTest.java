package io.insight.example;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


public class ExampleServiceTest {

  ExampleServiceChain chain;

  ExampleServiceMiddleware md1;
  ExampleServiceMiddleware md2;

  @Before
  public void setup(){
    chain = new ExampleServiceChain();
    md1=new ExampleServiceMiddleware() {
      @Override
      public Integer foo(Long a, Long b, FooInvocation invocation) throws IOException {
        return 2 * invocation.next();
      }

      @Override
      public void bar(Integer c, BarInvocation invocation) {
        invocation.next();
      }
    };
    md2 = mock(ExampleServiceMiddleware.class);
    chain.setServices(md1, md2);
  }

  @Test
  public void test() throws IOException {
    when(md2.foo(eq(1L), eq(2L), anyObject())).thenReturn(3);
    assertEquals(6, chain.foo(1L, 2L).intValue());
    verify(md2).foo(eq(1L), eq(2L), anyObject());
    chain.bar(10);
    verify(md2).bar(10,null);
  }
}