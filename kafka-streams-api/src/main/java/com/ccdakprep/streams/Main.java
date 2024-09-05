package com.ccdakprep.streams;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;

// This is a simple example of a kafka streams application that just process a stream of data from one topic and copies/publishes it to a different topic
public class Main {
    public static void mainz(String[] args) {

        // Setup Kafka configuration
        final Properties props = new Properties();

        // Application id
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ccdak-streams-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        
        // No caching (buffer before publishising) so we can see results immediately.
        // Only for testing purposes. In production the cache would be utilized for better performance.
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // Get the source data stream (from input topic)
        final StreamsBuilder builder = new StreamsBuilder(); // Will help build the stream topology

        // Stream processing logic - simply forward data records from the stream to the output topic
        final KStream<String, String> source = builder.stream("streams-input-topic");

        // Write data records in stream to output topic
        source.to("streams-output-topic");

        // Boiler plate/common code
        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        // Print the topology to the console
        System.out.println(topology.describe());
        final CountDownLatch latch = new CountDownLatch(1);

        
        // Attach a shutdown handler to catch control-c and terminate the application gracefully.
        Runtime.getRuntime().addShutdownHook(new Thread("streams-wordcount-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
            }
        });

        try {
            // Stream streams processing
            streams.start();
            // Count down latch - to make sure this main method/application keeps running while the stream processing is still taking place
            latch.await();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
}