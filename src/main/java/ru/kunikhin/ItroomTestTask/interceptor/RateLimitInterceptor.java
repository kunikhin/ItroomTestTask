package ru.kunikhin.ItroomTestTask.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();

    @Value("${ratelimiter.max-requests}")
    private int maxRequests;
    @Value("${ratelimiter.time-window-ms}")
    private long timeWindow;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RateLimitInfo rateLimitInfo = rateLimitMap.computeIfAbsent(
                request.getParameter("walletId"),
                k -> new RateLimitInfo()
        );
        long currentTime = System.currentTimeMillis();

        synchronized (rateLimitInfo) {
            if (currentTime - rateLimitInfo.getWindowStart() > timeWindow) {
                rateLimitInfo.setWindowStart(currentTime);
                rateLimitInfo.setRequestCount(1);
                return true;
            }

            if (rateLimitInfo.getRequestCount() < maxRequests) {
                rateLimitInfo.incrementRequestCount();
                return true;
            }

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "1");
            return false;
        }
    }

    private static class RateLimitInfo {
        private long windowStart;
        private int requestCount;

        public RateLimitInfo() {
            this.windowStart = System.currentTimeMillis();
            this.requestCount = 0;
        }

        public long getWindowStart() {
            return windowStart;
        }

        public void setWindowStart(long windowStart) {
            this.windowStart = windowStart;
        }

        public int getRequestCount() {
            return requestCount;
        }

        public void setRequestCount(int requestCount) {
            this.requestCount = requestCount;
        }

        public void incrementRequestCount() {
            this.requestCount++;
        }
    }
}