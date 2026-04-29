package com.audiocall.controller;

import com.audiocall.model.PhoneNumber;
import com.audiocall.service.PhoneNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/phone-numbers")
@RequiredArgsConstructor
public class PhoneNumberController {

    private final PhoneNumberService service;

    @GetMapping
    public ResponseEntity<Page<PhoneNumber>> getAll(
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(service.findAll(group, search, pageRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PhoneNumber> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<String>> getGroups() {
        return ResponseEntity.ok(service.findAllGroups());
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount() {
        return ResponseEntity.ok(Map.of("active", service.countActive()));
    }

    @PostMapping
    public ResponseEntity<PhoneNumber> add(@RequestBody Map<String, String> body) {
        PhoneNumber phone = service.addNumber(
                body.get("number"),
                body.get("name"),
                body.get("group")
        );
        return ResponseEntity.ok(phone);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "group", required = false) String group) {
        return ResponseEntity.ok(service.uploadFromCsv(file, group));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PhoneNumber> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        PhoneNumber phone = service.updateNumber(
                id,
                (String) body.get("name"),
                (String) body.get("group"),
                body.containsKey("active") ? (Boolean) body.get("active") : null
        );
        return ResponseEntity.ok(phone);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteNumber(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deleteBulk(@RequestBody Map<String, List<Long>> body) {
        service.deleteNumbers(body.get("ids"));
        return ResponseEntity.noContent().build();
    }
}
