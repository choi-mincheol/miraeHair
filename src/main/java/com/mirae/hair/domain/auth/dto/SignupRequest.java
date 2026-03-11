package com.mirae.hair.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO
 *
 * 왜 Entity를 직접 받지 않고 DTO를 사용하는가?
 * → Entity를 직접 @RequestBody로 받으면:
 *   1) 클라이언트가 id, role 등 조작하면 안 되는 필드까지 보낼 수 있다.
 *   2) Entity의 구조가 바뀌면 API 스펙도 같이 바뀌어서 프론트엔드가 깨진다.
 * → DTO로 필요한 필드만 받으면 이런 문제를 방지할 수 있다.
 *
 * 왜 @Valid + Bean Validation을 사용하는가?
 * → if (email == null) throw new ... 같은 수동 검증 코드를 반복하지 않아도 된다.
 * → 어노테이션만 붙이면 Spring이 자동으로 검증하고, 실패 시 GlobalExceptionHandler로 넘긴다.
 */
@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이메일은 필수 입력입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력입니다")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    private String password;

    @NotBlank(message = "이름은 필수 입력입니다")
    @Size(max = 50, message = "이름은 50자 이내로 입력해주세요")
    private String name;
}
