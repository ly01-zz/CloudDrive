package cn.bvovd.clouddrive.password;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


public class testP {
    @Test
    public  void pt() {
        String rawPassword = "Ly449973x@";
        String encoded = new BCryptPasswordEncoder().encode(rawPassword);
        System.out.println("加密后的密码哈希：");
        System.out.println(encoded);
    }
}
