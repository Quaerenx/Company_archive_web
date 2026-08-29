# Archive URL authorization matrix

Status: verified against `WEB-INF/web.xml`, filters, and Servlet handlers on
2026-08-20. This document records the accepted behavior; it does not introduce
a new role model.

## Global rules

- `AuthFilter` allows only `/login`, `/login.jsp`, configured error pages, and
  GET/HEAD requests for allowlisted static assets without authentication.
- All other routes require a typed `UserDTO` session principal.
- `CsrfFilter` requires the session CSRF token for every method except GET,
  HEAD, and OPTIONS. Unsupported methods are still rejected by the target
  Servlet/container.
- Development read-only mode remains an independent guard. Authorization never
  bypasses the database write guard.
- Shared business reads are intentionally available to every authenticated
  employee. No role is inferred from a display name or a hidden UI control.

## Configured routes

| Route / operation | Method | Access | Effect | CSRF | Ownership |
| --- | --- | --- | --- | --- | --- |
| `/login` (`/login.jsp` view) | GET | Public | Login form read | No | None |
| `/login` | POST | Public with anonymous session | Authentication/session creation | Yes | Submitted credentials; response does not reveal account existence |
| `/logout` | POST | Authenticated | Session invalidation and cookie expiry | Yes | Current session |
| `/logout` | GET | Authenticated | None; 405 with `Allow: POST` | No | None |
| `/dashboard` | GET | Authenticated | Shared dashboard read | No | None |
| `/customers` list/detail/edit/add forms and JSON actions | GET | Authenticated | Shared customer read | No | None |
| `/customers` `add`, `update`, `delete`, `saveDetail` | POST | Authenticated | Customer mutation | Yes | Accepted policy permits all authenticated users; dev read-only still blocks DB writes |
| `/customer-history` list/add/edit forms | GET | Authenticated | Shared major-work history read | No | All authenticated users may read; no maintenance data is included |
| `/customer-history` `add` | POST | Authenticated | External history-file creation | Yes | Stable creator `userId` comes only from the session |
| `/customer-history` `update`, `delete` | POST | Authenticated | Owner-scoped external history-file mutation | Yes | Record ID and stable creator `userId` are checked atomically in the repository |
| `/maintenance` cards/history/add form | GET | Authenticated | Shared maintenance read | No | Edit form is exposed only to the stable creator `userId` |
| `/maintenance` `add` | POST | Authenticated | Create maintenance record | Yes | Creator comes only from the session `userId` |
| `/maintenance` `update`, `delete` | POST | Authenticated | Owner-scoped mutation | Yes | Object ID and stable creator `userId` are matched in the mutation |
| `/meeting` list/view/write form | GET | Authenticated | Shared meeting read | No | Edit form is exposed only to the stable author `userId` |
| `/meeting` `write` | POST | Authenticated | Create meeting | Yes | Author comes only from the session `userId` |
| `/meeting` `update`, `delete` | POST | Authenticated | Owner-scoped mutation | Yes | Object ID and stable author `userId` are matched in the mutation |
| `/comment` `add` | POST | Authenticated | Create meeting comment | Yes | Author comes only from the session `userId` |
| `/comment` `update`, `delete` | POST | Authenticated | Owner-scoped comment mutation | Yes | Comment ID and stable author `userId` are matched in the mutation |
| `/troubleshooting` list/view/add form | GET | Authenticated | Shared troubleshooting read | No | Edit form is exposed only to the stable creator `userId` |
| `/troubleshooting` `add` | POST | Authenticated | Create troubleshooting record | Yes | Creator comes only from the session `userId` |
| `/troubleshooting` `update`, `delete` | POST | Authenticated | Owner-scoped mutation | Yes | Object ID and stable creator `userId` are matched in the mutation |
| `/mypage` view/edit/change-password/monthly-response | GET | Authenticated | Current-user data read | No | Session `userId` only |
| `/mypage` profile/password/monthly-response mutations | POST | Authenticated | Current-user mutation | Yes | Request user ID is not trusted; session `userId` only |
| `/vm-hosts` | GET | Authenticated | Current-user VM host read | No | Session `userId` only |
| `/vm-hosts` save/delete | POST | Authenticated | Current-user VM host mutation | Yes | IP and stable owner `userId` are matched |
| `/file-repository` | GET | Authenticated | Shared repository listing | No | Shared repository policy |
| `/file-repository/upload` | GET | Authenticated | Upload form read | No | Shared repository policy |
| `/file-repository/upload` | POST | Authenticated | File upload | Yes | Shared repository policy; no delete API |
| `/file-repository/import` | POST | Configured administrator IDs | Index server-copied files | Yes | Exact fail-closed administrator allowlist; current folder and safe descendants only |
| `/file-repository/download` | GET | Authenticated | Attachment-only download | No | Shared repository policy |
| `/admin/pool-status` | GET | Configured administrator IDs | Read-only pool check | No | Exact fail-closed administrator allowlist |
| `/favicon.ico` | GET | Public | Redirect to packaged favicon | No | None |
| configured error pages | GET/error dispatch | Public | Generic error display | No | None |
| allowlisted `/resources/`, `/images/`, `/css/`, `/js/`, `/webjars/` assets | GET/HEAD | Public | Static read | No | Extension and normalized-path allowlist |

## Legacy redirect and compatibility routes

The following legacy URL mappings remain authenticated compatibility aliases.
They execute the same Servlet and security filters as the canonical route.

| Legacy route | Method | Access | Effect | CSRF | Ownership |
| --- | --- | --- | --- | --- | --- |
| `/filerepo/filerepo_downlist.jsp` | GET | Authenticated | Repository listing | No | Shared repository policy |
| `/filerepo/filerepo_upload.jsp` | GET/POST | Authenticated | Upload form/upload | POST only | Shared repository policy |
| `/filerepo/filerepo_uploadProcess.jsp` | POST | Authenticated | Upload | Yes | Shared repository policy |
| `/filerepo/filerepo_download.jsp` | GET | Authenticated | Attachment-only download | No | Shared repository policy |

## Direct JSP surface

Direct JSP requests outside `WEB-INF` are not public: `AuthFilter` requires an
authenticated typed principal. These JSPs are view implementation details and
perform no direct mutation. Moving all of them below `WEB-INF` is a later
hardening refactor because it changes many forwards and links; it is not mixed
into this security patch. JSP fragments and file-repository views already under
`WEB-INF` cannot be requested directly.

## Regression evidence

- `SecurityRouteMatrixContractTest` fails when a configured Servlet route is
  missing from this matrix.
- `AuthFilterTest`, `SessionPrincipalTest`, and `AdminAccessPolicyTest` cover
  authentication, typed principals, and fail-closed administrator access.
- `CsrfFilterTest` and `CsrfWebCoverageTest` cover unsafe methods and form/JS
  token propagation.
- DAO and Servlet ownership tests cover stable-ID mutation predicates.
