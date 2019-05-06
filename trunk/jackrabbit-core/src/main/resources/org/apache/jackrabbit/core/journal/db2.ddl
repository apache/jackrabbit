# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
create table ${schemaObjectPrefix}JOURNAL (REVISION_ID BIGINT NOT NULL, JOURNAL_ID varchar(255), PRODUCER_ID varchar(255), REVISION_DATA blob)
create unique index ${schemaObjectPrefix}JOURNAL_IDX on ${schemaObjectPrefix}JOURNAL (REVISION_ID)
create table ${schemaObjectPrefix}GLOBAL_REVISION (REVISION_ID BIGINT NOT NULL)
create unique index ${schemaObjectPrefix}GLOBAL_REVISION_IDX on ${schemaObjectPrefix}GLOBAL_REVISION (REVISION_ID)
create table ${schemaObjectPrefix}LOCAL_REVISIONS (JOURNAL_ID varchar(255) NOT NULL, REVISION_ID BIGINT NOT NULL)

# Inserting the one and only revision counter record now helps avoiding race conditions
insert into ${schemaObjectPrefix}GLOBAL_REVISION VALUES(0)
