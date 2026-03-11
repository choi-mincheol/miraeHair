package com.mirae.hair.global.util;

import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256 양방향 암호화/복호화 유틸리티
 *
 * 왜 AES-256을 사용하는가?
 * → 개인정보(카드번호, 연락처 등)는 DB에 평문으로 저장하면 안 된다 (ISMS 대비).
 * → DB가 유출되어도 원본 데이터를 알 수 없도록 암호화해야 한다.
 *
 * AES vs BCrypt 차이:
 * → BCrypt: 단방향 해시 (비밀번호용) → 암호화만 가능, 복호화 불가
 * → AES: 양방향 암호화 (개인정보용) → 암호화도 되고 복호화도 된다
 * → 비밀번호는 "원본을 알 필요 없이 비교만 하면 되므로" BCrypt,
 *   개인정보는 "원본을 꺼내서 보여줘야 하므로" AES를 사용한다.
 *
 * AES/CBC/PKCS5Padding:
 * → AES: 대칭키 암호화 알고리즘 (암호화/복호화에 같은 키 사용)
 * → CBC: 블록 체인 모드 (각 블록이 이전 블록에 의존 → 같은 평문도 다른 암호문 가능)
 * → PKCS5Padding: 블록 크기에 맞게 패딩 추가
 *
 * 주의: 암호화 키는 반드시 환경변수로 관리!
 * → 코드에 키를 하드코딩하면 GitHub에 올라가서 보안 사고가 발생할 수 있다.
 */
@Slf4j
@Component
public class AES256Util {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private final SecretKeySpec secretKeySpec;
    private final IvParameterSpec ivParameterSpec;

    /**
     * 생성자: 암호화 키와 IV(초기화 벡터)를 설정한다.
     *
     * IV(Initialization Vector)란?
     * → CBC 모드에서 첫 번째 블록을 암호화할 때 사용하는 16바이트 값.
     * → IV가 없으면 같은 평문이 항상 같은 암호문을 생성한다.
     * → 여기서는 간단히 키의 앞 16바이트를 IV로 사용한다.
     * → 운영 환경에서는 IV를 별도로 관리하는 것이 더 안전하다.
     */
    public AES256Util(@Value("${encryption.aes.secret-key}") String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.secretKeySpec = new SecretKeySpec(Arrays.copyOf(keyBytes, 32), "AES");
        this.ivParameterSpec = new IvParameterSpec(Arrays.copyOf(keyBytes, 16));
    }

    /**
     * 평문 → 암호문 (Base64 인코딩)
     * 사용 예: String encrypted = aes256Util.encrypt("010-1234-5678");
     */
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AES-256 암호화 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "암호화 처리 중 오류가 발생했습니다");
        }
    }

    /**
     * 암호문 (Base64) → 원본 평문
     * 사용 예: String original = aes256Util.decrypt(encryptedText);
     */
    public String decrypt(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES-256 복호화 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "복호화 처리 중 오류가 발생했습니다");
        }
    }
}
