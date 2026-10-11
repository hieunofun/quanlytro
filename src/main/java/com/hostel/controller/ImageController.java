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
    @PostMapping(value="/rooms/{id}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional public Object roomImage(@PathVariable long id,@RequestParam MultipartFile file) throws IOException {
        access.manager();access.room(id);db.one("select id from rooms where id=? for update",id);
        require(db.jdbc.queryForObject("select count(*) from room_images where room_id=?",Long.class,id)<12,"Tối đa 12 ảnh mỗi phòng");
        var image=store(file,id,null);db.insert("room_images","room_id,url",id,image.get("url"));return image;
    }
    @PostMapping(value="/maintenance-requests/{id}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional public Object repairImage(@PathVariable long id,@RequestParam MultipartFile file) throws IOException {
        access.maintenance(id);var m=db.one("select * from maintenance_requests where id=? for update",id);
        require(!"COMPLETED".equals(m.get("status")),"Sự cố đã hoàn thành");
        var urls=new ArrayList<>(Arrays.asList(m.get("image_urls").toString().split("\n")));urls.removeIf(String::isBlank);
        require(urls.size()<12,"Tối đa 12 ảnh mỗi sự cố");
        var image=store(file,null,id);urls.add(image.get("url").toString());
        db.jdbc.update("update maintenance_requests set image_urls=? where id=?",String.join("\n",urls),id);return image;
    }
    @DeleteMapping("/rooms/{id}/images/{imageId}") @Transactional public Object remove(@PathVariable long id,@PathVariable long imageId) {
        access.manager();access.room(id);var before=db.one("select * from room_images where id=? and room_id=?",imageId,id);
        db.jdbc.update("delete from room_images where id=?",imageId);audit.log("REMOVE_IMAGE","rooms",id,before,null);return Map.of("id",imageId);
    }
    private Map<String,Object> store(MultipartFile file,Long roomId,Long repairId) throws IOException {
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
        db.jdbc.update("insert into uploaded_images(id,room_id,maintenance_request_id,original_name,content_type,extension,size_bytes,uploaded_by) values(?,?,?,?,?,?,?,?)",id,roomId,repairId,original,type,extension,bytes.length,access.userId());
        audit.log("UPLOAD_IMAGE",roomId==null?"maintenance_requests":"rooms",roomId==null?repairId:roomId,null,id);
        return Map.of("id",id,"url","/api/uploads/"+id,"name",original);
    }
    @GetMapping("/uploads/{id}") public ResponseEntity<ByteArrayResource> image(@PathVariable UUID id) throws IOException {
        var i=db.one("select * from uploaded_images where id=?",id);
        if(i.get("room_id")!=null) {
            access.room(id(i,"room_id"));
            require(db.jdbc.queryForObject("select count(*) from room_images where room_id=? and url=?",Long.class,i.get("room_id"),"/api/uploads/"+id)>0,"Ảnh đã được gỡ khỏi phòng");
        } else access.maintenance(id(i,"maintenance_request_id"));
        Path path=directory.resolve(id+"."+i.get("extension")).normalize();
        if(!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy tệp ảnh");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(i.get("content_type").toString()))
            .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
            .body(new ByteArrayResource(Files.readAllBytes(path)));
    }
}
