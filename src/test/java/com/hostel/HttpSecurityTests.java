package com.hostel;
import com.hostel.service.Database;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.net.*;
import java.net.http.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HttpSecurityTests {
    @Autowired Database db;@Autowired PasswordEncoder encoder;@Value("${local.server.port}") int port;
    HttpClient client=HttpClient.newHttpClient();long user;String username;
    @BeforeEach void seed() {username="http"+UUID.randomUUID();user=db.insert("users","username,password_hash,role",username,encoder.encode("TestPassword#2026"),"ADMIN");}
    @AfterEach void clean() {db.jdbc.update("delete from users where id=?",user);}
    HttpResponse<String> request(String path,String method,String body,String token) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api"+path)).header("Content-Type","application/json");
        if(token!=null)builder.header("Authorization","Bearer "+token);
        return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    String value(String json,String key) {var m=java.util.regex.Pattern.compile("\""+key+"\":\"([^\"]+)\"").matcher(json);assertTrue(m.find(),json);return m.group(1);}
    HttpResponse<String> login(String password) throws Exception {return request("/auth/login","POST","{\"username\":\""+username+"\",\"password\":\""+password+"\"}",null);}
    @Test void anonymousAndInvalidJwtCannotReadData() throws Exception {assertEquals(401,request("/rooms","GET",null,null).statusCode());assertEquals(401,request("/rooms","GET",null,"broken.jwt").statusCode());}
    @Test void refreshRotatesAndLogoutRevokesBothTokens() throws Exception {
        var login=login("TestPassword#2026");assertEquals(200,login.statusCode());String access=value(login.body(),"accessToken"),refresh=value(login.body(),"refreshToken");
        assertEquals(200,request("/auth/me","GET",null,access).statusCode());
        var renewed=request("/auth/refresh","POST","{\"refreshToken\":\""+refresh+"\"}",null);assertEquals(200,renewed.statusCode());
        assertEquals(401,request("/auth/refresh","POST","{\"refreshToken\":\""+refresh+"\"}",null).statusCode());
        String newAccess=value(renewed.body(),"accessToken"),newRefresh=value(renewed.body(),"refreshToken");
        assertEquals(200,request("/auth/logout","POST",null,newAccess).statusCode());assertEquals(401,request("/auth/me","GET",null,newAccess).statusCode());
        assertEquals(401,request("/auth/refresh","POST","{\"refreshToken\":\""+newRefresh+"\"}",null).statusCode());
    }
    @Test void fiveWrongPasswordsLockAccountTemporarily() throws Exception {for(int i=0;i<5;i++)assertEquals(401,login("incorrect").statusCode());assertEquals(401,login("TestPassword#2026").statusCode());assertEquals(401,login("TestPassword#2026").statusCode());}
    @Test void changePasswordRevokesSessionAndUsesNewPassword() throws Exception {
        String token=value(login("TestPassword#2026").body(),"accessToken");
        assertEquals(200,request("/auth/change-password","POST","{\"oldPassword\":\"TestPassword#2026\",\"newPassword\":\"NewPassword#2026\"}",token).statusCode());
        assertEquals(401,request("/auth/me","GET",null,token).statusCode());assertEquals(401,login("TestPassword#2026").statusCode());assertEquals(200,login("NewPassword#2026").statusCode());
    }
    @Test void invalidAmountsAndOpeningIndicesReturnValidationErrors() throws Exception {
        String token=value(login("TestPassword#2026").body(),"accessToken");
        assertEquals(400,request("/invoices/999999/payments","POST","{\"code\":\"negative\",\"amount\":-1,\"method\":\"CASH\"}",token).statusCode());
        assertEquals(400,request("/meter-readings","POST","{\"contractId\":1,\"period\":\"2026-01\",\"meterType\":\"WATER\",\"newIndex\":2,\"readingDate\":\"2026-01-31\",\"oldIndex\":-1}",token).statusCode());
    }
    @Test void usersNeedOnlyApprovedAccountFieldsAndNeverExposeHashes() throws Exception {
        String token=value(login("TestPassword#2026").body(),"accessToken"),created="new"+UUID.randomUUID();
        try {
            assertEquals(200,request("/users","POST","{\"username\":\""+created+"\",\"password\":\"TestPassword#2026\",\"role\":\"STAFF\"}",token).statusCode());
            var listed=request("/users","GET",null,token);assertEquals(200,listed.statusCode());assertFalse(listed.body().contains("password_hash"));
            assertEquals(410,request("/audit-logs","GET",null,token).statusCode());
        } finally {db.jdbc.update("delete from users where username=?",created);}
    }
}
