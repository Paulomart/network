/*
 * This file is part of Flow Network, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Flow Powered <https://flowpowered.com/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.flowpowered.network.pipeline;

import java.util.List;

import com.flowpowered.network.Codec.CodecRegistration;
import com.flowpowered.network.CodecContext;
import com.flowpowered.network.Message;
import com.flowpowered.network.protocol.Protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

/**
 * A {@link MessageToMessageEncoder} which encodes into {@link ByteBuf}s.
 */
public class MessageEncoder extends MessageToMessageEncoder<Message> {
    private final MessageHandler messageHandler;
    private CodecContext context;

    public MessageEncoder(final MessageHandler handler) {
        this.messageHandler = handler;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message message, List<Object> out) throws Exception {
        if (context == null && messageHandler.getSession() != null) {
            context = new CodecContext(messageHandler.getSession());
        }
        final Protocol protocol = messageHandler.getSession().getProtocol();
        final Class<? extends Message> clazz = message.getClass();
        CodecRegistration reg = protocol.getCodecRegistration(message.getClass());
        if (reg == null) {
            throw new Exception("Unknown message type: " + clazz + ".");
        }
        ByteBuf messageBuf = ctx.alloc().buffer();
        messageBuf = reg.getCodec().encode(context, messageBuf, message);

        ByteBuf headerBuf = ctx.alloc().buffer();
        headerBuf = protocol.writeHeader(headerBuf, reg, messageBuf);
        out.add(Unpooled.wrappedBuffer(headerBuf, messageBuf));
    }
}