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
package com.facebook.presto.lucene.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostAddressUtil {

	public static String hostAddress;

	public static String getHostAddress() throws UnknownHostException {
		if (hostAddress == null) {
			hostAddress = InetAddress.getLocalHost().getHostAddress();
		}
		return hostAddress;
	}

}
