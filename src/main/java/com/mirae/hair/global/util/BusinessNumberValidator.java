package com.mirae.hair.global.util;

/**
 * 한국 사업자등록번호 유효성 검증 유틸리티
 *
 * 사업자등록번호란?
 * → 국세청에서 사업자에게 부여하는 10자리 숫자 (형식: XXX-XX-XXXXX)
 * → 앞 3자리: 지방국세청/세무서 코드
 * → 중간 2자리: 법인(01~79) 또는 개인(80~99) 구분
 * → 뒤 5자리: 일련번호 + 검증번호(마지막 1자리)
 *
 * 검증 알고리즘 (가중치 검증법):
 * → 각 자리에 가중치 [1, 3, 7, 1, 3, 7, 1, 3, 5]를 곱한다.
 * → 9번째 자리 × 5의 결과를 10으로 나눈 몫도 추가로 더한다.
 * → 합계를 10으로 나눈 나머지를 10에서 뺀 값의 1의 자리가 마지막(10번째) 자리와 같으면 유효하다.
 *
 * 왜 프론트엔드가 아닌 백엔드에서도 검증하는가?
 * → 프론트엔드 검증은 사용자 편의를 위한 것이고, 쉽게 우회할 수 있다.
 * → Postman이나 curl로 API를 직접 호출하면 프론트 검증을 통과한다.
 * → "최종 방어선은 항상 백엔드"여야 한다. 이것이 보안의 기본 원칙이다.
 */
public class BusinessNumberValidator {

    /** 사업자등록번호 가중치 배열 */
    private static final int[] WEIGHTS = {1, 3, 7, 1, 3, 7, 1, 3, 5};

    /**
     * 사업자등록번호 유효성 검증
     *
     * @param businessNumber 사업자등록번호 (하이픈 포함/미포함 모두 허용)
     * @return true: 유효, false: 무효
     */
    public static boolean isValid(String businessNumber) {
        if (businessNumber == null) {
            return false;
        }

        // 하이픈 제거 후 숫자만 추출
        String digits = businessNumber.replaceAll("[^0-9]", "");

        // 10자리가 아니면 무효
        if (digits.length() != 10) {
            return false;
        }

        // 가중치 검증
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (digits.charAt(i) - '0') * WEIGHTS[i];
        }

        // 9번째 자리(인덱스 8) × 5를 10으로 나눈 몫을 추가로 더한다
        sum += ((digits.charAt(8) - '0') * 5) / 10;

        // 검증: (10 - (합계 % 10)) % 10 == 마지막 자리
        int checkDigit = (10 - (sum % 10)) % 10;
        int lastDigit = digits.charAt(9) - '0';

        return checkDigit == lastDigit;
    }

    /**
     * 사업자등록번호 형식 정규화 (하이픈 포함 형태로 변환)
     * 예: "1234567890" → "123-45-67890"
     *
     * @param businessNumber 원본 사업자등록번호
     * @return 정규화된 사업자등록번호 (XXX-XX-XXXXX)
     */
    public static String normalize(String businessNumber) {
        String digits = businessNumber.replaceAll("[^0-9]", "");
        if (digits.length() != 10) {
            return businessNumber; // 변환 불가 시 원본 반환
        }
        return digits.substring(0, 3) + "-" + digits.substring(3, 5) + "-" + digits.substring(5);
    }
}
