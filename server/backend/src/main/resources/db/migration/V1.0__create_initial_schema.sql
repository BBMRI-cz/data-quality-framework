CREATE TABLE user_account
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

INSERT INTO user_account (id, username, password) VALUES
    (102,'admin', '$argon2id$v=19$m=19456,t=2,p=1$SQGK8wXpQw5b+qjuq/Ih1A$WP87YsUIErq6O+7rMk65U0cH4OHBRdrnM3yIG50gwpE');
