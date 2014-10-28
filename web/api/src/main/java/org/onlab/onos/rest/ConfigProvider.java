/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultAnnotations;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.SparseAnnotations;
import org.onlab.onos.net.device.DefaultDeviceDescription;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.DeviceProvider;
import org.onlab.onos.net.device.DeviceProviderRegistry;
import org.onlab.onos.net.device.DeviceProviderService;
import org.onlab.onos.net.host.DefaultHostDescription;
import org.onlab.onos.net.host.HostProvider;
import org.onlab.onos.net.host.HostProviderRegistry;
import org.onlab.onos.net.host.HostProviderService;
import org.onlab.onos.net.link.DefaultLinkDescription;
import org.onlab.onos.net.link.LinkProvider;
import org.onlab.onos.net.link.LinkProviderRegistry;
import org.onlab.onos.net.link.LinkProviderService;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import java.net.URI;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.PortNumber.portNumber;

/**
 * Provider of devices and links parsed from a JSON configuration structure.
 */
class ConfigProvider implements DeviceProvider, LinkProvider, HostProvider {

    private static final ProviderId PID =
            new ProviderId("cfg", "org.onlab.onos.rest", true);

    private final JsonNode cfg;
    private final DeviceProviderRegistry deviceProviderRegistry;
    private final LinkProviderRegistry linkProviderRegistry;
    private final HostProviderRegistry hostProviderRegistry;

    /**
     * Creates a new configuration provider.
     *
     * @param cfg                    JSON configuration
     * @param deviceProviderRegistry device provider registry
     * @param linkProviderRegistry   link provider registry
     * @param hostProviderRegistry   host provider registry
     */
    ConfigProvider(JsonNode cfg,
                   DeviceProviderRegistry deviceProviderRegistry,
                   LinkProviderRegistry linkProviderRegistry,
                   HostProviderRegistry hostProviderRegistry) {
        this.cfg = checkNotNull(cfg, "Configuration cannot be null");
        this.deviceProviderRegistry = checkNotNull(deviceProviderRegistry, "Device provider registry cannot be null");
        this.linkProviderRegistry = checkNotNull(linkProviderRegistry, "Link provider registry cannot be null");
        this.hostProviderRegistry = checkNotNull(hostProviderRegistry, "Host provider registry cannot be null");
    }

    /**
     * Parses the given JSON and provides links as configured.
     */
    void parse() {
        parseDevices();
        parseLinks();
        parseHosts();
    }

    // Parses the given JSON and provides devices.
    private void parseDevices() {
        try {
            DeviceProviderService dps = deviceProviderRegistry.register(this);
            JsonNode nodes = cfg.get("devices");
            if (nodes != null) {
                for (JsonNode node : nodes) {
                    parseDevice(dps, node);
                }
            }
        } finally {
            deviceProviderRegistry.unregister(this);
        }
    }

    // Parses the given node with device data and supplies the device.
    private void parseDevice(DeviceProviderService dps, JsonNode node) {
        URI uri = URI.create(get(node, "uri"));
        Device.Type type = Device.Type.valueOf(get(node, "type"));
        String mfr = get(node, "mfr");
        String hw = get(node, "hw");
        String sw = get(node, "sw");
        String serial = get(node, "serial");
        ChassisId cid = new ChassisId(get(node, "mac"));
        SparseAnnotations annotations = annotations(node.get("annotations"));

        DeviceDescription desc =
                new DefaultDeviceDescription(uri, type, mfr, hw, sw, serial,
                                             cid, annotations);
        dps.deviceConnected(deviceId(uri), desc);
    }

    // Parses the given JSON and provides links as configured.
    private void parseLinks() {
        try {
            LinkProviderService lps = linkProviderRegistry.register(this);
            JsonNode nodes = cfg.get("links");
            if (nodes != null) {
                for (JsonNode node : nodes) {
                    parseLink(lps, node, false);
                    if (!node.has("halfplex")) {
                        parseLink(lps, node, true);
                    }
                }
            }
        } finally {
            linkProviderRegistry.unregister(this);
        }
    }

    // Parses the given node with link data and supplies the link.
    private void parseLink(LinkProviderService lps, JsonNode node, boolean reverse) {
        ConnectPoint src = connectPoint(get(node, "src"));
        ConnectPoint dst = connectPoint(get(node, "dst"));
        Link.Type type = Link.Type.valueOf(get(node, "type"));
        SparseAnnotations annotations = annotations(node.get("annotations"));

        DefaultLinkDescription desc = reverse ?
                new DefaultLinkDescription(dst, src, type, annotations) :
                new DefaultLinkDescription(src, dst, type, annotations);
        lps.linkDetected(desc);
    }

    // Parses the given JSON and provides hosts as configured.
    private void parseHosts() {
        try {
            HostProviderService hps = hostProviderRegistry.register(this);
            JsonNode nodes = cfg.get("hosts");
            if (nodes != null) {
                for (JsonNode node : nodes) {
                    parseHost(hps, node);
                }
            }
        } finally {
            hostProviderRegistry.unregister(this);
        }
    }

    // Parses the given node with host data and supplies the host.
    private void parseHost(HostProviderService hps, JsonNode node) {
        MacAddress mac = MacAddress.valueOf(get(node, "mac"));
        VlanId vlanId = VlanId.vlanId(node.get("vlan").shortValue());
        HostId hostId = HostId.hostId(mac, vlanId);
        SparseAnnotations annotations = annotations(node.get("annotations"));
        HostLocation location = new HostLocation(connectPoint(get(node, "location")), 0);
        IpAddress ip = IpAddress.valueOf(get(node, "ip"));

        DefaultHostDescription desc =
                new DefaultHostDescription(mac, vlanId, location, ip, annotations);
        hps.hostDetected(hostId, desc);
    }

    // Produces set of annotations from the given JSON node.
    private SparseAnnotations annotations(JsonNode node) {
        if (node == null) {
            return null;
        }

        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            String k = it.next();
            builder.set(k, node.get(k).asText());
        }
        return builder.build();
    }

    // Produces a connection point from the specified uri/port text.
    private ConnectPoint connectPoint(String text) {
        int i = text.lastIndexOf("/");
        return new ConnectPoint(deviceId(text.substring(0, i)),
                                portNumber(text.substring(i + 1)));
    }

    // Returns string form of the named property in the given JSON object.
    private String get(JsonNode node, String name) {
        return node.path(name).asText();
    }

    @Override
    public void triggerProbe(Device device) {
    }

    @Override
    public void roleChanged(Device device, MastershipRole newRole) {
    }

    @Override
    public void triggerProbe(Host host) {
    }

    @Override
    public ProviderId id() {
        return PID;
    }
}
