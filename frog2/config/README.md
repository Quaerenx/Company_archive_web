# External runtime configuration

Frog2 does not package database credentials in the WAR. Start Tomcat with the JVM system property below, pointing to a permission-restricted properties file outside the webroot:

```text
-Dfrog2.config=/absolute/path/to/db.properties
```

Use `db.properties.sample` as the key-only template. Never commit a populated `db.properties` file.
