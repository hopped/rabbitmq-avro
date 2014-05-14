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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hopped.runner.avro.Run;
import com.hopped.runner.avro.RunList;
import com.hopped.runner.avro.RunRequest;
import com.hopped.runner.avro.User;
import com.hopped.runner.avro.UserRef;

/**
 * @author Dennis Hoppe (hoppe.dennis@ymail.com)
 * 
 */
public class Main {

    /** Logger */
    final static Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        RPCClient client = new RPCClient("running_queue");

        // (1) Create a user object used by the following request
        User defaultUser = User.newBuilder()
                .setNameOrAlias("Dennis")
                .setId(1)
                .build();

        // (2) Create request object to be sent via queue
        UserRef uRef = UserRef.newBuilder().setId(defaultUser.getId()).build();
        RunRequest request = RunRequest.newBuilder()
                .setUser(uRef)
                .build();

        // (3) Call the client's method
        logger.info(" [>] Send request for user "
                + defaultUser.getNameOrAlias());

        RunList response = client.getRunsByUser(request);

        logger.info(" [<] Received data for "
                + response.getRuns().size() + " runs");

        // (4) Loop over returned results, e.g. runs done by defaultUser
        double totalDistance = 0.0;
        for (Run run : response.getRuns()) {
            totalDistance += run.getDistanceMeters();
        }

        logger.info(" [-] " + defaultUser.getNameOrAlias()
                + " ran " + totalDistance + " meters in total");

        client.close();
    }
}