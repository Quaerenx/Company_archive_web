# Archive authorization policy

Status: accepted development baseline on 2026-08-10.

This document records the authorization behavior that is already implemented.
It does not introduce a new role model or expand access. Database read-only
mode, CSRF checks, and input validation remain independent mandatory controls.

## Principals and default rule

- The only authenticated principal is the typed `UserDTO` stored by
  `SessionPrincipal`.
- Login, public static assets, and configured error pages are public.
- Every other application route requires an authenticated session.
- A missing or malformed session fails closed: HTML requests redirect to the
  login page and JSON requests return HTTP 401.
- No department or job-title value grants elevated access.

## Access matrix

| Area | Read | Create | Update/delete |
| --- | --- | --- | --- |
| Dashboard | Any authenticated user | Not applicable | Not applicable |
| Customers | Any authenticated user | Any authenticated user | Current primary or secondary customer assignee |
| Maintenance records | Any authenticated user | Current primary or secondary customer assignee; creator ID comes from the session | Current primary or secondary customer assignee |
| Troubleshooting | Any authenticated user | Current primary or secondary customer assignee; creator ID comes from the session | Current primary or secondary customer assignee |
| Meeting records | Any authenticated user | Any authenticated user; author ID comes from the session | Stable author `userId` only |
| Meeting comments | Any authenticated user | Any authenticated user; author ID comes from the session | Stable author `userId` only |
| My page and monthly responses | Current user's data | Current user | Current stable `userId` only |
| Personal VM hosts | Current user's data | Current user | Current stable `userId` only |
| File repository | Any authenticated user | Any authenticated user | No delete operation is exposed |
| Connection-pool monitor | Configured administrator IDs only | Not applicable | Not applicable |

Customer-linked mutations authorize against the stable primary or secondary assignee `userId`. Record updates and deletes combine the object ID with the server-loaded customer name, so a request cannot select a different record by forging a customer name. Creator and author IDs remain immutable audit provenance, not customer-level authorization keys. Personal records continue to combine the object ID and stable owner `userId` in one mutation. Display names are used only on installations that have not yet added the stable assignment columns.

## Administrator rule

`/admin/pool-status` is the only administrator-only route. Administrator IDs
are read from `frog2.adminUserIds` or `FROG2_ADMIN_USER_IDS`. An absent or empty
configuration grants access to nobody. IDs are matched exactly after trimming;
there is no default administrator account.

## Environment guard

The development deployment has `frog2.env=dev` and `frog2.readOnly=true`.
Therefore all database mutations are rejected even when the authenticated user
would otherwise be authorized. Write-capable behavior requires both a
write-capable environment (`prod` or `staging`) and an explicit
`frog2.readOnly=false` setting.

## Verification contracts

The baseline is covered by the following focused tests:

- `AuthFilterTest` and `SessionPrincipalTest`: authentication and typed session
  principal behavior.
- `AdminAccessPolicyTest`: fail-closed administrator matching.
- `MaintenanceServletAuthorizationTest` and `OwnershipMutationDAOTest`:
  stable-ID, single-statement maintenance, meeting, and comment ownership.
- `TroubleshootingServletAuthorizationTest` and
  `TroubleshootingDAOOwnershipTest`: stable-ID troubleshooting ownership.
- `MonthlyCustomerResponseDAOContractTest`: current-user ownership for monthly
  customer responses.
- `CsrfFilterTest`: authenticated mutations still require a valid CSRF token.

## Deferred product decision

Customer, maintenance, meeting, troubleshooting, and file-repository reads remain shared by all authenticated employees. Customer-linked writes use the stable primary and secondary assignee IDs. Meeting records, comments, monthly responses, and personal VM hosts remain owner-scoped because those records have no authoritative customer assignment relationship. Department text and URL visibility never grant privilege.
