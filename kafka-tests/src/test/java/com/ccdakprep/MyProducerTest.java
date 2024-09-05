package com.ccdakprep;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MyProducerTest {

    MockProducer<Integer, String> mockProducer;
    MyProducer myProducer;

    // Not kafka-specific. Just text fixtures so  we can capture the sout/serr

    private ByteArrayOutputStream systemOutContent;
    private ByteArrayOutputStream systemErrContent;
    private final PrintStream originalSystemOut = System.out;
    private final PrintStream originalSystemErr = System.err;


    @BeforeEach
    public void setUp() {
        mockProducer = new MockProducer<>(false, new IntegerSerializer(), new StringSerializer());
        myProducer = new MyProducer();
        myProducer.producer = mockProducer;
    }

    @BeforeEach
    public void setUpStreams() {
        systemOutContent = new ByteArrayOutputStream();
        systemErrContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(systemOutContent));
        System.setErr(new PrintStream(systemErrContent));
    }

     @AfterEach
    public void restoreStreams() {
        System.setOut(originalSystemOut);
        System.setErr(originalSystemErr);
     }

     @Test
    public void testPublishRecord_sent_data() {

        // given
         Integer recordKey = 1;
         String recordValue = "Test Data";

        // when
         myProducer.publishRecord(recordKey, recordValue);
         mockProducer.completeNext(); // autocomplete = false, we need to tell mockproducer how to respond to .send(), this could also be errorNext();

         // then
         List<ProducerRecord<Integer, String>> records = mockProducer.history();
         assertEquals(1, records.size());
         ProducerRecord<Integer, String> record = records.get(0);
         assertEquals(Integer.valueOf(recordKey), record.key());
         assertEquals(recordValue, record.value());
         assertEquals("test_topic", record.topic());
         assertEquals("key=1, value=Test Data\n", systemOutContent.toString());
         assertEquals("", systemErrContent.toString());
     }

    @Test
    public void testPublishRecord_error_occurred() {

        // given
        Integer recordKey = 2;
        String recordValue = "Test Data 2";
        RuntimeException exception = new RuntimeException("Failed publishing from test");

        // when
        myProducer.publishRecord(recordKey, recordValue);
        mockProducer.errorNext(exception);

        // then
        assertEquals("SLF4J: Failed to load class \"org.slf4j.impl.StaticLoggerBinder\".\n" +
                "SLF4J: Defaulting to no-operation (NOP) logger implementation\n" +
                "SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.\n" +
                "Failed publishing from test\n", systemErrContent.toString());
        assertEquals("", systemOutContent.toString());
    }


}
