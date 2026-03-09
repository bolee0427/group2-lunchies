package tech.picnic.lunchies.logic;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class LunchiesService {
  private final String appName;

  public LunchiesService(@Value("${lunchies.app.name:lunchies}") String appName) {
    this.appName = appName;
  }

  public Publisher<String> getStatus() {
    log.info("Status requested for {}", appName);
    return Mono.just("OK");
  }

  public ImmutableList<String> getFeatures() {
    return ImmutableList.of("reactive-streams", "component-scanning", "profiles");
  }
}
