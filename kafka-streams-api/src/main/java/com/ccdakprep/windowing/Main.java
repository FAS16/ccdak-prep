package com.ccdakprep.windowing;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.kstream.internals.TimeWindow;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

// Example of windowing (used in connection with stateful transformations)
// Windowing basically resets the stream based on the time windows.
// This can be useful to e.g. monitor how many users in average used your service per hour
// 4 types of windowing. 1) Tumbling time windows 2) Hopping time windows 3) Sliding time windows 4) Sessions windows
public class Main {

    public static void main(String[] args) {
        // Set up the configuration.
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "windowing-example");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        // Since the input topic uses Strings for both key and value, set the default Serdes to String.
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // Get the source stream.
        final StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> source = builder.stream("windowing-input-topic");

        // Group the source stream by the existing Key (because we want to do an aggregation)
        KGroupedStream<String, String> groupedStream = source.groupByKey();

        // Apply windowing to the stream with tumbling windows of 10 seconds
        // This is going to divide the stream up into the time windows.
        TimeWindowedKStream<String, String> windowedStream = groupedStream
                .windowedBy(
                        TimeWindows.ofSizeWithNoGrace(
                                        Duration.ofSeconds(10)) // Windows will be 10 seconds long
                                .advanceBy(Duration.ofSeconds(10)) // We are going to start a new window every 10 seconds (optional when same size as window)
                );

        // Example of applying windowing to the stream with hopping windows
        // This is going to divide the stream up into the time windows.
        /*TimeWindowedKStream<Object, Object> hoppingWindowedStream = groupedStream
                .windowedBy(
                        TimeWindows.ofSizeWithNoGrace(
                                        Duration.ofSeconds(10)) // Windows will be 10 seconds long
                                .advanceBy(Duration.ofSeconds(12)) // This is what makes it hopping, because we are creating gaps of 2 seconds in between our windows
                );*/

        // Now that we have windowed we can aggregate (or do any stateful transformation) as usual. Now they will be performed on records in the same window

        // Combine the values of all records with the same key into a string separated by spaces, using 10-second windows.
        // Key is no longer default key type, so we cannot use the default string serde for the key
        KTable<Windowed<String>, String> reducedTable = windowedStream.reduce((aggValue, newValue) -> aggValue + " " + newValue);
        reducedTable.toStream().to("windowing-output-topic", Produced.with(WindowedSerdes.timeWindowedSerdeFrom(String.class, 10L), Serdes.String()));

        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        // Print the topology to the console.
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
            streams.start();
            latch.await();
        } catch (final Throwable e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }
}

