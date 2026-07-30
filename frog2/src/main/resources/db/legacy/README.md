# Legacy SQL references

These files were previously packaged under `WEB-INF/classes/sql`. They are retained for historical review only and are not compatible as an automatic migration set. In particular, `create_monthly_customer_response.sql` uses MySQL-specific syntax while the application runtime uses Vertica.

The application never discovers or executes files in this directory.
