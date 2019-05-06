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
create table ${schemaObjectPrefix}FSENTRY (FSENTRY_PATH text not null, FSENTRY_NAME varchar(255) not null, FSENTRY_DATA longblob null, FSENTRY_LASTMOD bigint not null, FSENTRY_LENGTH bigint not null) character set latin1
create unique index ${schemaObjectPrefix}FSENTRY_IDX on ${schemaObjectPrefix}FSENTRY (FSENTRY_PATH(245), FSENTRY_NAME)
