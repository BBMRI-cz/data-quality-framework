-- Insert default OIDC settings
INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcAuthority', 'http://localhost:4011');

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcClientId', 'auth-code-client');

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcRedirectUri', 'http://localhost:5173/logged-in');

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcPostLogoutRedirectUri', 'http://localhost:5173');

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcScopes', 'openid profile email permissions some-app-scope-1');

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcSilentRedirectUri', 'http://localhost:5173/silent-renew');
