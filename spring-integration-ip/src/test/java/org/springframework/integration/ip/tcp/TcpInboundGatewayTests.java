/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ip.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import javax.net.SocketFactory;

import org.junit.Test;

import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.integration.support.channel.ChannelResolver;

public class TcpInboundGatewayTests {

	@Test
	public void testNetSingle() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		scf.setSingleUse(true);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 200) {
				fail("Failed to listen");
			}
		}
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		handler.setChannelResolver(new ChannelResolver() {
			public MessageChannel resolveChannelName(String channelName) {
				return channel;
			}
		});
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive());
		handler.handleMessage(channel.receive());
		byte[] bytes = new byte[12];
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test1\r\n", new String(bytes));
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test2\r\n", new String(bytes));
	}

	@Test
	public void testNetNotSingle() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		scf.setSingleUse(false);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 200) {
				fail("Failed to listen");
			}
		}
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive());
		handler.handleMessage(channel.receive());
		byte[] bytes = new byte[12];
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test1\r\n", new String(bytes));
		readFully(socket.getInputStream(), bytes);
		assertEquals("Echo:Test2\r\n", new String(bytes));
	}

	@Test
	public void testNioSingle() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		scf.setSingleUse(true);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 200) {
				fail("Failed to listen");
			}
		}
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		handler.setChannelResolver(new ChannelResolver() {
			public MessageChannel resolveChannelName(String channelName) {
				return channel;
			}
		});
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive());
		handler.handleMessage(channel.receive());
		Set<String> results = new HashSet<String>();
		byte[] bytes = new byte[12];
		readFully(socket.getInputStream(), bytes);
		results.add(new String(bytes));
		readFully(socket.getInputStream(), bytes);
		results.add(new String(bytes));
		assertTrue(results.remove("Echo:Test1\r\n"));
		assertTrue(results.remove("Echo:Test2\r\n"));
	}

	@Test
	public void testNioNotSingle() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		scf.setSingleUse(false);
		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 200) {
				fail("Failed to listen");
			}
		}
		final QueueChannel channel = new QueueChannel();
		gateway.setRequestChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new Service());
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		handler.handleMessage(channel.receive());
		handler.handleMessage(channel.receive());
		Set<String> results = new HashSet<String>();
		byte[] bytes = new byte[12];
		readFully(socket.getInputStream(), bytes);
		results.add(new String(bytes));
		readFully(socket.getInputStream(), bytes);
		results.add(new String(bytes));
		assertTrue(results.remove("Echo:Test1\r\n"));
		assertTrue(results.remove("Echo:Test2\r\n"));
	}

	private class Service {
		@SuppressWarnings("unused")
		public String serviceMethod(byte[] bytes) {
			return "Echo:" + new String(bytes);
		}
	}

	private void readFully(InputStream is, byte[] buff) throws IOException {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = (byte) is.read();
		}
	}
	
}
