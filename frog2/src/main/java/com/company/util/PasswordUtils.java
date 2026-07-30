package com.company.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    // 비밀번호 해싱
    public static String hashPassword(String plainTextPassword) {
        // 솔트 생성 및 비밀번호 해싱
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12));
    }

    // 비밀번호 검증
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        // 입력된 비밀번호와 저장된 해시가 일치하는지 확인
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
}