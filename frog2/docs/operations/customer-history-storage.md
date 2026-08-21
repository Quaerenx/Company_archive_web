# Customer history storage

The customer history page stores only manually curated major work records such
as incidents, upgrades, and capacity expansions. It does not import or copy
maintenance or troubleshooting records.

## Runtime contract

- Route: `/customer-history`
- JVM property: `frog2.customerHistoryRoot`
- Development root: `/opt/frog2-dev/data/customer-history`
- Records: `<root>/records/<uuid>.properties`
- Storage must remain outside Tomcat `webapps`.
- GET requests do not create or change storage files.
- Add, update, and delete mutate only this external directory; they do not run
  database DDL or DML.
- Updates and deletes require the stable creator `userId` from the session.

Outside the development environment, `frog2.customerHistoryRoot` is required
explicitly. The directory must be writable only by the application service
account and must not be a symbolic link.

## Backup and restore

Back up the entire configured root while preserving file names and UTF-8
contents. Restore it to the configured root before starting the application.
Do not edit record files manually. A malformed record causes the page to fail
closed instead of silently omitting data.

This implementation assumes one Archive Tomcat process owns the configured
local directory. A multi-node deployment requires shared transactional storage
or a database migration designed and approved separately.
