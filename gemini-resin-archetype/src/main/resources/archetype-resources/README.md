Welcome to your new Gemini project.

# Resin
Gemini can work with any [Servlet Container](https://en.wikipedia.org/wiki/Web_container). We recommend [Resin](http://caucho.com/products/resin/download/gpl) because it is the [fastest](https://www.techempower.com/benchmarks/). There are installers available, but it is simplest to download the .zip or .tar.gz file, extract it to a location on your drive (in our example here, "/home/me/opt/resin"), and launch it from within your IDE.

# Database
Gemini can work with any database with a JDBC driver. We have provided starter migration files for [PostgreSQL](https://www.postgresql.org/) but it is simple enough to port these to the database of your choice. This starter Gemini project is configured to use a development database with these attributes you provided during the Maven archetype generation:
  * Name: ${databaseName}
  * Host: ${databaseHost}
  * Port: ${databasePort}
  * Username: ${databaseUsername}

To change these defaults, edit WEB-INF/Configuration/${machineName}.conf

Be sure to add the Postgres driver dependency to your pom.xml file.

In PostgreSQL, these commands will create the default database and credentials:
  `CREATE USER ${databaseUsername} WITH PASSWORD '${databasePassword}';`
  `CREATE DATABASE ${databaseName} OWNER ${databaseUsername};`

Once this is prepared, on first startup your Gemini application will automatically apply the necessary schema migrations to this new database using [Flyway](https://flywaydb.org/).
