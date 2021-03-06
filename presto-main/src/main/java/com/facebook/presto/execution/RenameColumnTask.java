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
package com.facebook.presto.execution;

import com.facebook.presto.Session;
import com.facebook.presto.metadata.Metadata;
import com.facebook.presto.metadata.QualifiedObjectName;
import com.facebook.presto.metadata.TableHandle;
import com.facebook.presto.security.AccessControl;
import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.sql.analyzer.SemanticException;
import com.facebook.presto.sql.tree.RenameColumn;
import com.facebook.presto.transaction.TransactionManager;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.facebook.presto.metadata.MetadataUtil.createQualifiedObjectName;
import static com.facebook.presto.sql.analyzer.SemanticErrorCode.COLUMN_ALREADY_EXISTS;
import static com.facebook.presto.sql.analyzer.SemanticErrorCode.MISSING_COLUMN;
import static com.facebook.presto.sql.analyzer.SemanticErrorCode.MISSING_TABLE;
import static java.util.Locale.ENGLISH;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class RenameColumnTask
        implements DataDefinitionTask<RenameColumn>
{
    @Override
    public String getName()
    {
        return "RENAME COLUMN";
    }

    @Override
    public CompletableFuture<?> execute(RenameColumn statement, TransactionManager transactionManager, Metadata metadata, AccessControl accessControl, QueryStateMachine stateMachine)
    {
        Session session = stateMachine.getSession();
        QualifiedObjectName tableName = createQualifiedObjectName(session, statement, statement.getTable());
        Optional<TableHandle> tableHandle = metadata.getTableHandle(session, tableName);

        String source = statement.getSource().toLowerCase(ENGLISH);
        String target = statement.getTarget().toLowerCase(ENGLISH);

        if (!tableHandle.isPresent()) {
            throw new SemanticException(MISSING_TABLE, statement, "Table '%s' does not exist", tableName);
        }
        accessControl.checkCanRenameColumn(session.getRequiredTransactionId(), session.getIdentity(), tableName);

        Map<String, ColumnHandle> columnHandles = metadata.getColumnHandles(session, tableHandle.get());
        if (!columnHandles.containsKey(source)) {
            throw new SemanticException(MISSING_COLUMN, statement, "Column '%s' does not exist", source);
        }

        if (columnHandles.containsKey(target)) {
            throw new SemanticException(COLUMN_ALREADY_EXISTS, statement, "Column '%s' already exists", target);
        }
        metadata.renameColumn(session, tableHandle.get(), columnHandles.get(source), target);

        return completedFuture(null);
    }
}
