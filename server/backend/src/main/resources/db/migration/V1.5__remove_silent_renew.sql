-- Remove unused silent renew configuration
DELETE FROM setting WHERE setting_name = 'oidcSilentRedirectUri';
