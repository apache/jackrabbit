create table ${schemaObjectPrefix}BUNDLE (NODE_ID_HI bigint not null, NODE_ID_LO bigint not null, BUNDLE_DATA blob(2G) not null, PRIMARY KEY (NODE_ID_HI, NODE_ID_LO))
create table ${schemaObjectPrefix}REFS (NODE_ID_HI bigint not null, NODE_ID_LO bigint not null, REFS_DATA blob(2G) not null, PRIMARY KEY (NODE_ID_HI, NODE_ID_LO))
create table ${schemaObjectPrefix}BINVAL (BINVAL_ID char(64) PRIMARY KEY, BINVAL_DATA blob(2G) not null)
create table ${schemaObjectPrefix}NAMES (ID INTEGER GENERATED ALWAYS AS IDENTITY, NAME varchar(255) not null, PRIMARY KEY (ID, NAME))
