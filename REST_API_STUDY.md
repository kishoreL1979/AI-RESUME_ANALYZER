# REST API Design & Spring MVC request-handling Flow

## REST API Best Practices
- Semantic use of HTTP Methods (GET, POST, PUT, DELETE, PATCH).
- Consistent status codes (200 OK, 201 Created, 400 Bad Request, 404 Not Found, 500 Internal Server Error).
- Content negotiation and JSON formatting.

## Spring MVC Request Flow
1. Client sends request to `DispatcherServlet`.
2. `HandlerMapping` finds the correct controller method.
3. Controller executes business logic (delegating to service layer) and returns response.
4. Response body is serialized to JSON (via Jackson/HttpMessageConverter) and returned to client.
