-- Dev environment OIDC settings

UPDATE setting SET setting_value = 'http://localhost:4011' WHERE setting_name = 'oidcAuthority';
UPDATE setting SET setting_value = 'auth-code-client' WHERE setting_name = 'oidcClientId';
UPDATE setting SET setting_value = 'http://localhost:5173/logged-in' WHERE setting_name = 'oidcRedirectUri';
UPDATE setting SET setting_value = 'http://localhost:5173/login' WHERE setting_name = 'oidcPostLogoutRedirectUri';
UPDATE setting SET setting_value = 'openid profile email offline_access permissions some-app-scope-1' WHERE setting_name = 'oidcScopes';
UPDATE setting SET setting_value = 'OIDC Mock' WHERE setting_name = 'oidcAuthorityName';
UPDATE setting SET setting_value = 'http://localhost:5173/api/swagger-ui/oauth2-redirect.html' WHERE setting_name = 'oidcSwaggerRedirectUrl';