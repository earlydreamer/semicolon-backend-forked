package dukku.settlement.boundedContext.settlement.batch.notification;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 배치 실패 원인 진단
 * - 예외 체인을 분석하여 실패 유형과 영향받은 서비스를 식별
 */
@Component
public class BatchFailureDiagnoser {

    private static final Map<String, String> SERVICE_PATTERNS = new LinkedHashMap<>();

    static {
        SERVICE_PATTERNS.put("/orders", "Order");
        SERVICE_PATTERNS.put("/payments", "Payment");
        SERVICE_PATTERNS.put("/deposits", "Deposit");
        SERVICE_PATTERNS.put("/users", "User");
        SERVICE_PATTERNS.put("OrderApiClient", "Order");
        SERVICE_PATTERNS.put("PaymentApiClient", "Payment");
        SERVICE_PATTERNS.put("DepositApiClient", "Deposit");
        SERVICE_PATTERNS.put("UserApiClient", "User");
    }

    /**
     * JobExecution을 분석하여 사람이 읽을 수 있는 실패 요약을 반환
     */
    public String diagnose(JobExecution jobExecution) {
        List<Throwable> exceptions = jobExecution.getAllFailureExceptions();
        if (exceptions.isEmpty()) {
            return null;
        }

        BatchFailureType failureType = classifyFailure(exceptions);
        List<String> affectedServices = identifyAffectedServices(exceptions);
        String failedStep = findFailedStepName(jobExecution);

        return formatDiagnosis(failureType, affectedServices, failedStep);
    }

    private BatchFailureType classifyFailure(List<Throwable> exceptions) {
        for (Throwable ex : exceptions) {
            String chain = getExceptionChainText(ex);

            if (chain.contains("ConnectException")
                    || chain.contains("ResourceAccessException")
                    || chain.contains("Connection refused")
                    || chain.contains("Connection timed out")
                    || chain.contains("UnknownHostException")
                    || chain.contains("SocketTimeoutException")
                    || chain.contains("HttpServerErrorException")
                    || chain.contains("Bad Gateway")
                    || chain.contains("Service Unavailable")
                    || chain.contains("I/O error on")) {
                return BatchFailureType.SERVICE_UNAVAILABLE;
            }

            if (chain.contains("DataAccessException")
                    || chain.contains("JDBCException")
                    || chain.contains("CannotCreateTransactionException")) {
                return BatchFailureType.DB_ERROR;
            }

            if (chain.contains("SkipLimitExceededException")
                    || chain.contains("skipLimit")) {
                return BatchFailureType.SKIP_LIMIT_EXCEEDED;
            }
        }

        return BatchFailureType.UNKNOWN;
    }

    private List<String> identifyAffectedServices(List<Throwable> exceptions) {
        List<String> services = new ArrayList<>();

        for (Throwable ex : exceptions) {
            String chain = getExceptionChainText(ex);
            for (Map.Entry<String, String> entry : SERVICE_PATTERNS.entrySet()) {
                if (chain.contains(entry.getKey()) && !services.contains(entry.getValue())) {
                    services.add(entry.getValue());
                }
            }
        }

        return services;
    }

    private String findFailedStepName(JobExecution jobExecution) {
        for (StepExecution step : jobExecution.getStepExecutions()) {
            if (step.getStatus().isUnsuccessful()) {
                return step.getStepName();
            }
        }
        return null;
    }

    private String formatDiagnosis(BatchFailureType type, List<String> services, String failedStep) {
        StringBuilder sb = new StringBuilder();
        sb.append(":mag: *실패 원인 진단*\n");
        sb.append("*유형:* ").append(type.getTitle()).append("\n");

        if (failedStep != null) {
            sb.append("*실패 Step:* `").append(failedStep).append("`\n");
        }

        if (!services.isEmpty()) {
            sb.append("*영향 서비스:* ").append(String.join(", ", services)).append("\n");
        }

        sb.append("*조치:* ").append(type.getAction()).append("\n");

        // SERVICE_UNAVAILABLE일 때 구체적인 메시지
        if (type == BatchFailureType.SERVICE_UNAVAILABLE && !services.isEmpty()) {
            sb.append("\n:rotating_light: *")
                    .append(String.join(", ", services))
                    .append(" 서비스가 동작하지 않아 배치에 실패했습니다.*\n");
        }

        return sb.toString();
    }

    /**
     * 예외 체인의 모든 클래스명 + 메시지를 하나의 문자열로 합침
     */
    private String getExceptionChainText(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            sb.append(current.getClass().getName()).append(": ").append(current.getMessage()).append(" | ");
            if (current.getCause() == current) break;
            current = current.getCause();
        }
        return sb.toString();
    }
}
