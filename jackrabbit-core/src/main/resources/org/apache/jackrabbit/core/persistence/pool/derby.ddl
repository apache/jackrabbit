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
create table ${schemaObjectPrefix}BUNDLE (NODE_ID_HI bigint not null, NODE_ID_LO bigint not null, BUNDLE_DATA blob(2G) not null, PRIMARY KEY (NODE_ID_HI, NODE_ID_LO))
create table ${schemaObjectPrefix}REFS (NODE_ID_HI bigint not null, NODE_ID_LO bigint not null, REFS_DATA blob(2G) not null, PRIMARY KEY (NODE_ID_HI, NODE_ID_LO))
create table ${schemaObjectPrefix}BINVAL (BINVAL_ID char(64) PRIMARY KEY, BINVAL_DATA blob(2G) not null)
create table ${schemaObjectPrefix}NAMES (ID INTEGER GENERATED ALWAYS AS IDENTITY, NAME varchar(255) not null, PRIMARY KEY (ID, NAME))
