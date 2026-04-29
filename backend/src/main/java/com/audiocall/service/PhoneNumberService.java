package com.audiocall.service;

import com.audiocall.model.PhoneNumber;
import com.audiocall.repository.PhoneNumberRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneNumberService {

    private final PhoneNumberRepository repository;

    // Matches Bangladesh phone numbers: +880 1XXXXXXXXX
    private static final Pattern BD_PHONE_PATTERN =
            Pattern.compile("^\\+?880\\d{10}$");

    public Page<PhoneNumber> findAll(String group, String search, Pageable pageable) {
        return repository.findFiltered(group, search, pageable);
    }

    public PhoneNumber findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Phone number not found: " + id));
    }

    public List<String> findAllGroups() {
        return repository.findAllGroups();
    }

    @Transactional
    public PhoneNumber addNumber(String number, String name, String group) {
        String normalized = normalizeNumber(number);
        if (repository.existsByNumber(normalized)) {
            throw new RuntimeException("Phone number already exists: " + normalized);
        }

        PhoneNumber phone = PhoneNumber.builder()
                .number(normalized)
                .name(name)
                .group(group)
                .active(true)
                .build();

        return repository.save(phone);
    }

    @Transactional
    public PhoneNumber updateNumber(Long id, String name, String group, Boolean active) {
        PhoneNumber phone = findById(id);
        if (name != null) phone.setName(name);
        if (group != null) phone.setGroup(group);
        if (active != null) phone.setActive(active);
        return repository.save(phone);
    }

    @Transactional
    public void deleteNumber(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void deleteNumbers(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    /**
     * Bulk upload phone numbers from a CSV file.
     * Expected CSV format: number, name (optional), group (optional)
     */
    @Transactional
    public Map<String, Object> uploadFromCsv(MultipartFile file, String defaultGroup) {
        List<PhoneNumber> added = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();

            // Skip header row if present
            int startIdx = 0;
            if (rows.size() > 0) {
                String firstCell = rows.get(0)[0].trim().toLowerCase();
                if (firstCell.equals("number") || firstCell.equals("phone") || firstCell.equals("phone_number")) {
                    startIdx = 1;
                }
            }

            for (int i = startIdx; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length == 0 || row[0].trim().isEmpty()) continue;

                String rawNumber = row[0].trim();
                String name = row.length > 1 ? row[1].trim() : null;
                String group = row.length > 2 ? row[2].trim() : defaultGroup;

                try {
                    String normalized = normalizeNumber(rawNumber);
                    if (repository.existsByNumber(normalized)) {
                        skipped.add(rawNumber + " (already exists)");
                        continue;
                    }

                    PhoneNumber phone = PhoneNumber.builder()
                            .number(normalized)
                            .name(name)
                            .group(group != null && !group.isEmpty() ? group : defaultGroup)
                            .active(true)
                            .build();
                    added.add(repository.save(phone));

                } catch (Exception e) {
                    errors.add(rawNumber + " (" + e.getMessage() + ")");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage(), e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("added", added.size());
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    public List<PhoneNumber> getActiveNumbers() {
        return repository.findByActiveTrue();
    }

    public List<PhoneNumber> getActiveNumbersByGroup(String group) {
        return repository.findByGroupAndActiveTrue(group);
    }

    public List<PhoneNumber> getNumbersByIds(List<Long> ids) {
        return repository.findAllById(ids);
    }

    public long countActive() {
        return repository.countByActiveTrue();
    }

    /**
     * Normalize phone number to E.164 format for Bangladesh (+880XXXXXXXXXX).
     */
    private String normalizeNumber(String number) {
        // Remove spaces, dashes, parentheses
        String cleaned = number.replaceAll("[\\s\\-\\(\\)]", "");

        // Handle various formats
        if (cleaned.startsWith("0")) {
            // Local format: 01XXXXXXXXX -> +88001XXXXXXXXX
            cleaned = "+880" + cleaned.substring(1);
        } else if (cleaned.startsWith("880") && !cleaned.startsWith("+")) {
            cleaned = "+" + cleaned;
        } else if (!cleaned.startsWith("+")) {
            cleaned = "+880" + cleaned;
        }

        if (!BD_PHONE_PATTERN.matcher(cleaned).matches()) {
            throw new RuntimeException("Invalid Bangladesh phone number format: " + number);
        }

        return cleaned;
    }
}
