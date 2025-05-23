/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.networkingproj;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author moustafa
 */
public class Player {
    
        private static final Charset CHARSET = StandardCharsets.UTF_8;
        private final SocketChannel channel;
        private String name;
        private String currentGameId;
        private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        private final StringBuilder messageBuffer = new StringBuilder();
        private final Queue<String> writeQueue = new LinkedList<>();

        public Player(SocketChannel channel) throws IOException {
            this.channel = channel;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCurrentGameId() {
            return currentGameId;
        }

        public void setCurrentGameId(String gameId) {
            this.currentGameId = gameId;
        }

        public SocketChannel getChannel() {
            return channel;
        }

        public boolean isConnected() {
            return channel.isOpen();
        }

        public void sendMessage(String message) {
            synchronized (writeQueue) {
                writeQueue.add(message + "\n");
            }
        }

        public void flushMessages() throws IOException {
            synchronized (writeQueue) {
                while (!writeQueue.isEmpty()) {
                    String message = writeQueue.peek();
                    ByteBuffer buffer = CHARSET.encode(message);
                    int bytesWritten = channel.write(buffer);
                    if (bytesWritten == 0) {
                        return;
                    }
                    if (!buffer.hasRemaining()) {
                        writeQueue.poll();
                    } else {
                        return;
                    }
                }
            }
        }

        public String readMessage() throws IOException {
            int bytesRead = channel.read(readBuffer);
            if (bytesRead == -1) return null;
            if (bytesRead == 0) return "";

            readBuffer.flip();
            StringBuilder currentMessage = new StringBuilder();
            boolean messageComplete = false;
            while (readBuffer.hasRemaining()) {
                char c = (char) readBuffer.get();
                if (c == '\n') {
                    messageComplete = true;
                    break;
                }
                currentMessage.append(c);
            }

            String message = currentMessage.toString().trim();
            if (messageComplete) {
                messageBuffer.setLength(0);
                readBuffer.compact();
                if (!message.isEmpty()) return message;
                return "";
            } else {
                messageBuffer.append(message);
                readBuffer.compact();
                return "";
            }
        }

        public void disconnect() {
            try {
                channel.close();
            } catch (IOException e) {
            }
        }
    }
