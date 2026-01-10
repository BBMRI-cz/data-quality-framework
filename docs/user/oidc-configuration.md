# OIDC Authentication Configuration

The Data Quality Server supports OpenID Connect (OIDC) authentication for single sign-on integration with external identity providers.

#### Understanding OIDC Integration

OIDC authentication works alongside the internal authentication system:
- The server validates JWT tokens from both internal and external sources
- Token routing is automatic based on the issuer claim in the JWT
- Users authenticating via OIDC are automatically created in the database on first login
- The subject ID from the OIDC token is used as the unique identifier

#### OIDC Configuration Steps

1. **Configure Your OIDC Provider**

   In your OIDC provider, create a new client/application:
    - **Client Type**: Public or Confidential (depending on your setup)
    - **Redirect URI**: Configure appropriate callback URLs for your deployment
    - **Access Type**: Configure according to your security requirements

2. **Update Docker Compose Configuration**

   Edit your `compose.yaml` file and configure the OIDC issuer URI:

   ```yaml
   services:
     quality-server:
       image: ghcr.io/bbmri-cz/data-quality-server:latest
       container_name: quality-server
       restart: unless-stopped
       ports:
         - "8082:8082"
       volumes:
         - server-data:/app/data
       environment:
         # Enable OIDC authentication
         - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://your-oidc-provider.com

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

4. **Verify OIDC Configuration**

   Check the server logs to confirm OIDC initialization:

**For Internal Users:**
1. Log in via the login form with username/password
2. Server generates a JWT token with internal issuer
3. Token is stored in browser and used for API requests
4. Token validated against internal user database

**For OIDC Users:**
1. User authenticates with external OIDC provider
2. OIDC provider issues JWT token
3. Client sends token to server API
4. Server validates token against OIDC provider's public keys
5. User automatically created in database on first successful authentication

**Token Validation Process:**
- Server examines the `iss` (issuer) claim in the JWT
- If issuer matches internal issuer → validates with internal authentication
- If issuer matches OIDC provider → validates with OIDC provider's JWKS endpoint
- Invalid or expired tokens are rejected with 401 Unauthorized

#### Troubleshooting OIDC

**OIDC authentication not working:**
- Verify the issuer URI is correct and accessible: `curl {issuer-uri}/.well-known/openid-configuration`
- Check that the OIDC provider's discovery endpoint returns valid JSON
- Ensure redirect URIs are properly configured in the OIDC provider
- Verify network connectivity from server to OIDC provider
- Review server logs for specific error messages: `docker compose logs quality-server`

**Server fails to start with OIDC enabled:**
- The server will start even if the OIDC provider is temporarily unavailable
- Internal authentication remains functional if OIDC initialization fails
- Check for configuration syntax errors in the issuer URI
- Verify environment variables are properly set

For general deployment information, see the [Deployment Guide](./deployment.md).


