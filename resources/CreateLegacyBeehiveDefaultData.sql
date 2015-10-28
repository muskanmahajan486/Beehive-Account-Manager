INSERT INTO role VALUES (1, 'service-admin');
INSERT INTO role VALUES (2, 'account-owner');

INSERT INTO account VALUES (1);
INSERT INTO user VALUES (1, md5('password{admin}'), 'admin', 1, 'support@openremote.org', now(), NULL, 1);
INSERT INTO user_role VALUES (1, 1);

INSERT INTO account VALUES (2);
INSERT INTO user VALUES (2, md5('password{user}'), 'user', 2, 'support@openremote.org', now(), NULL, 1);
INSERT INTO user_role VALUES (2, 2);