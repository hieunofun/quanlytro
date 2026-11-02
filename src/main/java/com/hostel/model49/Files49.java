package com.hostel.model49;

import com.hostel.service.Database;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import static com.hostel.service.Database.*;

@RestController
@RequestMapping("/api")
@Profile("model49")
public class Files49 {
    private final Database db;
    private final Access49 access;
    private final Catalog49 catalog;
    private final Path directory;
    private static final Map<String, String> TARGETS =
            Map.ofEntries(
                    Map.entry("room", "room_id"),
                    Map.entry("asset", "asset_id"),
                    Map.entry("handover-item", "handover_item_id"),
                    Map.entry("meter-reading", "meter_reading_id"),
                    Map.entry("maintenance-request", "maintenance_request_id"),
                    Map.entry("maintenance-cost", "maintenance_cost_id"),
                    Map.entry("contract-term", "contract_term_id"),
                    Map.entry("billing-document", "billing_document_id"),
                    Map.entry("violation", "violation_id"),
                    Map.entry("fund-entry", "fund_entry_id"));

    public Files49(
            Database db,
            Access49 access,
            Catalog49 catalog,
            @Value("${app.upload-directory:.runtime/uploads}") String directory) {
        this.db = db;
        this.access = access;
        this.catalog = catalog;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    private void scope(String kind, long target) {
        require(TARGETS.containsKey(kind), "Invalid file target");
        switch (kind) {
            case "room" -> access.room(target);
            case "asset" -> catalog.get("assets", target);
            case "maintenance-request" -> access.maintenance(target);
            case "billing-document" -> access.document(target);
            case "meter-reading" -> {
                var r =
                        db.one(
                                "select m.room_id from meter_readings r join meters m on"
                                        + " m.id=r.meter_id where r.id=?",
                                target);
                access.room(id(r, "room_id"));
            }
            case "maintenance-cost" -> {
                access.manager();
                access.maintenance(
                        id(
                                db.one(
                                        "select request_id from maintenance_costs where id=?",
                                        target),
                                "request_id"));
            }
            case "contract-term" ->
                    access.contract(
                            id(
                                    db.one(
                                            "select contract_id from contract_terms where id=?",
                                            target),
                                    "contract_id"));
            case "violation" ->
                    access.contract(
                            id(
                                    db.one(
                                            "select contract_id from contract_violations where"
                                                    + " id=?",
                                            target),
                                    "contract_id"));
            case "handover-item" ->
                    access.contract(
                            id(
                                    db.one(
                                            "select h.contract_id from handover_items i join"
                                                    + " handovers h on h.id=i.handover_id where i.id=?",
                                            target),
                                    "contract_id"));
            case "fund-entry" -> {
                access.manager();
                var e = db.one("select * from fund_entries where id=?", target);
                if (e.get("from_fund_id") != null) {
                    access.fund(id(e, "from_fund_id"));
                } else if (e.get("to_fund_id") != null) {
                    access.fund(id(e, "to_fund_id"));
                } else {
                    var o =
                            db.one(
                                    "select * from fund_entries where id=?",
                                    e.get("original_entry_id"));
                    if (o.get("from_fund_id") != null) {
                        access.fund(id(o, "from_fund_id"));
                    }
                    if (o.get("to_fund_id") != null) {
                        access.fund(id(o, "to_fund_id"));
                    }
                }
            }
            default -> throw new IllegalArgumentException("Invalid target");
        }
    }

    @GetMapping("/files/{kind}/{target}")
    public Object list(@PathVariable String kind, @PathVariable long target) {
        scope(kind, target);
        return db.rows(
                ("select a.id,f.original_name,f.content_type,f.size_bytes,f.sha256,"
                                + "coalesce(f.external_url,'/api/uploads/'||f.storage_key) url,"
                                + "a.caption from file_attachments a join stored_files f on "
                                + "f.id=a.file_id where a.")
                        + TARGETS.get(kind)
                        + "=? order by a.id",
                target);
    }

    @PostMapping(value = "/files/{kind}/{target}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Object attach(
            @PathVariable String kind, @PathVariable long target, @RequestParam MultipartFile file)
            throws Exception {
        scope(kind, target);
        if (!kind.equals("maintenance-request")) {
            access.manager();
        }
        if (kind.equals("maintenance-request")) {
            db.one("select id from maintenance_requests where id=? for update", target);
            require(
                    db.jdbc.queryForObject(
                                    "select count(*) from file_attachments where"
                                            + " maintenance_request_id=?",
                                    Integer.class,
                                    target)
                            < 12,
                    "Maximum 12 evidence files");
        }
        require(
                !file.isEmpty() && file.getSize() <= 6 * 1024 * 1024,
                "Tệp phải từ 1 byte đến 6 MB");
        byte[] bytes = file.getBytes();
        String extension, type;
        if (bytes.length >= 5
                && new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)
                        .equals("%PDF-")) {
            require(!kind.equals("maintenance-request"), "Ảnh sự cố phải là JPEG hoặc PNG");
            extension = "pdf";
            type = "application/pdf";
        } else {
            try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(stream);
                require(readers.hasNext(), "Chỉ nhận JPEG, PNG hoặc PDF");
                var reader = readers.next();
                try {
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    require(List.of("jpeg", "png").contains(format), "Chỉ nhận JPEG hoặc PNG");
                    reader.setInput(stream, true, true);
                    require(
                            (long) reader.getWidth(0) * reader.getHeight(0) <= 20_000_000,
                            "Ảnh quá lớn");
                    require(reader.read(0) != null, "Ảnh hỏng");
                    extension = format.equals("jpeg") ? "jpg" : "png";
                    type = "image/" + format;
                } finally {
                    reader.dispose();
                }
            }
        }
        String key = UUID.randomUUID() + "." + extension,
                original = Objects.toString(file.getOriginalFilename(), "file").replace('\\', '/');
        original = original.substring(original.lastIndexOf('/') + 1);
        if (original.length() > 255) {
            original = original.substring(original.length() - 255);
        }
        Files.createDirectories(directory);
        Path output = directory.resolve(key);
        Files.write(output, bytes, StandardOpenOption.CREATE_NEW);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            try {
                                Files.deleteIfExists(output);
                            } catch (IOException ignored) {
                            }
                        }
                    }
                });
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        long f =
                db.insert(
                        "stored_files",
                        "storage_key,original_name,content_type,size_bytes,sha256,uploaded_by",
                        key,
                        original,
                        type,
                        bytes.length,
                        hash,
                        access.user());
        long link =
                db.insert(
                        "file_attachments",
                        "file_id," + TARGETS.get(kind) + ",attached_by",
                        f,
                        target,
                        access.user());
        return Map.of("id", link, "url", "/api/uploads/" + key, "name", original, "sha256", hash);
    }

    @PostMapping(
            value = "/maintenance-requests/{id}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Object repair(@PathVariable long id, @RequestParam MultipartFile file) throws Exception {
        return attach("maintenance-request", id, file);
    }

    @GetMapping("/uploads/{key}")
    public ResponseEntity<ByteArrayResource> download(@PathVariable String key) throws IOException {
        require(key.matches("[a-f0-9-]{36}\\.(jpg|png|pdf)"), "Invalid file name");
        var links =
                db.rows(
                        "select a.*,f.content_type,f.sha256,f.external_url from file_attachments a"
                                + " join stored_files f on f.id=a.file_id where f.storage_key=? or"
                                + " f.external_url=?",
                        key,
                        "/api/uploads/" + key);
        require(!links.isEmpty(), "File metadata missing");
        var link = links.getFirst();
        for (var e : TARGETS.entrySet()) {
            if (link.get(e.getValue()) != null) {
                scope(e.getKey(), id(link, e.getValue()));
            }
        }
        Path target = directory.resolve(key).normalize();
        require(
                target.getParent().equals(directory) && Files.isRegularFile(target),
                "File unavailable; restore the matching upload backup");
        var response =
                ResponseEntity.ok()
                        .contentType(
                                key.endsWith(".pdf")
                                        ? MediaType.APPLICATION_PDF
                                        : key.endsWith(".png")
                                                ? MediaType.IMAGE_PNG
                                                : MediaType.IMAGE_JPEG)
                        .cacheControl(CacheControl.noStore())
                        .header("X-Content-Type-Options", "nosniff");
        if (key.endsWith(".pdf")) {
            response.header("Content-Disposition", "attachment; filename=evidence.pdf");
        }
        return response.body(new ByteArrayResource(Files.readAllBytes(target)));
    }
}
