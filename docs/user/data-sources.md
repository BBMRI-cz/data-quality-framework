# Data Sources

The Data Quality Agent connects to a data source and runs data quality checks against it. The `databaseType` setting selects between two families:

- **FHIR** — a FHIR R4 server queried with CQL.
- **SQL** — a relational database or a directory of CSV files queried with SQL.

## FHIR servers

When `databaseType` is `FHIR`, the agent acts as a FHIR R4 REST client and runs CQL checks against the configured server:

| Setting          | Description                                                        |
|:-----------------|:-------------------------------------------------------------------|
| `fhirUrl`        | Base URL of the FHIR R4 server (e.g. `http://localhost:8080/fhir`). |
| `fhirUsername`   | Optional username for basic authentication.                        |
| `fhirPassword`   | Optional password for basic authentication (Base64 encoded).       |

Only the `databaseType=FHIR` configuration is required to use this data source; the agent connects to the server at `fhirUrl` and authenticates with the provided credentials when given.

## SQL driver selection

When `databaseType` is `SQL`, the agent looks at the JDBC URL (`sqlUrl`):

- **URLs starting with `jdbc:calcite:`** are handled by Apache Calcite. Only two sub-forms are accepted (see below); anything else under that prefix is rejected.
- **Any other `jdbc:` URL** is passed to the standard JDBC `DriverManager` and uses whichever driver is on the classpath. The agent ships with these drivers bundled:

| JDBC URL prefix               | Driver / engine          |
|:------------------------------|:-------------------------|
| `jdbc:postgresql://...`       | PostgreSQL               |
| `jdbc:mysql://...`            | MySQL                    |
| `jdbc:sqlite:...`             | SQLite                   |
| `jdbc:calcite:directory=<dir>`| Apache Calcite / CSV      |

For the standard JDBC URLs the agent connects using the supplied username/password. Additional database drivers can be supported by adding them to the agent's classpath — no code change is required, since these URLs simply go through `DriverManager`.

## Querying CSV files with Calcite

Only these two Calcite URL forms are supported:

| Form                              | Purpose                                              |
|:----------------------------------|:-----------------------------------------------------|
| `jdbc:calcite:directory=<dir>`    | Query a directory of CSV files (auto-generated model) |
| `jdbc:calcite:model=<model-file>` | Query using a custom Calcite JSON/YAML model file     |


**Example**

```
databaseType: SQL
sqlUrl: jdbc:calcite:directory=/opt/omop/csv
```

If `/opt/omop/csv/person.csv` and `/opt/omop/csv/specimen.csv` exist, checks such as `SELECT COUNT(*) FROM person` work as expected.

### CSV conventions

- **Header row**: the first row must be the column names; column types are inferred from the values.
- **Table names**: each file becomes a table named after the file's base name (e.g. `person.csv` → `person`).
- **Case sensitivity**: the `directory=` form uses `lex=MYSQL`, so unquoted identifiers are matched case-insensitively and the common OMOP lowercase style (`person`, `specimen`) works out of the box.

### Custom model file

For more control (custom delimiters, explicit schemas, multiple directories), use a standard Calcite model file instead:

```json
{
  "version": "1.0",
  "defaultSchema": "CSV",
  "schemas": [
    {
      "name": "CSV",
      "type": "custom",
      "factory": "org.apache.calcite.adapter.csv.CsvSchemaFactory",
      "operand": { "directory": "/opt/omop/csv" }
    }
  ]
}
```

```
sqlUrl: jdbc:calcite:model=/opt/omop/model.json
```

Note that with a custom model file, identifier quoting and case-sensitivity follow the conventions defined by the model rather than the defaults applied automatically to the `directory=` form.
