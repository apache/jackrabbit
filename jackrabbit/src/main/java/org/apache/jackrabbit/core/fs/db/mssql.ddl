create table ${schemaObjectPrefix}FSENTRY (FSENTRY_PATH varchar(2048) not null, FSENTRY_NAME varchar(255) not null, FSENTRY_DATA image null, FSENTRY_LASTMOD bigint not null, FSENTRY_LENGTH bigint not null)
create unique index ${schemaObjectPrefix}FSENTRY_IDX on ${schemaObjectPrefix}FSENTRY (FSENTRY_PATH, FSENTRY_NAME)
