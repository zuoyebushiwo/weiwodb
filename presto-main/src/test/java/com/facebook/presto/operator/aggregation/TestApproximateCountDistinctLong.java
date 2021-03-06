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
package com.facebook.presto.operator.aggregation;

import com.facebook.presto.metadata.Signature;
import com.facebook.presto.spi.type.Type;

import java.util.concurrent.ThreadLocalRandom;

import static com.facebook.presto.metadata.FunctionKind.AGGREGATE;
import static com.facebook.presto.spi.type.BigintType.BIGINT;
import static com.facebook.presto.spi.type.DoubleType.DOUBLE;

public class TestApproximateCountDistinctLong
        extends AbstractTestApproximateCountDistinct
{
    @Override
    public InternalAggregationFunction getAggregationFunction()
    {
        return metadata.getFunctionRegistry().getAggregateFunctionImplementation(
                new Signature("approx_distinct", AGGREGATE, BIGINT.getTypeSignature(), BIGINT.getTypeSignature(), DOUBLE.getTypeSignature()));
    }

    @Override
    public Type getValueType()
    {
        return BIGINT;
    }

    @Override
    public Object randomValue()
    {
        return ThreadLocalRandom.current().nextLong();
    }
}
