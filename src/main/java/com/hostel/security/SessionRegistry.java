package com.hostel.security;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
/** Single-instance ephemeral sessions; restart invalidates sessions via JwtFilter's boot key. */
@Component
public class SessionRegistry {
 public record Refresh(long user,String fingerprint,long epoch,Instant expires){}
 private final Map<String,Refresh> refreshes=new ConcurrentHashMap<>();
 private final Map<Long,AtomicLong> epochs=new ConcurrentHashMap<>();
 private final Map<Long,List<Instant>> attempts=new ConcurrentHashMap<>();
 public long epoch(long user){return epochs.computeIfAbsent(user,k->new AtomicLong()).get();}
 public void revoke(long user){epochs.computeIfAbsent(user,k->new AtomicLong()).incrementAndGet();refreshes.entrySet().removeIf(e->e.getValue().user()==user);}
 public synchronized boolean locked(long user){var a=attempts.get(user);if(a==null)return false;a.removeIf(t->t.isBefore(Instant.now().minusSeconds(900)));return a.size()>=5;}
 public synchronized void failed(long user){if(attempts.size()>10000)attempts.entrySet().removeIf(e->e.getValue().stream().allMatch(t->t.isBefore(Instant.now().minusSeconds(900))));attempts.computeIfAbsent(user,k->new ArrayList<>()).add(Instant.now());if(locked(user))revoke(user);}
 public void clear(long user){attempts.remove(user);}
 public String refresh(long user,String fingerprint){refreshes.entrySet().removeIf(e->e.getValue().expires().isBefore(Instant.now()));if(refreshes.size()>=10000)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"Session capacity reached");String token=UUID.randomUUID()+"-"+UUID.randomUUID();refreshes.put(token,new Refresh(user,fingerprint,epoch(user),Instant.now().plusSeconds(604800)));return token;}
 public Refresh consume(String token){var r=refreshes.remove(token);return r!=null && r.expires().isAfter(Instant.now()) && r.epoch()==epoch(r.user())?r:null;}
}
