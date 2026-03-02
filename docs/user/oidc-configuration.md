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
    - Ensure tokens include `client_id` for client credentials flow or `sub` for authorization code flow
    - Obtain the issuer URI from your OIDC provider (e.g., `https://your-oidc-provider.com`)
    - Verify the OIDC provider exposes a JWKS endpoint at `{issuer-uri}/.well-known/jwks.json` for token signature validation

2. **Configure OIDC Settings via Settings Page**
   
   - Log in to the Data Quality Server as an administrator
   - Navigate to **Settings → OIDC Settings**
   - Configure the following settings:
      - **OIDC Authority**: The OIDC provider's base URL (e.g., `https://your-oidc-provider.com`)
      - **Client ID**: Your application's client identifier registered with the OIDC provider
      - **Redirect URI**: The callback URL for frontend authentication (e.g., `http://localhost:5173/logged-in`)
      - **Scopes**: Space-separated list of OAuth scopes
        - **Required**: `openid profile offline_access`
      - **Swagger Redirect URL**: The OAuth callback URL for Swagger UI (e.g., `http://localhost:5173/api/swagger-ui/oauth2-redirect.html`)
   
   **Note**: Changes to OIDC settings are applied immediately for the main application. A backend restart is **only required** if you want OIDC authentication to work on the Swagger API documentation page.

   ::: warning Important Configuration Notes
   - The OIDC authority URL must be accessible from the server (network connectivity required)
   - The server will fetch OIDC provider metadata from `{authority}/.well-known/openid-configuration`
   - Ensure the authority URL does not end with a trailing slash
   :::

3. **(Optional) Restart Server for Swagger UI OIDC Support**

   If you want to use OIDC authentication on the Swagger API documentation page, restart the backend server after updating OIDC settings:

   ```bash
   docker compose restart quality-server
   ```
   
   Or for a full restart:
   
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
- Verify the OIDC authority URL is correct and accessible: `curl {authority}/.well-known/openid-configuration`
- Check that the OIDC provider's discovery endpoint returns valid JSON
- Ensure redirect URIs are properly configured in the OIDC provider
- Verify network connectivity from server to OIDC provider
- Review server logs for specific error messages: `docker compose logs quality-server`

**Settings changes not taking effect:**
- OIDC configuration changes are applied immediately for the main application authentication
- The frontend, user authentication, and API endpoints automatically use the updated OIDC settings without restart
- A backend restart is only required for OIDC authentication to work on the Swagger API documentation page:
  ```bash
  docker compose restart quality-server
  ```

For general deployment information, see the [Deployment Guide](./deployment.md).


