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
create table ${schemaObjectPrefix}BUNDLE (NODE_ID binary(16) not null, BUNDLE_DATA image not null) ${tableSpace}
create unique index ${schemaObjectPrefix}BUNDLE_IDX on ${schemaObjectPrefix}BUNDLE (NODE_ID) ${tableSpace}
create table ${schemaObjectPrefix}REFS (NODE_ID binary(16) not null, REFS_DATA image not null) ${tableSpace}
create unique index ${schemaObjectPrefix}REFS_IDX on ${schemaObjectPrefix}REFS (NODE_ID) ${tableSpace}
create table ${schemaObjectPrefix}BINVAL (BINVAL_ID varchar(64) not null, BINVAL_DATA image not null) ${tableSpace}
create unique index ${schemaObjectPrefix}BINVAL_IDX on ${schemaObjectPrefix}BINVAL (BINVAL_ID) ${tableSpace}
create table ${schemaObjectPrefix}NAMES (ID INTEGER IDENTITY(1,1) PRIMARY KEY, NAME varchar(255) COLLATE Latin1_General_CS_AS not null) ${tableSpace}
