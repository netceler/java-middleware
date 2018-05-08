# java-middleware  
a code generator for generating codes like express [middleware](https://expressjs.com/en/guide/writing-middleware.html) 

# How to use
1. add annotation to your interface class

```
import io.insight.Middleware;

@Middleware
public interface ExampleService {
  Integer foo(Long a, Long b) throws IOException;

  void bar(Integer c);
}
```
2. compile your code

`ExampleServiceMiddleware` and `ExampleServiceChain` will be generated

3. use `ExampleServiceChain` as your default implementation, expose it to your user

4. your user can implement `ExampleServiceMiddleware` and add them to `ExampleServiceChain`

see example

