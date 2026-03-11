package com.mirae.hair.global.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * XSS(Cross-Site Scripting) 공격 방어 필터
 *
 * XSS란?
 * → 공격자가 <script>alert('해킹')</script> 같은 악성 스크립트를
 *   입력 필드에 넣어 다른 사용자의 브라우저에서 실행시키는 공격이다.
 * → 예: 상품명에 <script>document.cookie</script>를 넣으면,
 *   상품 목록을 보는 다른 관리자의 쿠키(세션 정보)가 탈취될 수 있다.
 *
 * 이 필터의 역할:
 * → 요청 파라미터(Query String)에서 HTML 특수문자를 이스케이프 처리한다.
 * → <, >, &, ", ' 등을 &lt;, &gt;, &amp; 등으로 변환한다.
 * → 변환된 문자열은 브라우저에서 스크립트로 실행되지 않는다.
 *
 * 한계:
 * → JSON 요청 본문(Body)은 이 필터로 처리되지 않는다.
 * → JSON의 XSS 방어는 Jackson의 기본 이스케이프 기능과 프론트엔드에서의 출력 인코딩으로 처리한다.
 * → REST API에서는 응답이 HTML이 아닌 JSON이므로, 브라우저가 스크립트로 해석할 위험이 낮다.
 *
 * @Order(HIGHEST_PRECEDENCE): 다른 필터보다 가장 먼저 실행되어야 한다.
 * → XSS 공격이 다른 필터에 영향을 미치기 전에 차단해야 하기 때문이다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XssFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        chain.doFilter(new XssRequestWrapper((HttpServletRequest) request), response);
    }

    /**
     * HttpServletRequest를 감싸서 파라미터 값을 이스케이프 처리하는 래퍼
     *
     * 왜 Wrapper 패턴을 사용하는가?
     * → HttpServletRequest는 인터페이스라 직접 수정할 수 없다.
     * → Wrapper로 감싸면 getParameter() 같은 메서드를 오버라이드하여
     *   원본 값 대신 이스케이프된 값을 반환할 수 있다.
     * → 기존 코드를 수정하지 않고도 모든 파라미터에 XSS 방어가 적용된다.
     */
    private static class XssRequestWrapper extends HttpServletRequestWrapper {

        XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitize(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] sanitizedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitizedValues[i] = sanitize(values[i]);
            }
            return sanitizedValues;
        }

        /**
         * 왜 getHeader()는 sanitize하지 않는가?
         * → Authorization 헤더에 JWT 토큰이 담겨 있는데, sanitize하면 토큰이 손상된다.
         * → Content-Type, Accept 등 시스템 헤더도 변형되면 안 된다.
         * → HTTP 헤더는 브라우저에서 직접 렌더링되지 않으므로 XSS 위험이 낮다.
         */

        /**
         * HTML 특수문자를 이스케이프 처리
         * → <script> → &lt;script&gt; (브라우저에서 텍스트로 표시됨, 실행 안 됨)
         */
        private String sanitize(String value) {
            if (value == null) {
                return null;
            }
            return value
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
        }
    }
}
