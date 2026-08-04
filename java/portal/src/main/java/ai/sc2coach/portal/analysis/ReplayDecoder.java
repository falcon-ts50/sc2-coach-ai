package ai.sc2coach.portal.analysis;

import java.nio.file.Path;

public interface ReplayDecoder {

    Path decode(Path replay, Path outputDirectory);
}
