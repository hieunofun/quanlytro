package com.hostel.controller;
import com.hostel.security.Access;
import com.hostel.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static com.hostel.service.Database.*;
@RestController
@RequestMapping("/api")
public class ImageController {
    private final Database db;private final Access access;private final Audit audit;private final Path directory;
    public ImageController(Database db,Access access,Audit audit,@Value("${app.upload-directory:.runtime/uploads}") String directory) {
        this.db=db;this.access=access;this.audit=audit;this.directory=Path.of(directory).toAbsolutePath().normalize();
    }
    @PostMapping(value="/maintenance-requests/{id}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional public Object repairImage(@PathVariable long id,@RequestParam MultipartFile file)throws IOException {
        var m=access.maintenance(id);db.one("select id from maintenance_requests where id=? for update",id);
        require(!"COMPLETED".equals(m.get("status")),"Request already completed");
        require(db.jdbc.queryForObject("select count(*) from maintenance_images where maintenance_request_id=?",Long.class,id)<12,"Maximum 12 images");
        return store(file,id);
    }
    private Map<String,Object> store(MultipartFile file,long repairId) throws IOException {
        require(!file.isEmpty() && file.getSize()<=6*1024*1024,"Ảnh phải nhỏ hơn hoặc bằng 6 MB");
        byte[] bytes=file.getBytes();String format;
        try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReaders(stream);require(readers.hasNext(),"Chỉ nhận ảnh JPEG hoặc PNG hợp lệ");
            var reader=readers.next();
            try {
                format=reader.getFormatName().toLowerCase(Locale.ROOT);
                require(List.of("jpeg","png").contains(format),"Chỉ nhận ảnh JPEG hoặc PNG");
                reader.setInput(stream,true,true);
                require((long)reader.getWidth(0)*reader.getHeight(0)<=20_000_000,"Ảnh có độ phân giải quá lớn");
                require(reader.read(0)!=null,"Ảnh không đọc được");
            } finally {reader.dispose();}
        } catch(javax.imageio.IIOException e) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ảnh không đọc được");}
        UUID id=UUID.randomUUID();String extension=format.equals("jpeg")?"jpg":"png",type="image/"+format;
        String original=Optional.ofNullable(file.getOriginalFilename()).orElse("image");
        original=original.replace('\\','/');original=original.substring(original.lastIndexOf('/')+1);
        if(original.length()>255) original=original.substring(original.length()-255);
        Files.createDirectories(directory);Path target=directory.resolve(id+"."+extension);
        Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {if(status!=STATUS_COMMITTED) try {Files.deleteIfExists(target);}catch(IOException ignored) {}}
        });
        String url="/api/uploads/"+id+"."+extension;
        long image=db.insert("maintenance_images","maintenance_request_id,url,uploaded_by",repairId,url,access.userId());
        return Map.of("id",image,"url",url,"name",original);
    }
    @GetMapping("/uploads/{file}") public ResponseEntity<ByteArrayResource> image(@PathVariable String file)throws IOException {
        require(file.matches("[a-f0-9-]{36}\\.(jpg|png)"),"Invalid image name");
        var i=db.one("select * from maintenance_images where url=?","/api/uploads/"+file);
        access.maintenance(id(i,"maintenance_request_id"));Path target=directory.resolve(file).normalize();
        if(!target.getParent().equals(directory)||!Files.isRegularFile(target))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Image file not found");
        return ResponseEntity.ok().contentType(file.endsWith(".png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(new ByteArrayResource(Files.readAllBytes(target)));
    }
}
