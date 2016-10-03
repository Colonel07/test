package com.bwssystems.HABridge.upnp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bwssystems.HABridge.BridgeControlDescriptor;
import com.bwssystems.HABridge.BridgeSettingsDescriptor;
import com.bwssystems.HABridge.Configuration;
import com.bwssystems.HABridge.api.hue.HuePublicConfig;

import java.io.IOException;
import java.net.*;

import java.util.Enumeration;
import org.apache.http.conn.util.*;


public class UpnpListener {
	private Logger log = LoggerFactory.getLogger(UpnpListener.class);
	private int upnpResponsePort;
	private int httpServerPort;
	private String responseAddress;
	private boolean strict;
	private boolean traceupnp;
	private BridgeControlDescriptor bridgeControl;
	private boolean discoveryTemplateLatest;
	private String discoveryTemplate = "HTTP/1.1 200 OK\r\n" +
			"CACHE-CONTROL: max-age=86400\r\n" +
			"EXT:\r\n" +
			"LOCATION: http://%s:%s/description.xml\r\n" +
			"SERVER: Linux/3.14.0 UPnP/1.0 IpBridge/1.14.0\r\n" + 
			"ST: urn:schemas-upnp-org:device:basic:1\r\n" +
			"USN: uuid:2f402f80-da50-11e1-9b23-%s\r\n\r\n";
/*
	private String discoveryTemplate = "NOTIFY * HTTP/1.1\r\n" +
			"HOST: %s:%s\r\n" +
			"CACHE-CONTROL: max-age=100\r\n" +
			"LOCATION: http://%s:%s/description.xml\r\n" +
			"SERVER: Linux/3.14.0 UPnP/1.0 IpBridge/1.14.0\r\n" +
			"NTS: ssdp:alive\r\n" +
			"hue-bridgeid: %s\r\n" +
			"NT: uuid:2f402f80-da50-11e1-9b23-%s\r\n" +
			"USN: uuid:2f402f80-da50-11e1-9b23-%s\r\n\r\n";
			discoveryResponse = String.format(discoveryTemplate, Configuration.UPNP_MULTICAST_ADDRESS, Configuration.UPNP_DISCOVERY_PORT, responseAddress, httpServerPort, bridgeIdMac, bridgeIdMac, bridgeIdMac);
*/
/*
	private String discoveryTemplate = "HTTP/1.1 200 OK\r\n" +
			"CACHE-CONTROL: max-age=86400\r\n" +
			"EXT:\r\n" +
			"LOCATION: http://%s:%s/description.xml\r\n" +
			"SERVER: FreeRTOS/7.4.2 UPnP/1.0 IpBridge/1.10.0\r\n" + 
			"ST: urn:schemas-upnp-org:device:basic:1\r\n" +
			"USN: uuid:2f402f80-da50-11e1-9b23-001788102201::urn:schemas-upnp-org:device:basic:1\r\n\r\n";
			discoveryResponse = String.format(discoveryTemplate, responseAddress, httpServerPort);
*/
/*
	private String discoveryTemplate = "HTTP/1.1 200 OK\r\n" +
			"CACHE-CONTROL: max-age=86400\r\n" +
			"EXT:\r\n" +
			"LOCATION: http://%s:%s/description.xml\r\n" +
			"SERVER: FreeRTOS/7.4.2 UPnP/1.0 IpBridge/1.10.0\r\n" + 
			"ST: upnp:rootdevice\r\n" +
			"USN: uuid:2f402f80-da50-11e1-9b23-001788102201\r\n\r\n";
*/
/*
	private String discoveryTemplate = "HTTP/1.1 200 OK\r\n" +
			"HOST: %s:%s\r\n" +
			"CACHE-CONTROL: max-age=86400\r\n" +
			"EXT:\r\n" +
			"LOCATION: http://%s:%s/description.xml\r\n" +
			"SERVER: FreeRTOS/7.4.2 UPnP/1.0 IpBridge/1.10.0\r\n" + 
			"hue-bridgeid: %s\r\n" +
			"ST: upnp:rootdevice\r\n" +
			"USN: uuid:2f402f80-da50-11e1-9b23-001788102201\r\n\r\n";

			discoveryResponse = String.format(discoveryTemplate, Configuration.UPNP_MULTICAST_ADDRESS, Configuration.UPNP_DISCOVERY_PORT, responseAddress, httpServerPort, HuePublicConfig.createConfig("temp", responseAddress).getBridgeid());
*/
	private String discoveryTemplate091516 = "HTTP/1.1 200 OK\r\n" +
			"CACHE-CONTROL: max-age=86400\r\n" +
			"EXT:\r\n" +
			"LOCATION: http://%s:%s/description.xml\r\n" +
			"SERVER: FreeRTOS/6.0.5, UPnP/1.0, IpBridge/1.10.0\r\n" + 
			"ST: urn:schemas-upnp-org:device:basic:1\r\n" +
			"USN: uuid:Socket-1_0-221438K0100073::urn:schemas-upnp-org:device:basic:1\r\n\r\n";
/*
	private String discoveryTemplateOld = "HTTP/1.1 200 OK\r\n" +
			"CACHE-CONTROL: max-age=86400\r\n" +
			"EXT:\r\n" +
			"LOCATION: http://%s:%s/upnp/amazon-ha-bridge/setup.xml\r\n" +
			"OPT: \"http://schemas.upnp.org/upnp/1/0/\"; ns=01\r\n" +
			"01-NLS: %s\r\n" +
			"ST: urn:schemas-upnp-org:device:basic:1\r\n" +
			"USN: uuid:Socket-1_0-221438K0100073::urn:Belkin:device:**\r\n\r\n";
*/	
	public UpnpListener(BridgeSettingsDescriptor theSettings, BridgeControlDescriptor theControl) {
		super();
		upnpResponsePort = theSettings.getUpnpResponsePort();
		httpServerPort = Integer.valueOf(theSettings.getServerPort());
		responseAddress = theSettings.getUpnpConfigAddress();
		strict = theSettings.isUpnpStrict();
		traceupnp = theSettings.isTraceupnp();
		bridgeControl = theControl;
		discoveryTemplateLatest = true;
	}

	@SuppressWarnings("resource")
	public boolean startListening(){
		log.info("UPNP Discovery Listener starting....");
		DatagramSocket responseSocket = null;
		MulticastSocket upnpMulticastSocket = null;
		Enumeration<NetworkInterface> ifs = null;

		boolean portLoopControl = true;
		int retryCount = 0;
		while(portLoopControl) {
			try {
				responseSocket = new DatagramSocket(upnpResponsePort);
				if(retryCount > 0)
					log.info("Upnp Response Port issue, found open port: " + upnpResponsePort);
				portLoopControl = false;
			} catch(SocketException e) {
				if(retryCount == 0)
					log.warn("Upnp Response Port is in use, starting loop to find open port for 20 tries - configured port is: " + upnpResponsePort);
				if(retryCount >= 20) {
					portLoopControl = false;
					log.error("Upnp Response Port issue, could not find open port - last port tried: " + upnpResponsePort + " with message: " + e.getMessage());
					return false;
				}
			}
			if(portLoopControl) {
				retryCount++;
				upnpResponsePort++;
			}
		}

		try {
			upnpMulticastSocket  = new MulticastSocket(Configuration.UPNP_DISCOVERY_PORT);
		} catch(IOException e){
			log.error("Upnp Discovery Port is in use, or restricted by admin (try running with sudo or admin privs): " + Configuration.UPNP_DISCOVERY_PORT + " with message: " + e.getMessage());
			return false;
		}
		
		InetSocketAddress socketAddress = new InetSocketAddress(Configuration.UPNP_MULTICAST_ADDRESS, Configuration.UPNP_DISCOVERY_PORT);
		try {
			ifs =	NetworkInterface.getNetworkInterfaces();
		}  catch (SocketException e) {
			log.error("Could not get network interfaces for this machine: " + e.getMessage());
			return false;
		}

		while (ifs.hasMoreElements()) {
			NetworkInterface xface = ifs.nextElement();
			Enumeration<InetAddress> addrs = xface.getInetAddresses();
			String name = xface.getName();
			int IPsPerNic = 0;

			while (addrs.hasMoreElements()) {
				InetAddress addr = addrs.nextElement();
				if (traceupnp)
					log.info("Traceupnp: " + name + " ... has addr " + addr);
				else
					log.debug(name + " ... has addr " + addr);
				if (InetAddressUtils.isIPv4Address(addr.getHostAddress())) {
					IPsPerNic++;
				}
			}
			log.debug("Checking " + name + " to our interface set");
			if (IPsPerNic > 0) {
				try {
					upnpMulticastSocket.joinGroup(socketAddress, xface);
					if (traceupnp)
						log.info("Traceupnp: Adding " + name + " to our interface set");
					else
						log.debug("Adding " + name + " to our interface set");
				} catch (IOException e) {
					log.warn("Multicast join failed for: " + socketAddress.getHostName() + " to interface: "
							+ xface.getName() + " with message: " + e.getMessage());
				}
			}
		}

		log.info("UPNP Discovery Listener running and ready....");
		boolean loopControl = true;
		boolean error = false;
		while (loopControl) { // trigger shutdown here
			byte[] buf = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				upnpMulticastSocket.receive(packet);
				if (isSSDPDiscovery(packet)) {
					try {
						sendUpnpResponse(responseSocket, packet.getAddress(), packet.getPort());
					} catch (IOException e) {
						if(e.getMessage().equalsIgnoreCase("Host is down"))
							log.warn("UpnpListener encountered an error sending upnp response packet as requesting host is now not available. IP: " + packet.getAddress().getHostAddress());
						else {
							log.error("UpnpListener encountered an error sending upnp response packet. Shutting down", e);
							error = true;
						}
					}
				}
			} catch (IOException e) {
				log.error("UpnpListener encountered an error reading socket. Shutting down", e);
				error = true;
			}
			if (error || bridgeControl.isReinit() || bridgeControl.isStop()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// noop
				}
				loopControl = false;
			}
		}
		upnpMulticastSocket.close();
		responseSocket.close();
		if (bridgeControl.isReinit())
			log.info("UPNP Discovery Listener - ended, restart found");
		if (bridgeControl.isStop())
			log.info("UPNP Discovery Listener - ended, stop found");
		if (!bridgeControl.isStop() && !bridgeControl.isReinit()) {
			log.info("UPNP Discovery Listener - ended, error found");
			return false;
		}
		return bridgeControl.isReinit();
	}

	/**
	 * ssdp discovery packet detection
	 */
	protected boolean isSSDPDiscovery(DatagramPacket packet){
		//Only respond to discover request for strict upnp form
		String packetString = new String(packet.getData(), 0, packet.getLength());
		if(packetString != null && packetString.startsWith("M-SEARCH * HTTP/1.1") && packetString.contains("\"ssdp:discover\"")){
			log.debug("isSSDPDiscovery Found message to be an M-SEARCH message.");
			log.debug("isSSDPDiscovery Got SSDP packet from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + ", body: " + packetString);

			if(strict && (packetString.contains("ST: urn:schemas-upnp-org:device:basic:1") || packetString.contains("ST: upnp:rootdevice") || packetString.contains("ST: ssdp:all")))
			{
				if(traceupnp) {
					log.info("Traceupnp: isSSDPDiscovery found message to be an M-SEARCH message.");
					log.info("Traceupnp: isSSDPDiscovery found message to be valid under strict rules - strict: " + strict);
					log.info("Traceupnp: SSDP packet from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + ", body: " + packetString);
				}
				else
					log.debug("isSSDPDiscovery found message to be valid under strict rules - strict: " + strict);
				return true;
			}
			else if (!strict)
			{
				if(traceupnp) {
					log.info("Traceupnp: isSSDPDiscovery found message to be an M-SEARCH message.");
					log.info("Traceupnp: isSSDPDiscovery found message to be valid under loose rules - strict: " + strict);
					log.info("Traceupnp: SSDP packet from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + ", body: " + packetString);
				}
				else
					log.debug("isSSDPDiscovery found message to be valid under loose rules - strict: " + strict);
				return true;
			}
		}
		else {
//			log.debug("isSSDPDiscovery found message to not be valid - strict: " + strict);
//			log.debug("SSDP packet from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + ", body: " + packetString);
		}
		return false;
	}

	protected void sendUpnpResponse(DatagramSocket socket, InetAddress requester, int sourcePort) throws IOException {
		String discoveryResponse = null;
		String bridgeIdMac = null;
		if(discoveryTemplateLatest) {
			bridgeIdMac = HuePublicConfig.createConfig("temp", responseAddress).getBridgeid();
			discoveryResponse = String.format(discoveryTemplate, responseAddress, httpServerPort, bridgeIdMac);
		}
		else
			discoveryResponse = String.format(discoveryTemplate091516, responseAddress, httpServerPort);
		if(traceupnp) {
			log.info("Traceupnp: sendUpnpResponse discovery template with address: " + responseAddress + " and port: " + httpServerPort);
			log.info("Traceupnp: discoveryResponse is <<<" + discoveryResponse + ">>>");
		}
		else
			log.debug("sendUpnpResponse discovery template with address: " + responseAddress + " and port: " + httpServerPort);
		DatagramPacket response = new DatagramPacket(discoveryResponse.getBytes(), discoveryResponse.length(), requester, sourcePort);
		socket.send(response);
	}
}
