# OIDC Authentication Configuration

::: warning Under Construction
This feature is currently under development and the documentation may be incomplete.
:::

The Data Quality Server supports OpenID Connect (OIDC) authentication for single sign-on integration with external identity providers.

#### Understanding OIDC Integration

OIDC authentication works alongside the internal authentication system:
- The server validates JWT tokens from both internal and external sources
- Token routing is automatic based on the issuer claim in the JWT
- Users authenticating via OIDC are automatically created in the database on first login
- The server supports both Authorization Code Flow and Client Credentials Flow OAuth 2.0 grant types

#### OIDC Configuration Steps

1. **Configure Your OIDC Provider (Authorization Server)**
    - Ensure tokens include either `preferred_username` or `client_id`
    - Obtain the issuer URI from your OIDC provider (e.g., `https://your-oidc-provider.com`)
    - Verify the OIDC provider exposes a JWKS endpoint at `{issuer-uri}/.well-known/jwks.json` for token signature validation

2. **Update Docker Compose Configuration**

   Add the OIDC issuer URI to your server's environment variables in `compose.yaml`:

   ```yaml
   environment:
     # Enable OIDC authentication
     - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://your-oidc-provider.com
   ```

   ::: warning Important Configuration Notes
   - The issuer URI must be accessible from the server (network connectivity required)
   - The server will fetch OIDC provider metadata from `{issuer-uri}/.well-known/openid-configuration`
   - Ensure the issuer URI does not end with a trailing slash
   :::

3. **Restart the Server**

   ```bash
   docker compose down
   docker compose up -d
   ```
   
#### OIDC Authentication Flow
1. User authenticates with external OIDC provider
2. OIDC provider issues JWT token
3. Client sends token to server API
4. Server validates token against OIDC provider's public keys
5. User automatically created in database on first successful authentication

#### Troubleshooting OIDC

**OIDC authentication not working:**
- Verify the issuer URI is correct and accessible: `curl {issuer-uri}/.well-known/openid-configuration`
- Check that the OIDC provider's discovery endpoint returns valid JSON
- Ensure redirect URIs are properly configured in the OIDC provider
- Verify network connectivity from server to OIDC provider
- Review server logs for specific error messages: `docker compose logs quality-server`

**Server fails to start with OIDC enabled:**
- The server will start even if the OIDC provider is temporarily unavailable
- Check for configuration syntax errors in the issuer URI
- Verify environment variables are properly set

For general deployment information, see the [Deployment Guide](./deployment.md).


