package com.illumina.demo;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

public class Consumer {

    private static final String QUEUE_MANAGER_NAME = "*ANY_QM";
    private static final String QUEUE_NAME         = "Q1";
    private static final String CCDT_URL           = "file:///app/config/ccdt.json";

    // System exit status value (assume unset value to be 1)
    private static int status = 1;

    public static void main(String[] args) {

        System.out.println("Starting....");

        Connection connection = null;
        Session session = null;
        MessageConsumer consumer = null;
        try {
            JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
            JmsConnectionFactory cf = ff.createConnectionFactory();
            // Auto-reconnect is needed for the uniform cluster
            cf.setIntProperty(WMQConstants.WMQ_CLIENT_RECONNECT_OPTIONS, WMQConstants.WMQ_CLIENT_RECONNECT);
            // Application name is used to load-balance connections across the cluster
            cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "ConsAppl");
            // *ANY_QM: connect to whichever queue manager the CCDT offers
            cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QUEUE_MANAGER_NAME);
            cf.setStringProperty(WMQConstants.WMQ_CCDTURL, CCDT_URL);

            connection = cf.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(QUEUE_NAME);
            consumer = session.createConsumer(destination);

            connection.start();
            System.out.println("Connection opened.");

            while (true) {
                TextMessage message = (TextMessage) consumer.receive();
                System.out.println("Received message: " + message.getText());
            }
        } catch (JMSException ex) {
            recordFailure(ex);
        } finally {
            closeQuietly(consumer, session, connection);
        }
        System.exit(status);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static void closeQuietly(MessageConsumer consumer, Session session, Connection connection) {
        if (consumer != null) {
            try { consumer.close(); } catch (JMSException e) { recordFailure(e); }
        }
        if (session != null) {
            try { session.close(); } catch (JMSException e) { recordFailure(e); }
        }
        if (connection != null) {
            try { connection.close(); } catch (JMSException e) { recordFailure(e); }
        }
    }

    private static void processJMSException(JMSException jmsex) {
        System.out.println(jmsex);
        Throwable inner = jmsex.getLinkedException();
        while (inner != null) {
            System.out.println("  caused by: " + inner);
            inner = inner.getCause();
        }
    }

    private static void recordFailure(Exception ex) {
        if (ex instanceof JMSException) {
            processJMSException((JMSException) ex);
        } else {
            System.out.println(ex);
        }
        System.out.println("FAILURE");
        status = -1;
    }
}
