#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
create table ${schemaObjectPrefix}BUNDLE (NODE_ID raw(16) not null, BUNDLE_DATA blob not null) ${tablespace}
create unique index ${schemaObjectPrefix}BUNDLE_IDX on ${schemaObjectPrefix}BUNDLE (NODE_ID) ${indexTablespace}

create table ${schemaObjectPrefix}REFS (NODE_ID raw(16) not null, REFS_DATA blob not null) ${tablespace}
create unique index ${schemaObjectPrefix}REFS_IDX on ${schemaObjectPrefix}REFS (NODE_ID) ${indexTablespace}

create table ${schemaObjectPrefix}BINVAL (BINVAL_ID varchar2(64) not null, BINVAL_DATA blob null) ${tablespace}
create unique index ${schemaObjectPrefix}BINVAL_IDX on ${schemaObjectPrefix}BINVAL (BINVAL_ID) ${indexTablespace}

create table ${schemaObjectPrefix}NAMES (ID INTEGER primary key using index ${indexTablespace}, NAME varchar2(255) not null) ${tablespace}
create unique index ${schemaObjectPrefix}NAMES_IDX on ${schemaObjectPrefix}NAMES (NAME) ${indexTablespace}

create sequence ${schemaObjectPrefix}seq_names_id
create trigger ${schemaObjectPrefix}t1 before insert on ${schemaObjectPrefix}NAMES for each row begin select ${schemaObjectPrefix}seq_names_id.nextval into :new.id from dual; end;