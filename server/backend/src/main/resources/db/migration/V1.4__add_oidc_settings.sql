-- Insert default OIDC settings
INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcAuthority', 'http://localhost:4011');

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcClientId', 'auth-code-client');

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcRedirectUri', 'http://localhost:8082/logged-in');

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcPostLogoutRedirectUri', 'http://localhost:8082');

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcScopes', 'openid profile email permissions some-app-scope-1');

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcSilentRedirectUri', 'http://localhost:8082/silent-renew');
