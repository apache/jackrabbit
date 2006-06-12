create table ${schemaObjectPrefix}FSENTRY (FSENTRY_PATH varchar2(2048) not null, FSENTRY_NAME varchar2(255) not null, FSENTRY_DATA blob null, FSENTRY_LASTMOD number(38,0) not null, FSENTRY_LENGTH number(38,0) null)
create unique index ${schemaObjectPrefix}FSENTRY_IDX on ${schemaObjectPrefix}FSENTRY (FSENTRY_PATH, FSENTRY_NAME)
