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
package com.facebook.presto.lucene;

import static java.util.Objects.requireNonNull;

import java.util.List;

import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.ConnectorPageSource;
import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.ConnectorSplit;
import com.facebook.presto.spi.connector.ConnectorPageSourceProvider;
import com.facebook.presto.spi.connector.ConnectorRecordSetProvider;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;
/**
 * 
 * 
 * @author peter.wei
 *
 */
public class LucenePageSourceProvider implements ConnectorPageSourceProvider {

	private ConnectorRecordSetProvider recordSetProvider;

	public LucenePageSourceProvider(
			ConnectorRecordSetProvider recordSetProvider) {
		this.recordSetProvider = requireNonNull(recordSetProvider,
				"recordSetProvider is null");
	}

	@Override
	public ConnectorPageSource createPageSource(
			ConnectorTransactionHandle transactionHandle,
			ConnectorSession session, ConnectorSplit split,
			List<ColumnHandle> columns) {
		return new LucenePageSource(recordSetProvider
				.getRecordSet(transactionHandle, session, split, columns));
	}
}