Welcome to your new Gemini project.

# Resin
Gemini can work with any [Servlet Container](https://en.wikipedia.org/wiki/Web_container). We recommend [Resin](http://caucho.com/products/resin/download/gpl) because it is the [fastest](https://www.techempower.com/benchmarks/). There are installers available, but it is simplest to download the .zip or .tar.gz file, extract it to a location on your drive (in our example here, "/home/me/opt/resin"), and launch it from within your IDE.

# Database

By default, Gemini is configured to have database connectivity disabled, but Gemini can work with any database with a JDBC driver. We have provided starter migration files for [PostgreSQL](https://www.postgresql.org/) but it is simple enough to port these to the database of your choice. To enable database connectivity and migrations, edit the value for `db.Enabled` to `yes` in `WEB-INF/configuration/Base.conf`.

Be sure to add the database driver dependency to your pom.xml file. Example for PostgreSQL:

```
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <version>42.2.2</version>
</dependency>
```

Ensure the following environment variables are set for database connectivity:
```
DB_CONN=localhost:5432/exampleDb
DB_LOGIN=exampleLogin
DB_PASS=examplePassword
```

Once this is prepared, on first startup your Gemini application will automatically apply the necessary schema migrations to this new database using [Flyway](https://flywaydb.org/).
