/*
 * Copyright 2020-present Open Networking Foundation
 *
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
package nctu.winlab.vlanbasedsr;



import com.google.common.collect.ImmutableSet;
import org.onosproject.cfg.ComponentConfigService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;

import static org.onlab.util.Tools.get;


//////////////////////////////////////////////////////////////////////////////////////

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import java.util.Set;
import java.util.Map;
import org.onlab.packet.IpPrefix;
import org.onosproject.net.DeviceId;

import nctu.winlab.vlanbasedsr.MyConfig;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketPriority;

///////////////////////////////////////////////////////////////////////////////////
import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;


import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;

import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketPriority;

import org.onosproject.net.Path;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostService;
import org.onosproject.net.Link;
import org.onosproject.net.DeviceId;

import org.onosproject.net.host.HostService;

import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.net.topology.TopologyEdge;

import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRuleService;

import org.onosproject.net.group.GroupService;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupKey;


import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.lang.Integer;


/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ApplicationId appId;
    private final InternalConfigListener cfgListener = new InternalConfigListener();
    private ReactivePacketProcessor processor = new ReactivePacketProcessor();
    private static final int DEFAULT_TIMEOUT = 350;
    private static final int HIGH_PRIORITY = 4100;
    private static final int LOW_PRIORITY = 4000;
    Map<IpPrefix, DeviceId> edgeSubnetIP;
    Map<DeviceId, Short> segmentDeviceId;
    /** Some configurable property. */
    private String someProperty;
    
    /** Config factory */
    private final Set<ConfigFactory> factories = ImmutableSet.of(
        new ConfigFactory<ApplicationId, MyConfig>(APP_SUBJECT_FACTORY, MyConfig.class, "myconfig"){
            
            @Override
            public MyConfig createConfig(){
                return new MyConfig();
            }
        }
    );
    // onos service
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected GroupService groupService;
    
    //@Reference(cardinality = ReferenceCardinality.MANDATORY)
    //protected ComponentConfigService cfgService;

    

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("nctu.winlab.vlanbasedsr");

        cfgService.addListener(cfgListener);
        factories.forEach(cfgService::registerConfigFactory);
        cfgListener.reconfigureNetwork(cfgService.getConfig(appId, MyConfig.class));

        packetService.addProcessor(processor, PacketProcessor.director(2));
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);


        //cfgService.registerProperties(getClass());
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        //cfgService.unregisterProperties(getClass(), false);
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
        packetService.removeProcessor(processor);
        processor = null;

        cfgService.removeListener(cfgListener);
        factories.forEach(cfgService::unregisterConfigFactory);
        log.info("Stopped");
    }

    private class InternalConfigListener implements NetworkConfigListener {
        
        // update the variable
        private void reconfigureNetwork(MyConfig cfg) {
            if (cfg == null) {
                return;
            }
            segmentDeviceId = cfg.getNodeID();
            edgeSubnetIP = cfg.getSubnetIP();
	        configFlow();
        }

        @Override
        public void event(NetworkConfigEvent event) {
            if ((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                 event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED) &&
                 event.configClass().equals(MyConfig.class)) {
                    MyConfig cfg = cfgService.getConfig(appId, MyConfig.class);
                    reconfigureNetwork(cfg);
                    
                    log.info("[Reconfigured]");
                    log.info("getNodeID {}", cfg.getNodeID());
                    log.info("getSubnetIP {}", cfg.getSubnetIP());

            }
        }
    }

    private class ReactivePacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            //not ethernet packet
            if (ethPkt == null) {
                return;
            }
            log.info("Packet IN");
            handler(context);
        }
    }


    private void handler(PacketContext context){
        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();

        MacAddress srcmac = ethPkt.getSourceMAC();
        MacAddress dstmac = ethPkt.getDestinationMAC();

        //get source device
        Set<Host> srcHosts = hostService.getHostsByMac(srcmac);
	    Set<Host> dstHosts = hostService.getHostsByMac(dstmac);
        if(srcHosts.size()==0 || dstHosts.size()==0){
            log.info("Host Service no find.");
            return;
        }
        Host srcHost = srcHosts.iterator().next();
        Host dstHost = dstHosts.iterator().next();
        if(srcHost.location().deviceId().equals(dstHost.location().deviceId())){
            installRule(srcmac, dstmac, dstHost.location().port(), dstHost.location().deviceId());
            log.info("DD {} {} {}",srcmac,dstmac,srcHost.location().deviceId());
            return;
        }
        
        VlanId vId = VlanId.vlanId(segmentDeviceId.get(dstHost.location().deviceId()).shortValue());
        installDstEdgeRule(vId, dstmac, dstHost.location().port(), dstHost.location().deviceId());
    }

    private void configFlow(){
        for(IpPrefix to_IP : edgeSubnetIP.keySet()){
            
            DeviceId subnetDevice = edgeSubnetIP.get(to_IP);
            VlanId vId = VlanId.vlanId(segmentDeviceId.get(subnetDevice).shortValue());
            Map<DeviceId, List> myGraph = getGraph();
            Map<DeviceId, Link> parentLink = getAllPath(myGraph, subnetDevice);
            ///install flow rule on source leaf switch
            for(IpPrefix from_IP : edgeSubnetIP.keySet()){
                if(from_IP.equals(to_IP)){
                    installIntraRule(to_IP, PortNumber.FLOOD, edgeSubnetIP.get(to_IP));
                    continue;
                }

                Link firstlink = parentLink.get(edgeSubnetIP.get(from_IP));
                log.info("[SEG] {}, {}, {}, {}",to_IP, VlanId.vlanId(segmentDeviceId.get(edgeSubnetIP.get(to_IP)).shortValue()), firstlink.src().port(), edgeSubnetIP.get(from_IP));
                installSrcEdgeRule(to_IP, 
                      VlanId.vlanId(segmentDeviceId.get(edgeSubnetIP.get(to_IP)).shortValue()),
                      firstlink.src().port(),
                      edgeSubnetIP.get(from_IP) );
            }
            ///install flow rule on spine switch
            for(DeviceId dID : segmentDeviceId.keySet()){
                VlanId segmentvId = VlanId.vlanId(segmentDeviceId.get(dID).shortValue());
                if(segmentvId.equals(vId)){
                    continue;
                }
                Link firstlink = parentLink.get(dID);
                //log.info("[SS] {}", dID);
                //log.info("[LINKd] {}, {}, {}",vId, firstlink.src().port(), firstlink.src().deviceId());
                //log.info("[LINKd] {}, {}, {}",vId, firstlink.dst().port(), firstlink.dst().deviceId());
                installRule(vId, firstlink.src().port(), firstlink.src().deviceId());
            }
            ///install flow rule on destination leaf switch
            log.info("[Reconfigured] {}, {}, {}",vId, PortNumber.CONTROLLER, subnetDevice);
            installDstEdgeRule(vId, PortNumber.FLOOD, subnetDevice);
        }
    }

    private Map<DeviceId, List> getGraph(){
        Map<DeviceId, List> myGraph = new HashMap<DeviceId, List>();
        TopologyGraph currentGraph = topologyService.getGraph(topologyService.currentTopology());
        Set<TopologyVertex> vertexes = new HashSet<TopologyVertex>(currentGraph.getVertexes());
        for(TopologyVertex i : vertexes){
            //log.info("Device:  "+i.deviceId().toString());
            List vtxlist = new LinkedList<Link>();
            Set<TopologyEdge> to_edges = currentGraph.getEdgesFrom(i);
            for(TopologyEdge j : to_edges){
                vtxlist.add(j.link());
            }
            myGraph.put(i.deviceId(), vtxlist);
        }
        return myGraph;
    }
    // Dijkstra Algorithm
    private Map<DeviceId, Link> getAllPath(Map<DeviceId, List> myGraph, DeviceId sourceDeviceId){
        Map<DeviceId, Integer> distance = new HashMap<DeviceId, Integer>();  //dist
        Map<DeviceId, Link> parentLink = new HashMap<DeviceId, Link>();      //parent
        Set<DeviceId> vertexes = myGraph.keySet();
        DeviceId min_vertex;
        Integer min_dis;
        for (DeviceId i : vertexes) {

            distance.put(i, 10000);
            parentLink.put(i, null);
        }
        distance.put(sourceDeviceId, 0);
        List<Link> src_device_edge = myGraph.get(sourceDeviceId);
        for (Link connect_edge : src_device_edge) {

            Integer src_dis = distance.get(connect_edge.src().deviceId());
            distance.put(connect_edge.dst().deviceId(), new Integer(src_dis.intValue() + 1));
            parentLink.put(connect_edge.dst().deviceId(), connect_edge);
        }
        vertexes.remove(sourceDeviceId);
        while (!vertexes.isEmpty()) {

            min_vertex = null;
            min_dis = 10000;
            for (DeviceId i : vertexes) {

                if (distance.get(i) < min_dis) {

                    min_vertex = i;
                    min_dis = distance.get(i);
                }
            }
            vertexes.remove(min_vertex);
            for (DeviceId cur_vertex : vertexes) {

                List<Link> cur_vertex_edge = myGraph.get(cur_vertex);
                for (Link cur_edge : cur_vertex_edge) {

                    Integer compare_vertex_dis = distance.get(cur_edge.dst().deviceId());
                    Integer cur_vertex_dis = distance.get(cur_vertex);
                    if (compare_vertex_dis.intValue() + 1 < cur_vertex_dis.intValue()) {

                        distance.put(cur_vertex, compare_vertex_dis.intValue() + 1);
                        parentLink.put(cur_vertex, cur_edge);
                    }
                }
            }
        }
        return parentLink;
    }

    // Sends flow modify to device, for reactive forwarding
    private void installRule(MacAddress srcMac, MacAddress dstMac,
                             PortNumber outPort, DeviceId configDeviceId){
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder.matchEthSrc(srcMac)
                    .matchEthDst(dstMac);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(outPort)
                    .build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatment)
                    .withPriority(LOW_PRIORITY)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makeTemporary(DEFAULT_TIMEOUT)
                    .add();

        flowObjectiveService.forward(configDeviceId,  forwardingObjective);
    }

    // Sends flow modify to device 
    private void installRule(VlanId vID, PortNumber outPort, DeviceId configDeviceId){
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchVlanId(vID);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(outPort)
                    .build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatment)
                    .withPriority(HIGH_PRIORITY)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makeTemporary(DEFAULT_TIMEOUT)
                    .add();

        flowObjectiveService.forward(configDeviceId, forwardingObjective);
    }

    // Sends flow modify to device 
    private void installIntraRule(IpPrefix dIP, PortNumber outPort, DeviceId configDeviceId){
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(dIP);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(outPort)
                    .build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatment)
                    .withPriority(HIGH_PRIORITY)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makeTemporary(DEFAULT_TIMEOUT)
                    .add();

        flowObjectiveService.forward(configDeviceId, forwardingObjective);
    }

    // Sends flow modify to device 
    private void installSrcEdgeRule(IpPrefix dIP, VlanId vID,
                                    PortNumber outPort, DeviceId configDeviceId){
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(dIP);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .pushVlan()
                    .setVlanId(vID)
                    .setOutput(outPort)
                    .build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatment)
                    .withPriority(HIGH_PRIORITY)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makeTemporary(DEFAULT_TIMEOUT)
                    .add();

        flowObjectiveService.forward(configDeviceId, forwardingObjective);
    }

    // Sends flow modify to device 
    private void installDstEdgeRule(VlanId vID,
                                 PortNumber outPort, DeviceId configDeviceId){
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchVlanId(vID);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .popVlan()
                    .setOutput(outPort)
                    .build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatment)
                    .withPriority(HIGH_PRIORITY)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makeTemporary(DEFAULT_TIMEOUT)
                    .add();

        flowObjectiveService.forward(configDeviceId, forwardingObjective);
    }

    // Sends flow modify to device, for reactive forwarding
    private void installDstEdgeRule(VlanId vID, MacAddress dstMac,
                                 PortNumber outPort, DeviceId configDeviceId){
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchVlanId(vID).matchEthDst(dstMac);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .popVlan()
                    .setOutput(outPort)
                    .build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatment)
                    .withPriority(LOW_PRIORITY)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makeTemporary(DEFAULT_TIMEOUT)
                    .add();

        flowObjectiveService.forward(configDeviceId, forwardingObjective);
    }


}
