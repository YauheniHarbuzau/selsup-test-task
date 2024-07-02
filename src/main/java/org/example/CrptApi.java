package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.Month.JANUARY;

public class CrptApi {

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final ObjectMapper objectMapper;
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private long beginningTime = System.currentTimeMillis();
    private long currentTime;
    private long passedTime;

    private CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public static void main(String[] args) {
        var crptApi = new CrptApi(TimeUnit.SECONDS, 5);

        crptApi.createDocument(Constant.DOCUMENT, "signature");
    }

    @SneakyThrows
    private void createDocument(Object document, String signature) {
        synchronized (this) {
            currentTime = System.currentTimeMillis();
            passedTime = currentTime - beginningTime;
            requestCounterReset();

            while (requestCounter.get() >= requestLimit) {
                wait(timeUnit.toMillis(1) - passedTime);
                currentTime = System.currentTimeMillis();
                passedTime = currentTime - beginningTime;
                requestCounterReset();
            }

            try (var httpClient = HttpClientBuilder.create().build()) {
                var httpPost = new HttpPost(Constant.URL);
                var documentJson = objectMapper.writeValueAsString(document);
                var stringEntity = new StringEntity(documentJson);

                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Signature", signature);
                httpPost.setEntity(stringEntity);

                var httpResponse = httpClient.execute(httpPost);
                var statusCode = httpResponse.getCode();
                var response = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");

                createDocumentResultPrint(statusCode, response);
                requestCounter.getAndIncrement();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void requestCounterReset() {
        if (passedTime >= timeUnit.toMillis(1)) {
            requestCounter.set(0);
            beginningTime = System.currentTimeMillis();
        }
    }

    private void createDocumentResultPrint(int statusCode, String response) {
        System.out.println(statusCode == HttpStatus.SC_CREATED ?
                String.format(Constant.DOCUMENT_CREATED_MESSAGE, statusCode, response) :
                String.format(Constant.DOCUMENT_NOT_CREATED_MESSAGE, statusCode, response));
    }

    @Data
    @Builder
    static class Product {

        @JsonProperty("certificate_document")
        private String certificateDocument;

        @JsonProperty("certificate_document_date")
        @JsonFormat(pattern = Constant.DATE_TIME_FORMAT)
        private LocalDateTime certificateDocumentDate;

        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @JsonProperty("production_date")
        @JsonFormat(pattern = Constant.DATE_TIME_FORMAT)
        private LocalDateTime productionDate;

        @JsonProperty("tnved_code")
        private String tnvedCode;

        @JsonProperty("uit_code")
        private String uitCode;

        @JsonProperty("uitu_code")
        private String uituCode;
    }

    @Data
    @Builder
    static class Description {

        @JsonProperty("participantInn")
        private String participantInn;
    }

    @Data
    @Builder
    static class Document {

        @JsonProperty("description")
        private Description description;

        @JsonProperty("doc_id")
        private String docId;

        @JsonProperty("doc_status")
        private String docStatus;

        @JsonProperty("doc_type")
        private DocType docType;

        @JsonProperty("importRequest")
        private Boolean importRequest;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("participant_inn")
        private String participantInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @JsonProperty("production_date")
        @JsonFormat(pattern = Constant.DATE_TIME_FORMAT)
        private LocalDateTime productionDate;

        @JsonProperty("production_type")
        private String productionType;

        @JsonProperty("products")
        private List<Product> products;

        @JsonProperty("reg_date")
        private String regDate;

        @JsonProperty("reg_number")
        private String regNumber;
    }

    enum DocType {
        LP_INTRODUCE_GOODS
    }

    static class Constant {

        private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

        private static final String DATE_TIME_FORMAT = "yyyy-MM-dd";

        private static final String DOCUMENT_CREATED_MESSAGE = "Document created successfully - status code: %s\nResponse: %s";

        private static final String DOCUMENT_NOT_CREATED_MESSAGE = "Failed to create document - status code: %s\nResponse: %s";

        private static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(2020, JANUARY, 23, 0, 0, 0);

        private static final Product PRODUCT = Product.builder()
                .certificateDocument("string")
                .certificateDocumentDate(LOCAL_DATE_TIME)
                .certificateDocumentNumber("string")
                .ownerInn("string")
                .producerInn("string")
                .productionDate(LOCAL_DATE_TIME)
                .tnvedCode("string")
                .uitCode("string")
                .uituCode("string")
                .build();

        private static final Description DESCRIPTION = Description.builder()
                .participantInn("string")
                .build();

        private static final Document DOCUMENT = Document.builder()
                .description(DESCRIPTION)
                .docId("string")
                .docStatus("string")
                .docType(DocType.LP_INTRODUCE_GOODS)
                .importRequest(true)
                .ownerInn("string")
                .participantInn("string")
                .producerInn("string")
                .productionDate(LOCAL_DATE_TIME)
                .productionType("string")
                .products(List.of(PRODUCT))
                .regDate("string")
                .regNumber("string")
                .build();
    }
}
