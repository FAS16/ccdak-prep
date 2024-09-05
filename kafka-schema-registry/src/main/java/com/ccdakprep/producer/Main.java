package com.ccdakprep.producer;

import com.ccdakprep.Person;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;


import java.time.Period;
import java.util.Properties;

// A producer that creates the 'Person' schema (if not created) and uses the schema to publish records
public class Main {
    public static void main(String[] args) {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Using KafkaAvroSerializer to serialize the data upon publishing
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

        // Producer will register schema, thus it needs to know where to contact the schema registry
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8085");

        // Auto-generated Person by avro plugin. Now the avro serializer will take care of serializing upon publishing
        KafkaProducer<String, Person> producer = new KafkaProducer<>(props);

        Person fahad = new Person(501951765, "Fahad Ali", "Sajad", "Fahadalisajad@hotmail.com");
        producer.send(new ProducerRecord<String, Person>("employees", Integer.toString(fahad.getId()), fahad));

        Person sharoz = new Person(806961765, "Sharoz Ali", "Sajad", "Sharozalisajad@hotmail.com");
        producer.send(new ProducerRecord<String, Person>("employees", Integer.toString(sharoz.getId()), sharoz));

        producer.close();

    }
}