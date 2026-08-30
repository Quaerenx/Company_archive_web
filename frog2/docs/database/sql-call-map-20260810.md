# Archive SQL call map

Date: 2026-08-10
Scope: `/opt/frog2-dev/repo/frog2` static and mock-only inspection
Database access during this work: none

## Common request chain

Every mapped request crosses the filters in this order:

`CharacterEncodingFilter -> RequestTimingFilter -> ApplicationExceptionFilter -> SecurityHeadersFilter -> AuthFilter -> CsrfFilter -> Servlet`

- `AuthFilter` requires a session principal outside its public-path allowlist.
- `CsrfFilter` validates state-changing methods.
- `ApplicationExceptionFilter` converts SQLState `25006` into HTTP `409` with code `read_only`.
- `HEAD` uses the same read path as `GET`; the GET purity contract therefore covers both.

## HTTP to SQL map

| URL | Method | Controller path | DAO path | SQL class | Ownership source |
| --- | --- | --- | --- | --- | --- |
| `/login` | GET | `LoginServlet.doGet` | none | none | none |
| `/login` | POST | `LoginServlet.doPost` | `UserDAO.authenticateUser` | SELECT | submitted login ID is authentication input only |
| `/logout` | POST | `LogoutServlet.doPost` | none | none | session |
| `/dashboard` | GET/HEAD | `DashboardServlet.doGet` | `MaintenanceRecordDAO`, `CustomerDAO` | SELECT | session; dashboard data is shared read data |
| `/search` | GET/HEAD | `GlobalSearchServlet.doGet` | `CustomerDAO`, `TroubleshootingDAO`, `MeetingRecordDAO`; customer-history and file repositories | SELECT + filesystem reads | authenticated session; shared read data, no query values in logs |
| `/customers` | GET/HEAD | `CustomersServlet -> CustomerQueryController` | `CustomerDAO`, `CustomerDetailDAO`, `VerticaEosDAO` | SELECT | session; shared customer data |
| `/customers` | POST | `CustomersServlet -> CustomerCommandController -> CustomerCommandService` | `CustomerDAO`, `CustomerDetailDAO` | SELECT + INSERT/UPDATE | session; no request owner ID |
| `/maintenance` | GET/HEAD | `MaintenanceServlet.doGet` | `CustomerDAO`, `MaintenanceRecordDAO` | SELECT | session userId for edit authorization |
| `/maintenance` | POST | `MaintenanceServlet.doPost` | `MaintenanceRecordDAO` | INSERT/UPDATE/DELETE | session `userId` written/compared in SQL |
| `/meeting` | GET/HEAD | `MeetingServlet.doGet` | `MeetingRecordDAO`, `MeetingCommentDAO` | SELECT only | session userId for edit authorization |
| `/meeting` | POST | `MeetingServlet.doPost` | `MeetingRecordDAO` | INSERT/UPDATE/DELETE | session `userId` written/compared in SQL |
| `/comment` | POST | `CommentServlet.doPost` | `MeetingCommentDAO` | INSERT/UPDATE/DELETE | session `userId` written/compared in SQL |
| `/troubleshooting` | GET/HEAD | `TroubleshootingServlet.doGet` | `TroubleshootingDAO`, `CustomerDAO` | SELECT | session userId for edit authorization |
| `/troubleshooting` | POST | `TroubleshootingServlet.doPost` | `TroubleshootingDAO` | INSERT/UPDATE/DELETE | session `userId` written/compared in SQL |
| `/mypage` | GET/HEAD | `MyPageServlet.doGet` | `UserDAO`, `CustomerDAO`, `MaintenanceRecordDAO`, `TroubleshootingDAO`, `MonthlyCustomerResponseDAO`, `UserVmHostDAO` | SELECT | session `userId`; stored user name is used only to match main/sub customer assignments for the read-only work inbox |
| `/mypage` | POST | `MyPageServlet.doPost` | `UserDAO`, `MonthlyCustomerResponseDAO` | SELECT + INSERT/UPDATE/DELETE | session `userId` only |
| `/vm-hosts` | GET/HEAD | `UserVmHostServlet.doGet` | `UserVmHostDAO` | SELECT | session `userId` in SQL |
| `/vm-hosts` | POST | `UserVmHostServlet.doPost` | `UserVmHostDAO` | SELECT + INSERT/UPDATE/DELETE | session `userId` in SQL; request owner ignored |
| `/file-repository*` | GET/HEAD/POST | file repository servlets | none | none | filesystem repository only |
| `/admin/pool-status` | GET/HEAD | `PoolMonitorServlet.doGet` | `DBConnection` | connection acquisition only; no statement | admin session |
| startup | listener | `AppLifecycleListener` | `DatabaseSchemaReadiness` | JDBC metadata only | none |

## DAO inventory

| Component | SELECT | INSERT | UPDATE | DELETE | Runtime DDL |
| --- | --- | --- | --- | --- | --- |
| `CustomerDAO` | yes | yes | yes | logical delete via UPDATE | no |
| `CustomerDetailDAO` | yes | yes | yes | no | no |
| `MaintenanceRecordDAO` | yes | yes | yes | yes | no |
| `MeetingRecordDAO` | yes | yes | yes | yes | no |
| `MeetingCommentDAO` | yes | yes | yes | yes | no |
| `TroubleshootingDAO` | yes | yes | yes | yes | no |
| `MonthlyCustomerResponseDAO` | yes | yes | yes | yes | no |
| `UserDAO` | yes | no | yes | no | no |
| `UserVmHostDAO` | yes | yes | yes | yes | no |
| `VerticaEosDAO` | yes | no | no | no | no |
| `DatabaseSchemaReadiness` | metadata | no | no | no | no |

## Purity conclusion

- Runtime Java/JSP/listener/constructor paths contain no database DDL.
- The former meeting detail view counter UPDATE was removed; every mapped GET/HEAD path is now read-only.
- No last-access, implicit statistics, or hidden counter DML remains in a GET handler.
- `GetRequestDatabasePurityContractTest` prevents known mutating DAO calls from returning to any servlet `doGet` block and prevents runtime DDL literals.
- State-changing SQL remains reachable only from POST/controller command paths and is blocked by `ApplicationEnvironment` plus `ReadOnlyJdbcGuard` in development.

## Error and atomicity contract

- Owner-bound UPDATE/DELETE statements include both the object ID and session-derived stable owner userId in one SQL statement.
- A zero-row result is intentionally reported as “missing or forbidden” without a second existence query. This avoids both a race and object-existence disclosure.
- HTML controllers retain their existing redirect/flash-message behavior; JSON read-only violations remain `409 read_only`.
