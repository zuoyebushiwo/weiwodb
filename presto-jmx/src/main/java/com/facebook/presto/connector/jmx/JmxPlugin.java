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
package com.facebook.presto.connector.jmx;

import com.facebook.presto.spi.NodeManager;
import com.facebook.presto.spi.Plugin;
import com.facebook.presto.spi.connector.ConnectorFactory;
import com.google.common.collect.ImmutableList;

import javax.inject.Inject;
import javax.management.MBeanServer;

import java.util.List;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.util.Objects.requireNonNull;

public class JmxPlugin
        implements Plugin
{
    private final MBeanServer mBeanServer;
    private NodeManager nodeManager;

    public JmxPlugin()
    {
        this(getPlatformMBeanServer());
    }

    public JmxPlugin(MBeanServer mBeanServer)
    {
        this.mBeanServer = requireNonNull(mBeanServer, "mBeanServer is null");
    }

    @Inject
    public synchronized void setNodeManager(NodeManager nodeManager)
    {
        this.nodeManager = nodeManager;
    }

    @Override
    public synchronized <T> List<T> getServices(Class<T> type)
    {
        if (type == ConnectorFactory.class) {
            return ImmutableList.of(type.cast(new JmxConnectorFactory(mBeanServer, nodeManager)));
        }
        return ImmutableList.of();
    }
}
