package com.hopped.runner.rabbitmq;

/*!
 * Copyright (c) 2014 Dennis Hoppe
 * www.dennis-hoppe.com
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

import java.io.ByteArrayOutputStream;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

import com.hopped.runner.avro.RunList;
import com.hopped.runner.avro.RunRequest;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * Example is based on the excellent RPC tutorial by RabbitMQ:
 * http://goo.gl/CSqs2w
 * 
 * @author Dennis Hoppe (hoppe.dennis@ymail.com)
 * 
 */
public class RPCClient {

    private BinaryEncoder encoder = null;
    private final ByteArrayOutputStream baos;
    private final Channel channel;
    private final Connection connection;
    private BinaryDecoder decoder = null;
    private final QueueingConsumer consumer;
    private final String replyQueueName;
    private final String requestQueueName;

    /**
     * @param requestQueueName
     * @throws Exception
     */
    public RPCClient(String requestQueueName) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        this.requestQueueName = requestQueueName;
        connection = factory.newConnection();
        channel = connection.createChannel();

        replyQueueName = channel.queueDeclare().getQueue();
        consumer = new QueueingConsumer(channel);
        channel.basicConsume(replyQueueName, true, consumer);

        baos = new ByteArrayOutputStream();
    }

    /**
     * @param request
     * @return
     * @throws Exception
     */
    public RunList getRunsByUser(RunRequest request) throws Exception {
        RunList response = new RunList();
        String corrId = java.util.UUID.randomUUID().toString();

        BasicProperties props = new BasicProperties.Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        // Serialization
        baos.reset();
        DatumWriter<RunRequest> avroWriter =
                new SpecificDatumWriter<RunRequest>(RunRequest.class);
        encoder = EncoderFactory.get().binaryEncoder(baos, encoder);
        avroWriter.write(request, encoder);
        encoder.flush();

        channel.basicPublish("", requestQueueName, props, baos.toByteArray());

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                // Deserialization
                DatumReader<RunList> avroReader =
                        new SpecificDatumReader<RunList>(RunList.class);
                decoder = DecoderFactory.get().binaryDecoder(
                        delivery.getBody(), decoder);
                response = avroReader.read(response, decoder);
                break;
            }
        }

        return response;
    }

    /**
     * @throws Exception
     */
    public void close() throws Exception {
        connection.close();
    }

}
