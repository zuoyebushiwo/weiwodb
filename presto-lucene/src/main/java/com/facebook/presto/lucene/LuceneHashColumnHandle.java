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

import com.facebook.presto.lucene.hive.metastore.HiveType;
import com.facebook.presto.spi.type.TypeSignature;

public class LuceneHashColumnHandle extends LuceneColumnHandle {

	public LuceneHashColumnHandle(String connectorId, String columnName,
			HiveType columnType, int hiveColumnIndex,
			TypeSignature typeSignature, String luceneType) {
		super(connectorId, columnName, columnType, hiveColumnIndex,
				typeSignature, luceneType);
	}

	@Override
	public Category getCategory() {
		return Category.HASH;
	}
}
