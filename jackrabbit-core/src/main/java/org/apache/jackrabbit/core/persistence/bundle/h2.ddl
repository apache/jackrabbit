create cached table ${schemaObjectPrefix}BUNDLE (NODE_ID binary(16) PRIMARY KEY, BUNDLE_DATA varbinary not null)
create cached table ${schemaObjectPrefix}REFS (NODE_ID binary(16) PRIMARY KEY, REFS_DATA varbinary not null)
create cached table ${schemaObjectPrefix}BINVAL (BINVAL_ID varchar(64) PRIMARY KEY, BINVAL_DATA blob not null)
create cached table ${schemaObjectPrefix}NAMES (ID INTEGER AUTO_INCREMENT PRIMARY KEY, NAME varchar(255) not null)