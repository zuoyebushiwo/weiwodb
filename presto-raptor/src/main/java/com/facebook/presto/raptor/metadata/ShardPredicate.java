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
package com.facebook.presto.raptor.metadata;

import com.facebook.presto.raptor.RaptorColumnHandle;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.predicate.Domain;
import com.facebook.presto.spi.predicate.Range;
import com.facebook.presto.spi.predicate.Ranges;
import com.facebook.presto.spi.predicate.TupleDomain;
import com.facebook.presto.spi.type.Type;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.airlift.slice.Slice;

import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringJoiner;

import static com.facebook.presto.raptor.metadata.DatabaseShardManager.maxColumn;
import static com.facebook.presto.raptor.metadata.DatabaseShardManager.minColumn;
import static com.facebook.presto.raptor.storage.ColumnIndexStatsUtils.jdbcType;
import static com.facebook.presto.raptor.storage.ShardStats.truncateIndexValue;
import static com.facebook.presto.raptor.util.Types.checkType;
import static com.facebook.presto.raptor.util.UuidUtil.uuidStringToBytes;
import static com.facebook.presto.spi.StandardErrorCode.GENERIC_INTERNAL_ERROR;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

class ShardPredicate
{
    private final String predicate;
    private final List<JDBCType> types;
    private final List<Object> values;

    private ShardPredicate(String predicate, List<JDBCType> types, List<Object> values)
    {
        this.predicate = requireNonNull(predicate, "predicate is null");
        this.types = ImmutableList.copyOf(requireNonNull(types, "types is null"));
        this.values = ImmutableList.copyOf(requireNonNull(values, "values is null"));
        checkArgument(types.size() == values.size(), "types and values sizes do not match");
    }

    public String getPredicate()
    {
        return predicate;
    }

    public void bind(PreparedStatement statement)
            throws SQLException
    {
        for (int i = 0; i < types.size(); i++) {
            JDBCType type = types.get(i);
            Object value = values.get(i);
            bindValue(statement, type, value, i + 1);
        }
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .addValue(predicate)
                .toString();
    }

    public static ShardPredicate create(TupleDomain<RaptorColumnHandle> tupleDomain, boolean bucketed)
    {
        StringJoiner predicate = new StringJoiner(" AND ").setEmptyValue("true");
        ImmutableList.Builder<JDBCType> types = ImmutableList.builder();
        ImmutableList.Builder<Object> values = ImmutableList.builder();

        for (Entry<RaptorColumnHandle, Domain> entry : tupleDomain.getDomains().get().entrySet()) {
            Domain domain = entry.getValue();
            if (domain.isNullAllowed() || domain.isAll()) {
                continue;
            }
            RaptorColumnHandle handle = entry.getKey();
            Type type = handle.getColumnType();

            JDBCType jdbcType = jdbcType(type);
            if (jdbcType == null) {
                continue;
            }

            if (handle.isShardUuid()) {
                predicate.add(createShardPredicate(types, values, domain, jdbcType));
                continue;
            }

            if (!domain.getType().isOrderable()) {
                continue;
            }

            Ranges ranges = domain.getValues().getRanges();

            // TODO: support multiple ranges
            if (ranges.getRangeCount() != 1) {
                continue;
            }
            Range range = getOnlyElement(ranges.getOrderedRanges());

            Object minValue = null;
            Object maxValue = null;
            if (range.isSingleValue()) {
                minValue = range.getSingleValue();
                maxValue = range.getSingleValue();
            }
            else {
                if (!range.getLow().isLowerUnbounded()) {
                    minValue = range.getLow().getValue();
                }
                if (!range.getHigh().isUpperUnbounded()) {
                    maxValue = range.getHigh().getValue();
                }
            }

            String min;
            String max;
            if (handle.isBucketNumber()) {
                if (!bucketed) {
                    predicate.add("false");
                    continue;
                }
                min = "bucket_number";
                max = "bucket_number";
            }
            else {
                min = minColumn(handle.getColumnId());
                max = maxColumn(handle.getColumnId());
            }

            if (minValue != null) {
                predicate.add(format("(%s >= ? OR %s IS NULL)", max, max));
                types.add(jdbcType);
                values.add(minValue);
            }
            if (maxValue != null) {
                predicate.add(format("(%s <= ? OR %s IS NULL)", min, min));
                types.add(jdbcType);
                values.add(maxValue);
            }
        }
        return new ShardPredicate(predicate.toString(), types.build(), values.build());
    }

    private static String createShardPredicate(ImmutableList.Builder<JDBCType> types, ImmutableList.Builder<Object> values, Domain domain, JDBCType jdbcType)
    {
        List<Range> ranges = domain.getValues().getRanges().getOrderedRanges();

        // only apply predicates if all ranges are single values
        if (ranges.isEmpty() || !ranges.stream().allMatch(Range::isSingleValue)) {
            return "true";
        }

        ImmutableList.Builder<Object> valuesBuilder = ImmutableList.builder();
        ImmutableList.Builder<JDBCType> typesBuilder = ImmutableList.builder();

        StringJoiner rangePredicate = new StringJoiner(" OR ");
        for (Range range : ranges) {
            Slice uuidText = checkType(range.getSingleValue(), Slice.class, "uuid");
            try {
                Slice uuidBytes = uuidStringToBytes(uuidText);
                typesBuilder.add(jdbcType);
                valuesBuilder.add(uuidBytes);
            }
            catch (IllegalArgumentException e) {
                return "true";
            }
            rangePredicate.add("shard_uuid = ?");
        }

        types.addAll(typesBuilder.build());
        values.addAll(valuesBuilder.build());
        return rangePredicate.toString();
    }

    @VisibleForTesting
    protected List<JDBCType> getTypes()
    {
        return types;
    }

    @VisibleForTesting
    protected List<Object> getValues()
    {
        return values;
    }

    public static void bindValue(PreparedStatement statement, JDBCType type, Object value, int index)
            throws SQLException
    {
        if (value == null) {
            statement.setNull(index, type.getVendorTypeNumber());
            return;
        }

        switch (type) {
            case BOOLEAN:
                statement.setBoolean(index, (boolean) value);
                return;
            case INTEGER:
                statement.setInt(index, ((Number) value).intValue());
                return;
            case BIGINT:
                statement.setLong(index, ((Number) value).longValue());
                return;
            case DOUBLE:
                statement.setDouble(index, ((Number) value).doubleValue());
                return;
            case VARBINARY:
                statement.setBytes(index, truncateIndexValue((Slice) value).getBytes());
                return;
        }
        throw new PrestoException(GENERIC_INTERNAL_ERROR, "Unhandled type: " + type);
    }
}
