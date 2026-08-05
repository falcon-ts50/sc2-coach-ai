package ai.sc2coach.portal.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/build")
public class BuildInfoController {

    private final String version;
    private final String build;
    private final String commit;
    private final String buildTime;

    public BuildInfoController(
            @Value("${app.version:${APP_VERSION:dev}}") String version,
            @Value("${app.build:${BUILD_NUMBER:local}}") String build,
            @Value("${app.commit:${GIT_COMMIT:unknown}}") String commit,
            @Value("${app.build-time:${BUILD_TIME:unknown}}") String buildTime
    ) {
        this.version = version;
        this.build = build;
        this.commit = commit;
        this.buildTime = buildTime;
    }

    @GetMapping
    public Map<String, String> buildInfo() {
        return Map.of(
                "version", version,
                "build", build,
                "commit", commit,
                "buildTime", buildTime
        );
    }
}
