package com.ccdakprep;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Some of the features offered by kafka-streams-test-utls include:
 *
 * 1. TopologyTestDriver - Allows you to feed test records in, simulates your topology, and returns outuput records
 *
 * 2. ConsumerRecordFactory - Helps you convert consumer record data into byte arrays that can be processed by the TopologyTestDriver
 *
 * 3. OutputVerifier - Provides helper methods for verifying output records in your tests.
 */
public class MyStreamsTest {

    MyStreams myStreams;
    TopologyTestDriver testDriver; // Allows us to simulate our kafka streams application

    @BeforeEach
    public void setUp() {
        myStreams = new MyStreams();
        Topology topology = myStreams.topology;

        // Basic configuration for TopologyTestDriver
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                Serdes.Integer().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                Serdes.String().getClass().getName());
        testDriver = new TopologyTestDriver(topology, props);
    }

    @AfterEach
    public void tearDown() {
        testDriver.close();
    }

    @Test
    public void test_first_name() {
        // Verify that the stream reverses the record value.
        TestInputTopic<Integer, String> inputTopic = testDriver.createInputTopic("test_input_topic", new IntegerSerializer(), new StringSerializer());

        // Send a record to the input topic
        inputTopic.pipeInput(1, "reverse");

        // Create a test output topic
        TestOutputTopic<Integer, String> outputTopic = testDriver.createOutputTopic(
                "test_output_topic",
                new IntegerDeserializer(),
                new StringDeserializer());


        // Read the output record
        KeyValue<Integer, String> outputRecord = outputTopic.readKeyValue();

        // Verify the output record
        assertEquals(1, outputRecord.key);
        assertEquals("esrever", outputRecord.value);
    }
}


/**
 *  In recent versions of Kafka Streams, ConsumerRecordFactory and ConsumerRecord are no longer needed for testing purposes. Instead, Kafka Streams provides the TestInputTopic and TestOutputTopic classes for a more streamlined and effective way of writing tests.
 *
 * The TestInputTopic and TestOutputTopic classes simplify the process of writing tests for Kafka Streams applications. They offer a more straightforward and fluent way to send test records into your topology and read the results from it, without the need for manually creating ConsumerRecord instances.
 *
 * TestInputTopic: This class is used to easily produce records to the input topic of your Kafka Streams application. It handles the serialization of keys and values and allows you to directly send data into the stream for testing.
 * TestOutputTopic: Similarly, TestOutputTopic is used to read records from the output topic of your Kafka Streams application. It deserializes the keys and values, making it easy to verify the output of your stream processing logic.
 *
 * Using these classes, you can effectively test your Kafka Streams applications without dealing with the low-level details of record creation and serialization/deserialization, making your tests cleaner and more focused on the business logic.
 */