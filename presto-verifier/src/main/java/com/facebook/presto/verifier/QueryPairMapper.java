/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.verifier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.airlift.json.JsonCodec;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class QueryPairMapper
        implements ResultSetMapper<QueryPair>
{
    private static final JsonCodec<Map<String, String>> propertiesJsonCodec = JsonCodec.mapJsonCodec(String.class, String.class);
    private static final JsonCodec<List<String>> queriesJsonCodec = JsonCodec.listJsonCodec(String.class);

    @Override
    public QueryPair map(int index, ResultSet resultSet, StatementContext statementContext)
            throws SQLException
    {
        Map<String, String> sessionProperties = ImmutableMap.of();
        String json = resultSet.getString("session_properties_json");
        if (json != null) {
            sessionProperties = propertiesJsonCodec.fromJson(json);
        }

        return new QueryPair(
                resultSet.getString("suite"),
                resultSet.getString("name"),
                new Query(resultSet.getString("test_catalog"), resultSet.getString("test_schema"), fromJsonString(resultSet.getString("test_prequeries")), resultSet.getString("test_query"),
                        fromJsonString(resultSet.getString("test_postqueries")),
                        resultSet.getString("test_username"), resultSet.getString("test_password"), sessionProperties),
                new Query(resultSet.getString("control_catalog"), resultSet.getString("control_schema"), fromJsonString(resultSet.getString("control_prequeries")), resultSet.getString("control_query"),
                        fromJsonString(resultSet.getString("control_postqueries")),
                        resultSet.getString("control_username"), resultSet.getString("control_password"), sessionProperties));
    }

    private static List<String> fromJsonString(String jsonString)
            throws SQLException
    {
        return jsonString == null ? ImmutableList.of() : queriesJsonCodec.fromJson(jsonString);
    }
}
