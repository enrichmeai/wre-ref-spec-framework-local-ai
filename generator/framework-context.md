# What the framework gives you (read before writing code)

Package of the service: `com.enrichmeai.ref.shipping`.

Already written, do not write again:
- `public record Quote(String zone, int weightKg, int pricePence) {}`
- `ShippingQuoteController` calls `service.quote(zone, weightKg)` and returns the `Quote`.
- Error handling: the platform maps any `DomainException` to an HTTP problem detail with a `code`. Do not write
  `@ExceptionHandler`, `@ControllerAdvice` or `@RestControllerAdvice`.

Throw errors with the platform's factory methods; never create exception classes yourself:
```java
import com.enrichmeai.ref.platform.Errors;
throw Errors.notFound("SOME_CODE", "what was not found");        // 404
throw Errors.unprocessable("SOME_CODE", "which rule was broken");  // 422
```

The platform rules fail the build on: a RuntimeException class in the service that does not extend DomainException, any error handler,
`System.out` or `System.err`, and database access from a controller.

You fill ONE slot: the body of this method. The rest of the file is the template and is fixed:
```java
package com.enrichmeai.ref.shipping;

import com.enrichmeai.ref.platform.Errors;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ShippingQuoteService {

    public Quote quote(String zone, int weightKg) {
        // SLOT: quote
    }
}
```
Reply with only the statements that replace `// SLOT: quote`, in one ```java code block, and nothing else.
No class, no imports, no method signature.
