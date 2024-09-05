package com.ccdakprep.aggregations;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

// Example Streams Application for Aggregations (Stateful transformation)
// aggregate > reduce > count
public class Main {
    public static void mainx(String[] args) {

        // Setup Kafka configuration
        final Properties props = new Properties();

        // Application id
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ccdak-streams-aggregations-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");

        // No caching (buffer before publishising) so we can see results immediately.
        // Only for testing purposes. In production the cache would be utilized for better performance.
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        // Since the input topic uses Strings for both key and value, set the default Serdes to String.
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // Get the source data stream (from input topic)
        final StreamsBuilder builder = new StreamsBuilder(); // Will help build the stream topology

        // Stream processing logic

        // Read from input topic
        final KStream<String, String> source = builder.stream("aggregations-input-topic");

        // Group the source stream by the existing Key.
        KGroupedStream<String, String> groupedStream = source.groupByKey();

        //Stateful transformations: Aggregate: Generates a new record from a calculation involving the grouped records
        // Create an aggregation that totals the length of characters of the value for all records sharing the same key
        KTable<String, Integer> aggregatedTable = groupedStream.aggregate(
                () -> 0, // Initializer - Initializes the aggregate value
                (aggKey, newValue, aggValue) -> aggValue + newValue.length(), // aggregation function/logic - aggKey = the key that is grouped by, newValue = the value of the current record, aggValue = the existing aggregated value (i.e. not including newValue). Initially 0 because of the line above
                Materialized.with(Serdes.String(), Serdes.Integer())); // To tell the kafka streams app how to build the data store (needed for stateful transformations) - optional if the serdes are the same as default

        aggregatedTable.toStream().to(
                "aggregations-output-charactercount-topic",
                Produced.with(Serdes.String(), Serdes.Integer())); // optional if the serdes are the same as default

        //Stateful transformations: Count: Counts the number of records for each grouped key
        KTable<String, Long> countedTable = groupedStream.count(Materialized.with(Serdes.String(), Serdes.Long()));
        countedTable.toStream().to("aggregations-output-count-topic", Produced.with(Serdes.String(), Serdes.Long()));

        //Stateful transformations: Reduce - Combines the grouped records into a single record
        // Combine the values of all records with the same key into a string seperated by spaces
        KTable<String, String> reducedTable = groupedStream.reduce(
                (aggValue, newValue) -> aggValue + " " + newValue);
        reducedTable.toStream().to("aggregations-output-reduce-topic");

        // Boiler plate/common code
        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        // Print the topology to the console
        System.out.println(topology.describe());
        final CountDownLatch latch = new CountDownLatch(1);


        // Attach a shutdown handler to catch control-c and terminate the application gracefully.
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
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