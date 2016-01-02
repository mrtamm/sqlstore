Environment Setup for Testing SqlStore with Derby 10.12
=======================================================

There are no preparation steps for running SqlStore database tests.

The path, where the database folder will be created, is defined in the
`test.properties` file. By default, the folder will be below the target-folder,
so that `mvn clean` command always deletes the folder with its content.

The database will run in embedded mode. SqlStore database will be prepared by
the test class.
