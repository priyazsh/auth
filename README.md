# Auth Center

OAuth2/OIDC login for apps like GoLink. `server/` is a Spring Boot API that signs ES256 tokens. `client/` is the Next.js login page. Google runs only when the browser has no Auth Center session.

The browser keeps an HttpOnly `SESSION` cookie. Tokens go to the relying app's server, not to page JavaScript.

- [Architecture](ARCHITECTURE.md)
- [Run locally](LOCAL.md)

