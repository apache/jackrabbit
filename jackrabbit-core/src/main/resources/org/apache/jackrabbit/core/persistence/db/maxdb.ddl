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
create table ${schemaObjectPrefix}NODE (NODE_ID VARCHAR(36) not null, NODE_DATA LONG BYTE not null)
create unique index ${schemaObjectPrefix}NODE_IDX on ${schemaObjectPrefix}NODE (NODE_ID)
create table ${schemaObjectPrefix}PROP (PROP_ID VARCHAR(1000) not null, PROP_DATA LONG BYTE not null)
create unique index ${schemaObjectPrefix}PROP_IDX on ${schemaObjectPrefix}PROP (PROP_ID)
create table ${schemaObjectPrefix}REFS (NODE_ID VARCHAR(36) not null, REFS_DATA LONG BYTE not null)
create unique index ${schemaObjectPrefix}REFS_IDX on ${schemaObjectPrefix}REFS (NODE_ID)
create table ${schemaObjectPrefix}BINVAL (BINVAL_ID VARCHAR(1000) not null, BINVAL_DATA LONG BYTE not null)
create unique index ${schemaObjectPrefix}BINVAL_IDX on ${schemaObjectPrefix}BINVAL (BINVAL_ID)
