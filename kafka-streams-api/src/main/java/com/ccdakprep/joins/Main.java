package com.ccdakprep.joins;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;

public class Main {

    public static void main(String[] args) {
        // Set up the configuration.
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "joins-example-1");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        // Since the input topic uses Strings for both key and value, set the default Serdes to String.
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // Get the source stream.
        final StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> left = builder.stream("joins-input-topic-left");
        KStream<String, String> right = builder.stream("joins-input-topic-right");

        // Perform an inner join.
        KStream<String, String> innerJoined = left.join(
                right,
                (leftValue, rightValue) -> "left=" + leftValue + ", right=" + rightValue,
                JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(5)));
        innerJoined.to("inner-join-output-topic");

        // Perform a left join.
        KStream<String, String> leftJoined = left.leftJoin(
                right,
                (leftValue, rightValue) -> "left=" + leftValue + ", right=" + rightValue,
                JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(5)));
        leftJoined.to("left-join-output-topic");

        // Perform an outer join.
        KStream<String, String> outerJoined = left.outerJoin(
                right,
                (leftValue, rightValue) -> "left=" + leftValue + ", right=" + rightValue,
                JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(5)));
        outerJoined.to("outer-join-output-topic");

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


//package com.ccdakprep.joins;
//
//import org.apache.kafka.common.serialization.Serdes;
//import org.apache.kafka.streams.KafkaStreams;
//import org.apache.kafka.streams.StreamsBuilder;
//import org.apache.kafka.streams.StreamsConfig;
//import org.apache.kafka.streams.Topology;
//import org.apache.kafka.streams.kstream.JoinWindows;
//import org.apache.kafka.streams.kstream.KStream;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.time.Duration;
//import java.util.Properties;
//import java.util.concurrent.CountDownLatch;
//
//public class Main {
//
//    // Set up a Logger instance for logging
//    private static final Logger logger = LoggerFactory.getLogger(Main.class);
//
//    public static void main(String[] args) {
//        final Properties props = new Properties();
//        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ccdak-streams-joins-app-local");
//        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
//        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100L);
//        props.put("cache.max.bytes", 0);
//        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
//        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
//
//        final StreamsBuilder builder = new StreamsBuilder();
//
//        KStream<String, String> leftStream = builder.stream("joins-input-topic-left");
//        KStream<String, String> rightStream = builder.stream("joins-input-topic-right");
//
//        // Log when a record is consumed from left topic
//        leftStream.peek((key, value) -> logger.info("Consumed record with key {} from left topic: {}", key, value));
//
//        // Log when a record is consumed from right topic
//        rightStream.peek((key, value) -> logger.info("Consumed record with key {} from right topic: {}", key, value));
//
//        KStream<String, String> innerJoined = leftStream.join(
//                rightStream,
//                (leftValue, rightValue) -> {
//                    logger.info("Performing inner join. Left Value: {}, Right Value: {}", leftValue, rightValue);
//                    return "left=" + leftValue + ", right=" + rightValue;
//                },
//                JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(80)));
//        innerJoined.to("inner-join-output-topic");
//
//        KStream<String, String> leftJoined = leftStream.leftJoin(
//                rightStream,
//                (leftValue, rightValue) -> {
//                    logger.info("Performing left join. Left Value: {}, Right Value: {}", leftValue, rightValue);
//                    return "left=" + leftValue + ", right=" + rightValue;
//                },
//                JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(80)));
//        leftJoined.to("left-join-output-topic");
//
//        KStream<String, String> outerJoined = leftStream.outerJoin(
//                rightStream,
//                (leftValue, rightValue) -> {
//                    logger.info("Performing outer join. Left Value: {}, Right Value: {}", leftValue, rightValue);
//                    return "left=" + leftValue + ", right=" + rightValue;
//                },
//                JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(80)));
//        outerJoined.to("outer-join-output-topic");
//
//        final Topology topology = builder.build();
//        final KafkaStreams streams = new KafkaStreams(topology, props);
//        logger.info("Starting Kafka Streams Application. Topology: {}", topology.describe());
//
//        final CountDownLatch latch = new CountDownLatch(1);
//
//        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
//            @Override
//            public void run() {
//                streams.close();
//                latch.countDown();
//                logger.info("Shutting down Kafka Streams Application");
//            }
//        });
//
//        try {
//            streams.start();
//            latch.await();
//        } catch (Exception e) {
//            logger.error("Exception in Kafka Streams Application: ", e);
//            System.exit(1);
//        }
//    }
//}


//package com.ccdakprep.joins;
//
//import org.apache.kafka.common.serialization.Serdes;
//import org.apache.kafka.streams.KafkaStreams;
//import org.apache.kafka.streams.StreamsBuilder;
//import org.apache.kafka.streams.StreamsConfig;
//import org.apache.kafka.streams.Topology;
//import org.apache.kafka.streams.kstream.JoinWindows;
//import org.apache.kafka.streams.kstream.KStream;
//
//import java.time.Duration;
//import java.util.Properties;
//import java.util.concurrent.CountDownLatch;
//
//// Example Streams Application for Joins (Stateful transformation)
//public class Main {
//
//    public static void main(String[] args) {
//// Setup Kafka configuration
//        final Properties props = new Properties();
//
//        // Application id
////        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ccdak-prep-streams-joins-example-application");
//        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ccdak-streams-joins-app-5");
////        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
//        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-broker-1:9092");
//        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100L);
//
//        // No caching (buffer before publishising) so we can see results immediately.
//        // Only for testing purposes. In production the cache would be utilized for better performance.
//        props.put("cache.max.bytes", 0);
//        // Since the input topic uses Strings for both key and value, set the default Serdes to String.
//        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
//        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
//
//        // Get the source data stream (from input topic)
//        final StreamsBuilder builder = new StreamsBuilder(); // Will help build the stream topology
//
//        // Stream processing logic
//
//        // Read from input topics (think pipeline)
//        KStream<String, String> leftStream = builder.stream("joins-input-topic-left");
//        KStream<String, String> rightStream = builder.stream("joins-input-topic-right");
//
//        // Perform an inner join (inner join is the default join function)
//        // An inner join only contains records that are present in both of the two entities being joined
//        KStream<String, String> innerJoined = leftStream.join(
//                rightStream,
//                (leftValue, rightValue) -> "left=" + leftValue + ", right=" + rightValue, // The Value Joiner: Determines the value of the combined record
//                JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(80))); // Windowing needed when joining two KStreams
//        innerJoined.to("inner-join-output-topic");
//
//        // Left join
//        // A left join contains all the records from the left entity, and if those records are also present in the right entity they are combined with those from the left entity.
//        // Similar to an inner join, but we are going to see records from the left topic, even if they are not present in the right topic.
//        KStream<String, String> leftJoined = leftStream.leftJoin(
//                rightStream,
//                (leftValue, rightValue) -> "left=" + leftValue + ", right=" + rightValue, // The Value Joiner: Determines the value of the combined record
//                JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(80))); // Windowing needed when joining two KStreams
//        leftJoined.to("left-join-output-topic");
//
//        // Outer join
//        // An outer join contains all of the records from both topics, and it will join the together if the same key exists in both topics
//        // So it returns everything, regardless of if there is a shared key. If there is a shared key, those records will be joined.
//        KStream<String, String> outerJoined = leftStream.outerJoin(
//                rightStream,
//                (leftValue, rightValue) -> "left=" + leftValue + ", right=" + rightValue, // The Value Joiner: Determines the value of the combined record
//                JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(80))); // Windowing needed when joining two KStreams
//        outerJoined.to("outer-join-output-topic");
//
//
//        // Boiler plate/common code
//        final Topology topology = builder.build();
//        final KafkaStreams streams = new KafkaStreams(topology, props);
//        // Print the topology to the console
//        System.out.println(topology.describe());
//        final CountDownLatch latch = new CountDownLatch(1);
//
//
//        // Attach a shutdown handler to catch control-c and terminate the application gracefully.
//        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
//            @Override
//            public void run() {
//                streams.close();
//                latch.countDown();
//            }
//        });
//
//        try {
//            // Stream streams processing
//            streams.start();
//            // Count down latch - to make sure this main method/application keeps running while the stream processing is still taking place
//            latch.await();
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//            System.exit(1);
//        }
//    }
//}
