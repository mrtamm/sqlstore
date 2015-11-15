-- Here are some helpful scripts for preparing PostgreSQL database for the
-- SqlStoreTest. These statements need to be executed using administrator
-- account.

/*
sudo -u postgres createuser sqlstore
sudo -u postgres --owner=sqlstore --encoding=UTF-8 createdb sqlstore
*/

CREATE ROLE sqlstore;
CREATE DATABASE sqlstore WITH OWNER = sqlstore ENCODING = 'UTF8'
CREATE SCHEMA sqlstore AUTHORIZATION sqlstore;
