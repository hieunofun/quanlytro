package com.hostel;

import com.hostel.service.Database;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.flyway.target=13","spring.datasource.url=${DB49_URL:jdbc:postgresql://127.0.0.1:55449/hostel_test49}","app.upload-directory=.runtime/implementation-49/http-uploads"})
@ActiveProfiles({"test","model49"})
class Model49HttpTests {
    @Autowired Database db;@Autowired PasswordEncoder encoder;@Value("${local.server.port}")int port;
    final ObjectMapper json=new ObjectMapper();final HttpClient client=HttpClient.newHttpClient();String token,username;long user;
    @BeforeEach void seed()throws Exception{username="http49-"+UUID.randomUUID().toString().substring(0,10);user=db.insert("users","username,password_hash,role",username,encoder.encode("TestPassword#2026"),"ADMIN");token=data(send("/auth/login","POST",Map.of("username",username,"password","TestPassword#2026"),null)).get("accessToken").toString();}
    HttpResponse<String> send(String path,String method,Object body,String auth)throws Exception{var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api"+path)).header("Content-Type","application/json");if(auth!=null)b.header("Authorization","Bearer "+auth);return client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
    @SuppressWarnings("unchecked")Map<String,Object> data(HttpResponse<String> r){assertEquals(200,r.statusCode(),r.body());return json.readValue(r.body(),Map.class);}
    @Test void healthAndCatalogUse49SchemaWithoutExposingCredentials()throws Exception{
        assertEquals(49,((Number)data(send("/health","GET",null,null)).get("schemaVersion")).intValue());
        var users=send("/users","GET",null,token);assertEquals(200,users.statusCode());assertFalse(users.body().contains("password_hash"));assertEquals(401,send("/fund-accounts","GET",null,null).statusCode());
    }
    @Test void resetCodeIsHashedSingleUseAndRevokesExistingSession()throws Exception{
        var issued=data(send("/users/"+user+"/reset-code","POST",Map.of("reason","Identity verified in person"),token));String code=issued.get("code").toString();long reset=((Number)issued.get("resetId")).longValue();
        assertNotEquals(code,db.one("select token_hash from password_reset_tokens where id=?",reset).get("token_hash"));
        assertEquals(400,send("/auth/reset-password","POST",Map.of("resetId",reset,"code","wrong","newPassword","ChangedPassword#2026"),null).statusCode());assertEquals(1,db.jdbc.queryForObject("select attempts from password_reset_tokens where id=?",Integer.class,reset));
        data(send("/auth/reset-password","POST",Map.of("resetId",reset,"code",code,"newPassword","ChangedPassword#2026"),null));
        assertEquals(401,send("/auth/me","GET",null,token).statusCode());assertEquals(400,send("/auth/reset-password","POST",Map.of("resetId",reset,"code",code,"newPassword","ChangedPassword#2026"),null).statusCode());
        assertEquals(200,send("/auth/login","POST",Map.of("username",username,"password","ChangedPassword#2026"),null).statusCode());
    }
    @Test void resetStopsAfterFiveWrongAttempts()throws Exception{
        var issued=data(send("/users/"+user+"/reset-code","POST",Map.of("reason","Verified reset"),token));
        for(int i=0;i<5;i++)assertEquals(400,send("/auth/reset-password","POST",Map.of("resetId",issued.get("resetId"),"code","wrong","newPassword","ChangedPassword#2026"),null).statusCode());
        assertEquals(400,send("/auth/reset-password","POST",Map.of("resetId",issued.get("resetId"),"code",issued.get("code"),"newPassword","ChangedPassword#2026"),null).statusCode());
    }
    @Test void emptyRoomRepairSupportsPrivateByteUploadAndDownload()throws Exception{
        var p=data(send("/properties","POST",Map.of("code","H-"+username.substring(7),"name","Test House","address","Local"),token));
        var r=data(send("/rooms","POST",Map.of("propertyId",p.get("id"),"code","101","maxOccupants",2,"price",1000),token));
        var repair=data(send("/maintenance-requests","POST",Map.of("roomId",r.get("id"),"title","Empty room repair","description","No lease required","priority","NORMAL"),token));
        var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes);String boundary="Evidence49";
        var multipart=new ByteArrayOutputStream();multipart.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"image.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));multipart.write(bytes.toByteArray());multipart.write(("\r\n--"+boundary+"--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/maintenance-requests/"+repair.get("id")+"/images")).header("Authorization","Bearer "+token).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(multipart.toByteArray())).build();
        var uploaded=data(client.send(request,HttpResponse.BodyHandlers.ofString()));
        var download=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+uploaded.get("url"))).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(200,download.statusCode());assertArrayEquals(bytes.toByteArray(),download.body());
        assertEquals(401,send(uploaded.get("url").toString().substring(4),"GET",null,null).statusCode());
    }
}
