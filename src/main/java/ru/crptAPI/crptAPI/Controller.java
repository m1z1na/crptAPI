package ru.crptAPI.crptAPI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@RestController
@RequestMapping("/v1")
@Tag(name = "Класс-контроллер создания документов")
public class Controller {

    private final Memory memory;
    private final Limiter limiter;

    @Autowired
    public Controller(Memory memory, Limiter limiter) {
        this.memory = memory;
        this.limiter = limiter;
    }


    @PostMapping("/")
    @Operation(summary = "Создать документ")
    public ResponseEntity<Data> create(@RequestBody Document document, String signature) {
        if (limiter.makeApiRequest() == true) {
            return new ResponseEntity<Data>(
                    memory.save(new Data(document, signature)),
                    HttpStatus.CREATED
            );
        } else {
            return ResponseEntity.status(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED).build();
        }
    }

    @Repository
    static class Memory {
        private final Map<Integer, Data> dataMem = new ConcurrentHashMap<>();
        private final AtomicInteger counter = new AtomicInteger(0);

        public Data save(Data data) {
            data.setId(counter.addAndGet(1));
            return dataMem.put(data.getId(), data);
        }
    }

    public static class Document {
        private String document;
    }

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class Data {
        @EqualsAndHashCode.Include
        private int id;
        private Document document;
        private String signature;


        public Data(Document document, String signature) {
            this.document = document;
            this.signature = signature;
        }
    }

    public static class Limiter {

        private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        private static int requestCount = 0;
        private final int requestLimit;


        public Limiter(TimeUnit timeUnit, int requestLimit) {
            this.requestLimit = requestLimit;
            scheduler.scheduleAtFixedRate(Limiter::resetRequestCount, 0, 1, timeUnit);
        }

        private static void resetRequestCount() {
            requestCount = 0;
        }

        private synchronized boolean makeApiRequest() {

            var response = false;
            if (requestCount < this.requestLimit) {
                response = true;
                requestCount++;
            }
            return response;
        }
    }

}
