# Frog2 database migrations

These SQL files are version-controlled migration artifacts only. The application does not discover or execute them at startup or during HTTP requests.

Do not run these migrations against the shared database without a separate approval, a schema baseline review, and a maintenance-window rollback plan. Existing installations must be baselined so migrations for already-present tables or columns are marked as applied rather than executed again.
