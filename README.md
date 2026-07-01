# aws-datalake-demo

Aplicacion Java que se conecta a un data lake en S3 catalogado con AWS Glue Data Catalog:

1. Lista, paginado, las tablas de una base del Glue Data Catalog (`GlueCatalogService`).
2. Ejecuta una consulta SQL contra el data lake via Amazon Athena (usando el Glue Data Catalog
   como metastore) y recorre los resultados paginados (`AthenaQueryService`).

## Requisitos

- Java 25 (via sdkman: `sdk env` en la raiz del repo, usa el `.sdkmanrc`).
- Credenciales AWS configuradas con el proveedor por defecto del SDK (perfil de `~/.aws/credentials`,
  variables de entorno `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`, rol de IAM, etc). Esta app **no**
  recibe ni hardcodea credenciales.
- Permisos IAM minimos en el rol/usuario usado:
  - `glue:GetTables`
  - `athena:StartQueryExecution`, `athena:GetQueryExecution`, `athena:GetQueryResults`
  - `s3:GetObject`, `s3:PutObject`, `s3:ListBucket` sobre el bucket de datos y el de resultados de Athena

## Variables de entorno

| Variable | Requerida | Default | Descripcion |
|---|---|---|---|
| `GLUE_DATABASE` | si | - | Base del Glue Data Catalog |
| `GLUE_TABLE` | si | - | Tabla a consultar |
| `ATHENA_OUTPUT_LOCATION` | si | - | `s3://bucket/prefix/` donde Athena escribe resultados |
| `AWS_REGION` | no | `us-east-1` | Region de AWS |
| `ATHENA_QUERY` | no | `SELECT * FROM <GLUE_TABLE>` | Query SQL a ejecutar |
| `PAGE_SIZE` | no | `50` | Tamano de pagina para Glue y Athena |

## Build y ejecucion

```bash
mvn clean package

GLUE_DATABASE=mi_base \
GLUE_TABLE=mi_tabla \
ATHENA_OUTPUT_LOCATION=s3://mi-bucket-resultados/athena/ \
AWS_REGION=us-east-1 \
java -jar target/aws-datalake-demo-1.0-SNAPSHOT.jar
```

## Archivos de resultado en S3

Cada ejecucion de la query escribe en `ATHENA_OUTPUT_LOCATION` un `<query-execution-id>.csv` (el
resultado) y un `<query-execution-id>.csv.metadata` (metadata interna de Athena, tipos de columna,
etc). Esto es propio de como funciona Athena y ocurre siempre, independientemente del `PAGE_SIZE`:
el paginador de `AthenaQueryService` solo trocea la *lectura* del resultado via API
(`GetQueryResults` + `NextToken`), no la escritura en S3 (que es un unico archivo por ejecucion).

Recomendaciones para no acumular basura en el bucket:

- Usar un prefix dedicado, ej. `s3://mi-bucket-resultados/athena/aws-datalake-demo/`, para no
  mezclarlo con resultados de otras queries/equipos.
- Configurar una **lifecycle policy** en ese bucket/prefix que expire los objetos a los N dias
  (por ejemplo 7-30 dias, segun necesidad de auditoria). Esto se maneja por fuera de la app, a
  nivel de infraestructura S3 (Terraform/CloudFormation/consola).
