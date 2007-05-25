create table ${schemaObjectPrefix}JOURNAL (REVISION_ID BIGINT NOT NULL, JOURNAL_ID varchar(255), PRODUCER_ID varchar(255), REVISION_DATA IMAGE)
create unique index ${schemaObjectPrefix}JOURNAL_IDX on ${schemaObjectPrefix}JOURNAL (REVISION_ID)
create table ${schemaObjectPrefix}GLOBAL_REVISION (REVISION_ID BIGINT NOT NULL)
create unique index ${schemaObjectPrefix}GLOBAL_REVISION_IDX on ${schemaObjectPrefix}GLOBAL_REVISION (REVISION_ID)

# Inserting the one and only revision counter record now helps avoiding race conditions
insert into ${schemaObjectPrefix}GLOBAL_REVISION VALUES(0)
