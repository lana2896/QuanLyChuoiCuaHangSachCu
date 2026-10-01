package com.oldbook.test.controller;

import com.oldbook.test.model.TestEntity;
import com.oldbook.test.repository.TestRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test/db")
public class TestDBController {

    private final TestRepository testRepository;

    public TestDBController(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    @GetMapping
    public Map<String, Object> checkDb() {
        return Map.of("status", "DB OK", "totalRows", testRepository.count());
    }

    @GetMapping("/add")
    public TestEntity add(@RequestParam(defaultValue = "test") String name) {
        return testRepository.save(new TestEntity(name));
    }

    @GetMapping("/all")
    public List<TestEntity> all() {
        return testRepository.findAll();
    }
}
