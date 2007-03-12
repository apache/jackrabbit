create table ${schemaObjectPrefix}BUNDLE (NODE_ID raw(16) not null, BUNDLE_DATA blob not null)
create unique index ${schemaObjectPrefix}BUNDLE_IDX on ${schemaObjectPrefix}BUNDLE (NODE_ID)

create table ${schemaObjectPrefix}REFS (NODE_ID raw(16) not null, REFS_DATA blob not null)
create unique index ${schemaObjectPrefix}REFS_IDX on ${schemaObjectPrefix}REFS (NODE_ID)

create table ${schemaObjectPrefix}BINVAL (BINVAL_ID varchar2(64) not null, BINVAL_DATA blob null)
create unique index ${schemaObjectPrefix}BINVAL_IDX on ${schemaObjectPrefix}BINVAL (BINVAL_ID)

create table ${schemaObjectPrefix}NAMES (ID INTEGER primary key, NAME varchar2(255) not null)
create unique index ${schemaObjectPrefix}NAMES_IDX on ${schemaObjectPrefix}NAMES (NAME)
create sequence ${schemaObjectPrefix}seq_names_id
create trigger ${schemaObjectPrefix}t1 before insert on ${schemaObjectPrefix}NAMES for each row begin select ${schemaObjectPrefix}seq_names_id.nextval into :new.id from dual; end;