# Legacy SQL references

These files were previously packaged under `WEB-INF/classes/sql`. They are retained for historical review only and are not compatible as an automatic migration set. In particular, `create_monthly_customer_response.sql` uses MySQL-specific syntax while the application runtime uses Vertica.

The application never discovers or executes files in this directory.

`V20260720_02__create_hosts.sql` and
`V20260720_03__add_hosts_row_color.sql` belonged to the removed
`HostDAO`/`HostDTO` implementation. They remain here unchanged as historical
schema references and must not be executed as active migrations.
