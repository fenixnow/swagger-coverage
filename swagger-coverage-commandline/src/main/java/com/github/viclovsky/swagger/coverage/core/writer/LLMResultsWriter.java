package com.github.viclovsky.swagger.coverage.core.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.viclovsky.swagger.coverage.SwaggerCoverageWriteException;
import com.github.viclovsky.swagger.coverage.configuration.options.ResultsWriterOptions;
import com.github.viclovsky.swagger.coverage.core.model.Condition;
import com.github.viclovsky.swagger.coverage.core.model.OperationKey;
import com.github.viclovsky.swagger.coverage.core.results.Results;
import com.github.viclovsky.swagger.coverage.core.results.data.OperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LLMResultsWriter implements CoverageResultsWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LLMResultsWriter.class);

    private static final Pattern PARAM_PATTERN = Pattern.compile("(query|header|path|cookie) «(.+?)» is not empty");
    private static final Pattern STATUS_PATTERN = Pattern.compile("HTTP status (\\d+)");

    private final ObjectMapper mapper;
    private final String filename;

    public LLMResultsWriter() {
        this("swagger-coverage-llm-report.json");
    }

    public LLMResultsWriter(ResultsWriterOptions options) {
        this(options != null && options.getFilename() != null ? options.getFilename() : "swagger-coverage-llm-report.json");
    }

    public LLMResultsWriter(String filename) {
        this.filename = filename;
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    }

    @Override
    public void write(Results results) {
        Path path = Paths.get(filename);
        LOGGER.info(String.format("Write LLM report in file '%s'", path.toAbsolutePath()));

        try (OutputStream os = Files.newOutputStream(path)) {
            Map<String, Object> report = buildReport(results);
            mapper.writeValue(os, report);
        } catch (IOException e) {
            throw new SwaggerCoverageWriteException("Could not write LLM results", e);
        }
    }

    private Map<String, Object> buildReport(Results results) {
        Map<String, Object> report = new LinkedHashMap<>();

        // API info
        if (results.getInfo() != null) {
            String title = results.getInfo().getTitle();
            String version = results.getInfo().getVersion();
            report.put("api", title != null ? title + (version != null ? " v" + version : "") : "API");
        }

        // Generated at
        if (results.getGenerationStatistics() != null) {
            report.put("generated_at", results.getGenerationStatistics().getGenerateDate());
        }

        // Summary
        report.put("summary", buildSummary(results));

        // Paths (operations grouped by path)
        report.put("paths", buildPaths(results));

        return report;
    }

    private Map<String, Object> buildSummary(Results results) {
        Map<String, Object> summary = new LinkedHashMap<>();

        int totalOps = results.getOperations().size();
        int fullCovered = 0;
        int partyCovered = 0;
        int empty = 0;
        int deprecated = 0;
        int totalConditions = 0;
        int coveredConditions = 0;

        for (Map.Entry<OperationKey, OperationResult> entry : results.getOperations().entrySet()) {
            OperationResult op = entry.getValue();
            switch (op.getState()) {
                case FULL:
                    fullCovered++;
                    break;
                case PARTY:
                    partyCovered++;
                    break;
                case EMPTY:
                    empty++;
                    break;
                case DEPRECATED:
                    deprecated++;
                    break;
            }
            totalConditions += op.getAllConditionCount();
            coveredConditions += op.getCoveredConditionCount();
        }

        empty += results.getZeroCall().size();

        summary.put("total_operations", totalOps);
        summary.put("fully_covered", fullCovered);
        summary.put("partially_covered", partyCovered);
        summary.put("not_covered", empty);
        summary.put("deprecated", deprecated);
        summary.put("total_conditions", totalConditions);
        summary.put("covered_conditions", coveredConditions);
        summary.put("coverage_percent", totalConditions > 0 ?
                Math.round(coveredConditions * 1000.0 / totalConditions) / 10.0 : 0.0);

        return summary;
    }

    private Map<String, Map<String, Object>> buildPaths(Results results) {
        Map<String, Map<String, Object>> paths = new LinkedHashMap<>();

        for (Map.Entry<OperationKey, OperationResult> entry : results.getOperations().entrySet()) {
            OperationKey key = entry.getKey();
            OperationResult op = entry.getValue();

            String path = key.getPath();
            String method = key.getHttpMethod().toString().toUpperCase();

            Map<String, Object> pathEntry = paths.computeIfAbsent(path, k -> new LinkedHashMap<>());
            pathEntry.put(method, buildOperationEntry(op));
        }

        // Add zeroCall operations
        for (OperationKey key : results.getZeroCall()) {
            String path = key.getPath();
            String method = key.getHttpMethod().toString().toUpperCase();

            if (!paths.containsKey(path)) {
                Map<String, Object> pathEntry = new LinkedHashMap<>();
                pathEntry.put(method, buildEmptyOperationEntry());
                paths.put(path, pathEntry);
            } else if (!paths.get(path).containsKey(method)) {
                paths.get(path).put(method, buildEmptyOperationEntry());
            }
        }

        return paths;
    }

    private Map<String, Object> buildOperationEntry(OperationResult op) {
        Map<String, Object> entry = new LinkedHashMap<>();

        entry.put("state", op.getState().name());
        entry.put("coverage", op.getCoveredConditionCount() + "/" + op.getAllConditionCount());
        entry.put("deprecated", op.getDeprecated());

        // Requirements
        entry.put("requirements", buildRequirements(op.getConditions()));

        return entry;
    }

    private Map<String, Object> buildEmptyOperationEntry() {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("state", "EMPTY");
        entry.put("coverage", "0/0");
        entry.put("deprecated", false);
        entry.put("requirements", new HashMap<>());
        return entry;
    }

    private Map<String, List<Map<String, Object>>> buildRequirements(List<Condition> conditions) {
        Map<String, List<Map<String, Object>>> requirements = new LinkedHashMap<>();
        List<Map<String, Object>> statusCodes = new ArrayList<>();
        List<Map<String, Object>> parameters = new ArrayList<>();
        List<Map<String, Object>> body = new ArrayList<>();
        List<Map<String, Object>> properties = new ArrayList<>();

        for (Condition condition : conditions) {
            String type = condition.getType();
            Map<String, Object> item = new HashMap<>();
            item.put("covered", condition.isCovered());
            item.put("description", condition.getDescription() != null ? condition.getDescription() : "");

            if ("DefaultStatusConditionPredicate".equals(type)) {
                Matcher m = STATUS_PATTERN.matcher(condition.getName());
                if (m.find()) {
                    item.put("code", m.group(1));
                    statusCodes.add(item);
                }
            } else if ("DefaultParameterConditionPredicate".equals(type)) {
                Matcher m = PARAM_PATTERN.matcher(condition.getName());
                if (m.find()) {
                    item.put("in", m.group(1));
                    item.put("name", m.group(2));
                    parameters.add(item);
                }
            } else if ("DefaultBodyConditionPredicate".equals(type)) {
                body.add(item);
            } else if ("DefaultPropertyConditionPredicate".equals(type)) {
                // Extract property name from "«name» is not empty"
                String name = condition.getName();
                if (name.startsWith("«") && name.contains("»")) {
                    int start = name.indexOf("«") + 1;
                    int end = name.indexOf("»");
                    if (end > start) {
                        item.put("name", name.substring(start, end));
                        properties.add(item);
                    }
                }
            }
            // Skip FullStatusConditionPredicate - technical condition
        }

        requirements.put("status_codes", statusCodes);
        requirements.put("parameters", parameters);
        requirements.put("body", body);
        requirements.put("properties", properties);

        return requirements;
    }
}