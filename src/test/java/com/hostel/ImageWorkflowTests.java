package com.hostel;
import com.hostel.service.Database;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="app.upload-directory=.runtime/test-uploads")
@ActiveProfiles("test")
class ImageWorkflowTests {
    @Autowired Database db;@Autowired PasswordEncoder encoder;@Autowired DataSource dataSource;@Value("${local.server.port}") int port;
    HttpClient client=HttpClient.newHttpClient();long admin,staff,tenant,profile,property,floor,room,sibling,contract,repair;String ownerToken,staffToken,tenantToken;
    long user(String role,String suffix) {
        return db.insert("users","username,password_hash,full_name,phone,role",suffix,encoder.encode("UploadTest#2026"),role,UUID.randomUUID().toString().substring(0,18),role);
    }
    String token(String username) throws Exception {
        var r=client.send(HttpRequest.newBuilder(URI.create(base()+"/auth/login")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"username\":\""+username+"\",\"password\":\"UploadTest#2026\"}")).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,r.statusCode());var m=java.util.regex.Pattern.compile("\"accessToken\":\"([^\"]+)\"").matcher(r.body());assertTrue(m.find());return m.group(1);
    }
    String base() {return "http://127.0.0.1:"+port+"/api";}
    @BeforeEach void seed() throws Exception {
        String key=UUID.randomUUID().toString();admin=user("ADMIN","ua"+key);staff=user("STAFF","us"+key);tenant=user("TENANT","ut"+key);
        property=db.insert("properties","property_code,name,address",key.substring(0,20),"Upload fixture","HCM");
        floor=db.insert("floors","property_id,floor_number,name",property,1,"Tầng 1");
        room=db.insert("rooms","property_id,floor_id,room_code,base_price,max_occupants,status",property,floor,"301",3000000,2,"RENTED");
        sibling=db.insert("rooms","property_id,floor_id,room_code,base_price,max_occupants",property,floor,"302",3000000,2);
        profile=db.insert("tenant_profiles","user_id,full_name,identity_number,phone",tenant,"Upload tenant",key.substring(0,15),"0900000000");
        contract=db.insert("contracts","contract_code,room_id,primary_tenant_id,start_date,end_date,rental_price,deposit_amount,payment_cycle_day,initial_electric_index,initial_water_index",key,room,profile,LocalDate.now().minusMonths(1),LocalDate.now().plusYears(1),3000000,3000000,5,0,0);
        repair=db.insert("maintenance_requests","contract_id,property_id,room_id,submitted_by,title,description,priority,expected_date",contract,property,room,tenant,"Hỏng khóa","Upload test","NORMAL",LocalDate.now().plusDays(3));
        ownerToken=token("ua"+key);staffToken=token("us"+key);tenantToken=token("ut"+key);
    }
    @AfterEach void clean() throws Exception {
        for(var row:db.rows("select id,extension from uploaded_images where room_id in (?,?) or maintenance_request_id=?",room,sibling,repair)) Files.deleteIfExists(Path.of(".runtime/test-uploads").resolve(row.get("id")+"."+row.get("extension")));
        db.jdbc.update("delete from audit_logs where user_id in (?,?,?)",admin,staff,tenant);
        db.jdbc.update("delete from uploaded_images where room_id in (?,?) or maintenance_request_id=?",room,sibling,repair);
        db.jdbc.update("delete from room_images where room_id in (?,?)",room,sibling);
        db.jdbc.update("delete from maintenance_requests where id=?",repair);db.jdbc.update("delete from room_employee_assignments where user_id=?",staff);
        db.jdbc.update("delete from contracts where id=?",contract);db.jdbc.update("delete from tenant_profiles where id=?",profile);
        db.jdbc.update("delete from rooms where id in (?,?)",room,sibling);db.jdbc.update("delete from floors where id=?",floor);db.jdbc.update("delete from properties where id=?",property);
        db.jdbc.update("delete from auth_sessions where user_id in (?,?,?)",admin,staff,tenant);db.jdbc.update("delete from users where id in (?,?,?)",admin,staff,tenant);
    }
    byte[] png() throws Exception {var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}
    HttpResponse<byte[]> upload(String endpoint,byte[] bytes,String token) throws Exception {
        String boundary="upload-fixture";var out=new ByteArrayOutputStream();
        out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"../../image.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));out.write(bytes);out.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        var builder=HttpRequest.newBuilder(URI.create(base()+endpoint)).timeout(java.time.Duration.ofSeconds(15)).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()));if(token!=null)builder.header("Authorization","Bearer "+token);
        return client.send(builder.build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    String url(HttpResponse<byte[]> response) {var m=java.util.regex.Pattern.compile("\"url\":\"([^\"]+)\"").matcher(new String(response.body(),StandardCharsets.UTF_8));assertTrue(m.find());return m.group(1);}
    HttpResponse<byte[]> read(String path,String token) throws Exception {var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));if(token!=null)b.header("Authorization","Bearer "+token);return client.send(b.GET().build(),HttpResponse.BodyHandlers.ofByteArray());}
    @Test void imageBytesArePrivateAndRoomAssignmentsApplyToUploadAndRead() throws Exception {
        byte[] bytes=png();var saved=upload("/rooms/"+room+"/images",bytes,ownerToken);assertEquals(200,saved.statusCode());String image=url(saved);
        assertEquals(401,read(image,null).statusCode());assertEquals(403,read(image,staffToken).statusCode());assertArrayEquals(bytes,read(image,tenantToken).body());
        db.insert("room_employee_assignments","user_id,room_id",staff,room);
        assertArrayEquals(bytes,read(image,staffToken).body());assertEquals(403,upload("/rooms/"+sibling+"/images",bytes,staffToken).statusCode());assertEquals(200,upload("/rooms/"+room+"/images",bytes,staffToken).statusCode());
    }
    @Test void tenantCanAttachPhotoToOwnRepairButCannotAttachRoomPhotos() throws Exception {
        var saved=upload("/maintenance-requests/"+repair+"/images",png(),tenantToken);assertEquals(200,saved.statusCode());assertEquals(200,read(url(saved),tenantToken).statusCode());assertEquals(403,read(url(saved),staffToken).statusCode());
        assertEquals(403,upload("/rooms/"+room+"/images",png(),tenantToken).statusCode());
    }
    @Test void disguisedFilesOversizeAndAnonymousUploadsAreRejected() throws Exception {
        assertEquals(400,upload("/rooms/"+room+"/images","<script>alert(1)</script>".getBytes(),ownerToken).statusCode());
        assertEquals(413,upload("/rooms/"+room+"/images",new byte[6*1024*1024+1],ownerToken).statusCode());
        assertEquals(401,upload("/rooms/"+room+"/images",png(),null).statusCode());
    }
    List<HttpResponse<byte[]>> simultaneousRepairUploads() throws Exception {
        byte[] bytes=png();
        try(var pool=Executors.newFixedThreadPool(2)) {
            Future<HttpResponse<byte[]>> first,second;
            try(var connection=dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try(var lock=connection.prepareStatement("select id from maintenance_requests where id=? for update")) {lock.setLong(1,repair);lock.executeQuery().close();}
                first=pool.submit(()->upload("/maintenance-requests/"+repair+"/images",bytes,tenantToken));
                second=pool.submit(()->upload("/maintenance-requests/"+repair+"/images",bytes,tenantToken));
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);int waiting=0;
                while(System.nanoTime()<deadline) {
                    waiting=db.jdbc.queryForObject("select count(*)::int from pg_stat_activity where datname=current_database() and wait_event_type='Lock' and query like 'select * from maintenance_requests%for update%'",Integer.class);
                    if(waiting>=2)break;Thread.sleep(25);
                }
                assertEquals(2,waiting,"Both uploads must wait on the fixture lock before release");connection.commit();
            }
            return List.of(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS));
        }
    }
    @Test void concurrentRepairUploadsRetainBothAttachments() throws Exception {
        var responses=simultaneousRepairUploads();assertTrue(responses.stream().allMatch(r->r.statusCode()==200));
        String urls=db.one("select image_urls from maintenance_requests where id=?",repair).get("image_urls").toString();
        assertEquals(2,urls.lines().count());for(var response:responses)assertTrue(urls.contains(url(response)));
        assertEquals(2,db.jdbc.queryForObject("select count(*)::int from uploaded_images where maintenance_request_id=?",Integer.class,repair));
    }
    @Test void concurrentRepairUploadsCannotExceedAttachmentLimit() throws Exception {
        db.jdbc.update("update maintenance_requests set image_urls=? where id=?",String.join("\n",java.util.stream.IntStream.range(0,11).mapToObj(i->"https://example.test/fixture-"+i+".png").toList()),repair);
        var responses=simultaneousRepairUploads();assertEquals(List.of(200,400),responses.stream().map(HttpResponse::statusCode).sorted().toList());
        assertEquals(12,db.one("select image_urls from maintenance_requests where id=?",repair).get("image_urls").toString().lines().count());
        assertEquals(1,db.jdbc.queryForObject("select count(*)::int from uploaded_images where maintenance_request_id=?",Integer.class,repair));
    }
}
