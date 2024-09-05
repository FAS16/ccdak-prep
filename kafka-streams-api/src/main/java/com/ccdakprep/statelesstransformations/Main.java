package com.ccdakprep.statelesstransformations;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;


public class Main {
    public static void mainy(String[] args) {
        // Setup Kafka configuration
        final Properties props = new Properties();

        // Application id
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ccdak-streams-stateless-trans-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        
        // No caching (buffer before publishising) so we can see results immediately.
        // Only for testing purposes. In production the cache would be utilized for better performance.
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // Get the source data stream (from input topic)
        final StreamsBuilder builder = new StreamsBuilder(); // Will help build the stream topology

        // Stream processing logic
        final KStream<String, String> source = builder.stream("stateless-transformations-input-topic");

        // Stateless transformations: Branching, splitting a data streams into multiple streams
        // Create a BranchedKStream with the split method
        var branched = source.split(Named.as("Split-"))
            .branch((key, value) -> key.startsWith("a"), Branched.as("startsWithA"))
            .defaultBranch(Branched.as("everythingElse"));

        // Retrieve the individual KStreams
        KStream<String, String> streamForKeysStartingWithA = branched.get("Split-startsWithA");
        KStream<String, String> streamForEverythingElse = branched.get("Split-everythingElse");

        // Stateless transformations: Filtering, removing records from a stream.
        // Remove records from the stream where the value does not start with "a"
        streamForKeysStartingWithA = streamForKeysStartingWithA
        .filter((key, value) -> value.startsWith("a"));

        // Stateless transformations: The flatMap transformation is used to transform each record of the input stream into zero or more records in the output stream.
        streamForKeysStartingWithA = streamForKeysStartingWithA
        .flatMap((key, value) -> {
            List<KeyValue<String, String>> result = new ArrayList<>();
            result.add(KeyValue.pair(key, value.toUpperCase()));
            result.add(KeyValue.pair(key, value.toLowerCase()));
            return result;
        });

        // Stateless transformations: The foreach transformation is used to perform an arbitrary stateless action on each record. 
        // It does not return anything (terminal operation). Typically used for side effect (logging, updating an external system, other actions that don't involve sending records to another kafka topic)
        // streamForKeysStartingWithA.foreach((key, value) -> System.out.println("key=" + key + ", value=" + value));

        // Stateless transformations: GroupBy/GroupByKey are stateless transformations, but are used and required to perform stateful transformations
    
        // Stateless transformations: Map, similar to flatMap but processes one record and returns exactly one record.
        streamForKeysStartingWithA = streamForKeysStartingWithA.map((key, value) -> KeyValue.pair(key.toUpperCase(), value));

        // Stateless transformations: Merge, the inverse of Branching. Allows us to merge two streams into one.
        // Merging the two streams back together (after potential processing)
        KStream<String, String> mergedStream = streamForKeysStartingWithA.merge(streamForEverythingElse);

        // Stateless transformations: Peek, similar to forech, but does not stop processing (meaning it is not terminal)
        mergedStream = mergedStream.peek((key, value) -> System.out.println("key=" + key + ", value=" + value));        

        // Finally output the transformed data to a topic
        mergedStream.to("stateless-transformations-output-topic");
        

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