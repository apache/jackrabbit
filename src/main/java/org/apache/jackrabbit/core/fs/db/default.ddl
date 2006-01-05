create table ${schemaObjectPrefix}FSENTRY (FSENTRY_PATH varchar not null, FSENTRY_NAME varchar not null, FSENTRY_DATA varbinary null, FSENTRY_LASTMOD bigint not null, FSENTRY_LENGTH bigint not null)
create unique index ${schemaObjectPrefix}FSENTRY_IDX on ${schemaObjectPrefix}FSENTRY (FSENTRY_PATH, FSENTRY_NAME)
