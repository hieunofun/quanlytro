package com.hostel.service;
import com.hostel.security.Access;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class Audit {
 private final Access access; private static final Logger LOG=LoggerFactory.getLogger(Audit.class);
 public Audit(Access access){this.access=access;}
 public void log(String action,String type,long id,Object before,Object after){LOG.info("actor={} action={} resource={} id={}",access.userId(),action,type,id);}
}
