Environment Setup for Testing SqlStore with PostgreSQL 9.4
==========================================================

Preparation consists of just adding an SQLSTORE database/schema/user in the
database.

It can be done on command-line. For example, on Ubuntu:

```
sudo -u postgres createuser sqlstore
sudo -u postgres --owner=sqlstore --encoding=UTF-8 createdb sqlstore
```
Or as an administrative user on the database console (`psql`):

```
CREATE ROLE sqlstore;
CREATE DATABASE sqlstore WITH OWNER = sqlstore ENCODING = 'UTF8'
CREATE SCHEMA sqlstore AUTHORIZATION sqlstore;
```

The SQLSTORE database may be dropped when it is not needed anymore for testing.
