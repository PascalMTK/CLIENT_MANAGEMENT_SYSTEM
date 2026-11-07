package com.clientfiles.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** What may be submitted. The list holds only formats that are safe to open on a reviewer's PC. */
public final class Uploads {
    public static final long MAX_BYTES = 25L * 1024 * 1024;
    public static final List<String> CATEGORIES =
            List.of("Report", "Invoice", "Contract", "Identity", "Certificate", "Other");
    private static final Set<String> ALLOWED = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "txt", "png", "jpg", "jpeg");

    private Uploads() {
    }

    public static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean isAllowedType(String fileName) {
        return ALLOWED.contains(extension(fileName));
    }

    /** Returns the message to display, or null when the file can be submitted. */
    public static String problemWith(Path file) {
        try {
            String name = file.getFileName().toString();
            if (!Files.isRegularFile(file)) {
                return "This file could not be found.";
            }
            if (!isAllowedType(name)) {
                return "This file type isn't accepted. Use PDF, Word, Excel, PowerPoint, CSV, TXT, PNG or JPG.";
            }
            if (name.length() > 255) {
                return "This file name is too long. Rename the file and try again.";
            }
            long size = Files.size(file);
            if (size == 0) {
                return "This file is empty.";
            }
            return size > MAX_BYTES ? "This file is larger than 25 MB." : null;
        } catch (IOException e) {
            return "This file could not be read.";
        }
    }
}
