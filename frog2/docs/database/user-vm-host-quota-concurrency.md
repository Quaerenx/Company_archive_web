# User VM host quota and IP concurrency

Status: code hardening implemented; database enforcement is a reviewed design
only. No SQL in this document has been executed.

## Current guarantee

`UserVmHostDAO` uses 64 fixed lock stripes keyed by namespaced mutation
resources. A save locks the stable owner user ID, the target IP, and (for an
update) the original IP. A delete locks its owner and IP. Duplicate stripe
indexes are removed, all stripes are acquired in ascending index order, and
they are released in reverse order from `finally`.

The array is static, so all DAO instances in one application classloader share
the same bounded lock registry. Hash collisions only serialize unrelated
mutations; they do not weaken either invariant.

This provides the following exact guarantee:

- concurrent creates for the same owner are serialized within one Archive JVM;
- the `COUNT(*)` check and subsequent `INSERT` cannot overlap for that owner in
  that JVM;
- different owners cannot concurrently claim the same target IP through this
  DAO in that JVM;
- updates serialize both the IP being released and the IP being claimed, and
  deletes use the same owner/IP lock contract;
- the lock registry remains exactly 64 entries and cannot grow with user count;
- deterministic acquisition order prevents update paths that touch multiple IP
  stripes from acquiring those locks in opposite order.

It does **not** provide a durable database invariant. Two Archive JVMs, a
maintenance script, or any other writer that bypasses this DAO can still race
and can still create duplicate IPs or more than 20 active rows for one owner.
The application lock is therefore an immediate risk reduction, not the final
database contract.

The initial migration declares `ip VARCHAR(15) PRIMARY KEY` but does not state
`ENABLED`. Constraint enforcement must therefore be verified in the target
Vertica catalog rather than assumed from the DDL text. The application remains
correct within one JVM even when that constraint is informational, but a
multi-JVM deployment still requires an explicitly enforced database
constraint.

## Why an exclusive table lock was not added

Vertica supports `LOCK TABLE ... IN EXCLUSIVE MODE` and documents that an X
lock lasts until commit or rollback. `SELECT ... FOR UPDATE` also acquires an X
table lock. Either form would serialize writers across JVMs, including when the
table or owner has no rows.

That lock is table-wide, not owner-scoped. Applying it to every personal-host
save would make unrelated users wait and could also delay other reads or writes.
The application therefore does not introduce that operational bottleneck.

References:

- <https://docs.vertica.com/24.3.x/en/sql-reference/statements/lock-table/>
- <https://docs.vertica.com/24.3.x/en/admin/db-locks/deadlocks/>

## Durable enforcement proposal

Vertica supports enforced primary-key, `CHECK`, and multi-column `UNIQUE`
constraints. A follow-up migration should first make IP uniqueness explicit,
then represent the quota as 20 owner slots instead of trying to put an aggregate
`COUNT(*)` in a check constraint.

Proposed transition:

1. Stop if any owner already has more than 20 active rows, or if duplicate IPs
   exist. Do not repair those rows automatically.
2. In a new migration, explicitly enable the existing IP primary key or replace
   it with a named, explicitly enabled primary-key/unique constraint. Do not
   rewrite the already checksummed initial migration.
3. Add a nullable `owner_slot SMALLINT` column.
4. Backfill active rows deterministically with slots 1 through 20 per owner.
5. Add and explicitly enable constraints equivalent to:
   - active rows require a non-null slot;
   - slots must be between 1 and 20;
   - `(owner_user_id, owner_slot)` is unique.
6. Update inserts to select the lowest free slot and retry a bounded number of
   times when the enforced unique constraint reports a collision.
7. Only after an isolated concurrency test succeeds, make the capability
   required in schema readiness and deploy the new write path.

Inactive rows, if they are introduced later, must clear `owner_slot` so a slot
can be reused. Current application deletion is physical and current writes use
`ACTIVE`, so this is a future compatibility rule rather than a current data
change.

Vertica constraint references:

- <https://docs.vertica.com/24.3.x/en/admin/constraints/constraint-enforcement/>
- <https://docs.vertica.com/24.3.x/en/admin/constraints/supported-constraints/unique-constraints/>
- <https://docs.vertica.com/26.1.x/en/sql-reference/statements/create-statements/create-table/column-constraint/>

## Migration safety gate

Before writing an active migration artifact:

- export the real table DDL and constraint names from an isolated snapshot;
- confirm the target Vertica version supports the exact `ENABLED` syntax;
- measure the lock and projection cost of enabling constraints;
- prove zero duplicate IPs, zero owners above 20, and zero duplicate owner-slot
  pairs;
- document forward application order and rollback order;
- do not execute the migration against the shared database without separate
  approval and a maintenance window.

The current migration manifest remains unchanged.
