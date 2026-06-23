package com.AssociateSoftwareEngineer.AssociateSoftware.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility for building Metadata API deployment packages.
 * Creates in-memory zip files containing package.xml and validation rule metadata.
 */
public class MetadataDeployUtil {

    private MetadataDeployUtil() {
        // Utility class — no instantiation
    }

    /**
     * Build a zip deployment package for toggling validation rules.
     *
     * @param rules List of maps with keys: fullName, active, description, errorMessage, formula, errorDisplayField
     * @return byte array of the zip file
     */
    public static byte[] buildDeploymentZip(List<Map<String, Object>> rules) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add package.xml
            String packageXml = buildPackageXml(rules);
            addToZip(zos, "package.xml", packageXml);

            // Group by objectName
            Map<String, List<Map<String, Object>>> rulesByObject = new java.util.HashMap<>();
            for (Map<String, Object> rule : rules) {
                String fullName = (String) rule.get("fullName");
                String objectName = fullName.contains(".") ? fullName.split("\\.")[0] : "Account";
                rulesByObject.computeIfAbsent(objectName, k -> new java.util.ArrayList<>()).add(rule);
            }

            // Add each CustomObject XML
            for (Map.Entry<String, List<Map<String, Object>>> entry : rulesByObject.entrySet()) {
                String objectName = entry.getKey();
                List<Map<String, Object>> objectRules = entry.getValue();

                StringBuilder objectXml = new StringBuilder();
                objectXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                        .append("<CustomObject xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n");

                for (Map<String, Object> rule : objectRules) {
                    String fullName = (String) rule.get("fullName");
                    String ruleName = fullName.contains(".") ? fullName.split("\\.")[1] : fullName;

                    objectXml.append("  <validationRules>\n")
                            .append("    <fullName>").append(ruleName).append("</fullName>\n")
                            .append("    <active>").append(rule.get("active")).append("</active>\n");

                    String desc = getStringValue(rule, "description", "");
                    if (!desc.isEmpty() && !"null".equals(desc)) {
                        objectXml.append("    <description>").append(escapeXml(desc)).append("</description>\n");
                    }

                    objectXml.append("    <errorMessage>").append(escapeXml(getStringValue(rule, "errorMessage", "Validation error"))).append("</errorMessage>\n");

                    String displayField = getStringValue(rule, "errorDisplayField", "");
                    if (!displayField.isEmpty() && !"null".equals(displayField)) {
                        objectXml.append("    <errorDisplayField>").append(displayField).append("</errorDisplayField>\n");
                    }

                    objectXml.append("    <errorConditionFormula>").append(escapeXml(getStringValue(rule, "formula", "false"))).append("</errorConditionFormula>\n")
                            .append("  </validationRules>\n");
                }

                objectXml.append("</CustomObject>");

                String path = "objects/" + objectName + ".object";
                addToZip(zos, path, objectXml.toString());
            }
        }

        return baos.toByteArray();
    }

    /**
     * Build a simple deployment zip for a single rule toggle.
     */
    public static byte[] buildSingleRuleDeployZip(String objectName, String ruleName,
                                                   boolean active, String description,
                                                   String errorMessage, String formula,
                                                   String errorDisplayField) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // package.xml
            String packageXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n"
                    + "  <types>\n"
                    + "    <members>" + objectName + "." + ruleName + "</members>\n"
                    + "    <name>ValidationRule</name>\n"
                    + "  </types>\n"
                    + "  <version>59.0</version>\n"
                    + "</Package>";
            addToZip(zos, "package.xml", packageXml);

            // Validation rule file
            StringBuilder ruleXml = new StringBuilder();
            ruleXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                    .append("<CustomObject xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n")
                    .append("  <validationRules>\n")
                    .append("    <fullName>").append(ruleName).append("</fullName>\n")
                    .append("    <active>").append(active).append("</active>\n");

            if (description != null && !description.isEmpty() && !"null".equals(description)) {
                ruleXml.append("    <description>").append(escapeXml(description)).append("</description>\n");
            }

            ruleXml.append("    <errorMessage>").append(escapeXml(errorMessage != null ? errorMessage : "Validation error")).append("</errorMessage>\n");

            if (errorDisplayField != null && !errorDisplayField.isEmpty() && !"null".equals(errorDisplayField)) {
                ruleXml.append("    <errorDisplayField>").append(errorDisplayField).append("</errorDisplayField>\n");
            }

            ruleXml.append("    <errorConditionFormula>").append(escapeXml(formula != null ? formula : "false")).append("</errorConditionFormula>\n")
                    .append("  </validationRules>\n")
                    .append("</CustomObject>");

            String path = "objects/" + objectName + ".object";
            addToZip(zos, path, ruleXml.toString());
        }

        return baos.toByteArray();
    }

    private static String buildPackageXml(List<Map<String, Object>> rules) {
        StringBuilder members = new StringBuilder();
        for (Map<String, Object> rule : rules) {
            String fullName = (String) rule.get("fullName");
            members.append("    <members>").append(fullName).append("</members>\n");
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n"
                + "  <types>\n"
                + members
                + "    <name>ValidationRule</name>\n"
                + "  </types>\n"
                + "  <version>59.0</version>\n"
                + "</Package>";
    }

    private static String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return String.valueOf(value);
    }


    private static void addToZip(ZipOutputStream zos, String filename, String content) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    /**
     * Escape special XML characters.
     */
    private static String escapeXml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
