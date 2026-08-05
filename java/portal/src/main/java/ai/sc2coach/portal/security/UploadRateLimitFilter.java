package ai.sc2coach.portal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class UploadRateLimitFilter extends OncePerRequestFilter {

    private static final String ANALYSIS_PATH = "/api/v1/analyses";
    private static final int MAX_UPLOADS_PER_WINDOW = 3;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Semaphore GLOBAL_ANALYSIS_SLOTS = new Semaphore(2);

    private final Map<String, ClientWindow> clients = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !ANALYSIS_PATH.equals(request.getRequestURI()) || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String clientIp = clientIp(request);
        ClientWindow window = clients.computeIfAbsent(clientIp, ignored -> new ClientWindow());
        if (!window.tryAcquire()) {
            reject(response, HttpStatus.TOO_MANY_REQUESTS, "Too many replay uploads. Try again in a minute.");
            return;
        }
        if (!GLOBAL_ANALYSIS_SLOTS.tryAcquire()) {
            window.release();
            reject(response, HttpStatus.TOO_MANY_REQUESTS, "Server is busy processing replays. Try again shortly.");
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            GLOBAL_ANALYSIS_SLOTS.release();
            window.release();
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank() && isLoopback(request.getRemoteAddr())) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static boolean isLoopback(String address) {
        return "127.0.0.1".equals(address) || "0:0:0:0:0:0:0:1".equals(address) || "::1".equals(address);
    }

    private static void reject(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setHeader("Retry-After", "60");
        response.getWriter().write("""
                {"title":"Replay upload rate limit","status":%d,"detail":"%s"}\
                """.formatted(status.value(), detail));
    }

    private static final class ClientWindow {
        private long startedAtNanos = System.nanoTime();
        private int uploads;
        private boolean inFlight;

        synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            if (now - startedAtNanos > WINDOW.toNanos()) {
                startedAtNanos = now;
                uploads = 0;
                inFlight = false;
            }
            if (inFlight || uploads >= MAX_UPLOADS_PER_WINDOW) {
                return false;
            }
            uploads++;
            inFlight = true;
            return true;
        }

        synchronized void release() {
            inFlight = false;
        }
    }
}
