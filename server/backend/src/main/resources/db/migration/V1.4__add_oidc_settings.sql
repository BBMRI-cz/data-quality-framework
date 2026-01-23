-- Allow NULL values in setting_value column
-- This is needed for OIDC settings that may not be configured yet

CREATE TABLE setting_new (
    setting_name TEXT PRIMARY KEY,
    setting_value TEXT NULL
);

INSERT INTO setting_new (setting_name, setting_value)
SELECT setting_name, setting_value FROM setting;

DROP TABLE setting;

ALTER TABLE setting_new RENAME TO setting;

-- Insert OIDC settings with NULL values (to be configured per deployment)
INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcAuthority', NULL);

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcClientId', NULL);

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcRedirectUri', NULL);

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcPostLogoutRedirectUri', NULL);

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcScopes', NULL);

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcSilentRedirectUri', NULL);

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcAuthorityName', NULL);

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcAuthorityLogo', NULL);
