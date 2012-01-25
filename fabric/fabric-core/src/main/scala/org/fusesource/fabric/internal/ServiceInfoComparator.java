/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.internal;

import java.util.Comparator;

import org.fusesource.fabric.api.data.ServiceInfo;

public class ServiceInfoComparator implements Comparator<ServiceInfo> {

    public int compare(ServiceInfo serviceInfo1, ServiceInfo serviceInfo2) {
        return serviceInfo1.getId().compareTo(serviceInfo2.getId());
    }
}
